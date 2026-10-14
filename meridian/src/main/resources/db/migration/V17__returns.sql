-- BR-RTN-01 to 09: return requests, their images, and the disputes escalation produces.
--
-- BR-RTN-09 (return rate tracking, Should — not implemented; the data this table already holds is
-- sufficient to build it later without a schema change) is not built. Everything else here is real.

CREATE TABLE return_request (
    id                      UUID   NOT NULL,
    sub_order_id            UUID   NOT NULL,
    parent_order_id         UUID   NOT NULL,
    buyer_account_id        UUID   NOT NULL,
    vendor_id               UUID   NOT NULL,
    reason                  TEXT   NOT NULL
        CONSTRAINT return_request_reason_check CHECK (reason IN
            ('DAMAGED', 'WRONG_ITEM', 'NOT_AS_DESCRIBED', 'DEFECTIVE', 'CHANGED_MIND', 'OTHER')),
    description              TEXT,
    -- BR-RTN-04: partial refunds are the requested subset of the sub-order's value, computed once
    -- at request time from the lines and quantities the buyer actually selected — not re-derived
    -- later, the same "resolve once, never re-resolve" reasoning COM-05 applies to commission.
    refund_amount_minor_units BIGINT NOT NULL,
    currency                 TEXT   NOT NULL,
    status                   TEXT   NOT NULL
        CONSTRAINT return_request_status_check CHECK (status IN
            ('REQUESTED', 'ESCALATED', 'RESOLVED_BUYER', 'RESOLVED_VENDOR')),
    requested_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    resolved_at              TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT return_request_pkey PRIMARY KEY (id),
    CONSTRAINT return_request_refund_amount_positive CHECK (refund_amount_minor_units > 0)
);

CREATE INDEX return_request_by_buyer_idx ON return_request (buyer_account_id);
CREATE INDEX return_request_by_vendor_idx ON return_request (vendor_id);
CREATE INDEX return_request_by_sub_order_idx ON return_request (sub_order_id);
-- BR-RTN-03: the timeout sweep scans exactly this.
CREATE INDEX return_request_pending_idx ON return_request (requested_at) WHERE status = 'REQUESTED';

-- BR-RTN-02: supporting images. A child table, not an array column, matching product_image's
-- established shape for the same kind of thing.
CREATE TABLE return_image (
    id             UUID   NOT NULL,
    return_id      UUID   NOT NULL,
    blob_reference TEXT   NOT NULL,
    content_type   TEXT   NOT NULL,
    uploaded_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT return_image_pkey PRIMARY KEY (id),
    CONSTRAINT return_image_return_fkey FOREIGN KEY (return_id) REFERENCES return_request (id)
);

CREATE INDEX return_image_by_return_idx ON return_image (return_id);

-- BR-RTN-03/06/07: created on vendor rejection or on timeout; admins resolve it in favour of either
-- party with a mandatory reason (BR-RTN-07, AC-RTN-04).
CREATE TABLE dispute (
    id                      UUID   NOT NULL,
    return_id               UUID   NOT NULL,
    status                  TEXT   NOT NULL
        CONSTRAINT dispute_status_check CHECK (status IN ('OPEN', 'RESOLVED')),
    resolution_favor        TEXT
        CONSTRAINT dispute_resolution_favor_check CHECK (resolution_favor IN ('BUYER', 'VENDOR')),
    resolution_reason       TEXT,
    resolved_by_account_id  UUID,
    created_at              TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    resolved_at             TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT dispute_pkey PRIMARY KEY (id),
    CONSTRAINT dispute_return_fkey FOREIGN KEY (return_id) REFERENCES return_request (id)
);

CREATE INDEX dispute_by_return_idx ON dispute (return_id);
CREATE INDEX dispute_open_idx ON dispute (status) WHERE status = 'OPEN';
