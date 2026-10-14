-- Vendor — walking-skeleton stand-in.
--
-- BR-VND-01 to 10 describe a full application, verification and approval lifecycle; that is
-- Phase 2. Phase 1 needs only enough of a vendor concept for catalog to satisfy BR-CAT-02 (every
-- product scoped to exactly one owning vendor) and for an order to have a vendor to split against.
-- This table is deliberately thin — one row per vendor, no application state, no verification
-- documents, no payout details — and is expected to gain columns and companion tables in Phase 2
-- rather than being replaced outright, since the identifier created here is what Phase 1's
-- products and orders will already be referencing.

CREATE TABLE vendor (
    id               UUID         NOT NULL,
    owner_account_id UUID         NOT NULL,
    business_name    TEXT         NOT NULL,
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT vendor_pkey PRIMARY KEY (id),
    CONSTRAINT vendor_owner_account_fkey FOREIGN KEY (owner_account_id) REFERENCES account (id)
);

-- One vendor per account for now. BR-VND does not state this as a permanent rule and Phase 2 may
-- need to revisit it (a business with multiple legal entities under one owner, say), but nothing
-- in Phase 1 needs more than one, and a unique index is cheap to relax later and expensive to add
-- retroactively once multiple vendors per account exist in the data.
CREATE UNIQUE INDEX vendor_owner_account_key ON vendor (owner_account_id);
