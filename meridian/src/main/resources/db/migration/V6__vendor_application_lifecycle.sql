-- BR-VND-01 to 10: the full application, verification and approval lifecycle.
--
-- The Phase 1 `vendor` table stays as the operational record catalog and order already reference
-- by foreign key. This migration adds the process that now governs how a `vendor` row is created —
-- replacing Phase 1's self-service POST /vendors — plus the documents, payout details and
-- suspension that process needs.

CREATE TABLE vendor_application (
    id                   UUID         NOT NULL,
    account_id           UUID         NOT NULL,
    business_name        TEXT         NOT NULL,
    -- BR-VND-02, UC-01 A2: absent for a sole trader.
    registration_number  TEXT,
    kra_pin              TEXT         NOT NULL,
    physical_address     TEXT         NOT NULL,
    county                TEXT        NOT NULL,
    contact_name          TEXT        NOT NULL,
    contact_phone         TEXT        NOT NULL,
    contact_email         TEXT,
    -- BR-VND-05. SUSPENDED is deliberately not one of these: this column is the application
    -- process, which ends at Approved or Rejected. Suspension is a state of the resulting vendor
    -- (see the vendor.status column below), computed back onto an application's reported status by
    -- the reading code rather than stored twice — see VendorApplicationEntity's javadoc.
    status                TEXT        NOT NULL
        CONSTRAINT vendor_application_status_check CHECK (status IN
            ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'INFORMATION_REQUESTED', 'APPROVED', 'REJECTED')),
    -- BR-VND-06: required whenever status is REJECTED (AC-VND-06); enforced in code, not by a
    -- conditional check constraint, so the message from that enforcement can be specific.
    rejection_reason      TEXT,
    information_request   TEXT,
    submitted_at           TIMESTAMP(6) WITH TIME ZONE,
    reviewed_at            TIMESTAMP(6) WITH TIME ZONE,
    reviewer_account_id    UUID,
    -- Set once, on approval. Never reused if the vendor is later suspended and a new application
    -- is somehow submitted — that is a new application row with its own vendor.
    vendor_id              UUID,
    created_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT vendor_application_pkey PRIMARY KEY (id),
    CONSTRAINT vendor_application_account_fkey FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT vendor_application_reviewer_fkey FOREIGN KEY (reviewer_account_id) REFERENCES account (id),
    CONSTRAINT vendor_application_vendor_fkey FOREIGN KEY (vendor_id) REFERENCES vendor (id)
);

-- BR-VND-02, AC-VND-05: a KRA PIN already committed to an approved vendor, or live in another
-- pending application, blocks a new submission. A rejected or still-draft application does not
-- hold the PIN, so the applicant — or someone else — may use it again.
CREATE UNIQUE INDEX vendor_application_kra_pin_key ON vendor_application (kra_pin)
    WHERE status IN ('SUBMITTED', 'UNDER_REVIEW', 'INFORMATION_REQUESTED', 'APPROVED');

CREATE INDEX vendor_application_by_account_idx ON vendor_application (account_id, created_at DESC);

-- BR-ADM-01: the admin approval queue reads exactly this.
CREATE INDEX vendor_application_pending_idx ON vendor_application (submitted_at)
    WHERE status IN ('SUBMITTED', 'UNDER_REVIEW', 'INFORMATION_REQUESTED');

-- BR-VND-03: verification documents, stored via shared.BlobStorage. blob_reference is opaque —
-- see that interface's javadoc for why it is never a filesystem path or URL.
CREATE TABLE verification_document (
    id                 UUID         NOT NULL,
    application_id     UUID         NOT NULL,
    document_type      TEXT         NOT NULL
        CONSTRAINT verification_document_type_check CHECK (document_type IN
            ('NATIONAL_ID', 'BUSINESS_REGISTRATION_CERTIFICATE', 'KRA_PIN_CERTIFICATE')),
    blob_reference      TEXT         NOT NULL,
    original_filename   TEXT         NOT NULL,
    content_type        TEXT         NOT NULL,
    size_bytes          BIGINT       NOT NULL,
    uploaded_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT verification_document_pkey PRIMARY KEY (id),
    CONSTRAINT verification_document_application_fkey
        FOREIGN KEY (application_id) REFERENCES vendor_application (id)
);

CREATE INDEX verification_document_by_application_idx ON verification_document (application_id);

-- BR-VND-10: a vendor's own operational status. Existing Phase 1 rows backfill to ACTIVE, which is
-- correct — every vendor created before this migration was created through the Phase 1 self-service
-- path and none has been suspended, since suspension did not exist as a concept until now.
ALTER TABLE vendor
    ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT vendor_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    ADD COLUMN suspended_at TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN suspension_reason TEXT;

-- BR-VND-08/09: payout details, captured separately from the application. One live record per
-- vendor — BR-VND-09's cooling-off only needs the most recent change, not a full history.
--
-- verified_at exists but nothing sets it yet: Phase 2 has no mechanism to verify a bank or M-Pesa
-- destination belongs to the vendor. It is present now so Phase 6's settlement exclusion (BR-STL-08,
-- AC-VND-04) has real cooling-off data to read rather than needing its own migration first.
CREATE TABLE payout_detail (
    id                 UUID         NOT NULL,
    vendor_id          UUID         NOT NULL,
    method             TEXT         NOT NULL
        CONSTRAINT payout_detail_method_check CHECK (method IN ('MPESA', 'BANK')),
    destination        TEXT         NOT NULL,
    verified_at        TIMESTAMP(6) WITH TIME ZONE,
    changed_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    -- BR-VND-09: settlement (Phase 6) excludes this vendor from any run before this instant.
    cooling_off_until  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT payout_detail_pkey PRIMARY KEY (id),
    CONSTRAINT payout_detail_vendor_fkey FOREIGN KEY (vendor_id) REFERENCES vendor (id)
);

CREATE UNIQUE INDEX payout_detail_vendor_key ON payout_detail (vendor_id);

-- BR-IAM-04, AC-VND-03: every attempt to view or modify payout details, successful or refused, is
-- logged. A dedicated table rather than the generic auth_event: the actor may be a vendor staff
-- member with no admin privileges, so this is a business event about payout data specifically, not
-- an authentication event.
CREATE TABLE payout_detail_access_event (
    id           UUID         NOT NULL,
    vendor_id    UUID         NOT NULL,
    actor_account_id UUID     NOT NULL,
    action       TEXT         NOT NULL
        CONSTRAINT payout_detail_access_action_check CHECK (action IN ('VIEW', 'CHANGE')),
    outcome      TEXT         NOT NULL
        CONSTRAINT payout_detail_access_outcome_check CHECK (outcome IN ('ALLOWED', 'REFUSED')),
    occurred_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT payout_detail_access_event_pkey PRIMARY KEY (id)
);

CREATE INDEX payout_detail_access_by_vendor_idx ON payout_detail_access_event (vendor_id, occurred_at DESC);
