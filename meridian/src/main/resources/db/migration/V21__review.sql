-- BR-REV-01 to 06 (all Should — see this module's package doc on why they were still built to the
-- BRD's own "not optional if reviews ship" standard). One row per review; a vendor's response and
-- an admin's removal are both fields on it rather than child tables — each happens at most once
-- per review (BR-REV-04's "respond... once"; a removal has no further lifecycle after it).

CREATE TABLE review (
    id                      UUID    NOT NULL,
    product_id              UUID    NOT NULL,
    vendor_id               UUID    NOT NULL,
    buyer_account_id        UUID    NOT NULL,
    rating                  INTEGER NOT NULL,
    comment                 TEXT,
    vendor_response         TEXT,
    vendor_response_at      TIMESTAMP(6) WITH TIME ZONE,
    status                  TEXT    NOT NULL
        CONSTRAINT review_status_check CHECK (status IN ('ACTIVE', 'REMOVED')),
    removal_reason          TEXT,
    removed_by_account_id   UUID,
    removed_at              TIMESTAMP(6) WITH TIME ZONE,
    created_at              TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT review_pkey PRIMARY KEY (id),
    CONSTRAINT review_rating_range CHECK (rating BETWEEN 1 AND 5),
    -- Not a BRD-stated requirement verbatim, but the obvious reading of "a numeric rating" per
    -- product from "buyers with a delivered order" (BR-REV-01/02): one review per buyer per
    -- product, not an unbounded number from the same buyer re-reviewing the same purchase.
    CONSTRAINT review_buyer_product_unique UNIQUE (buyer_account_id, product_id)
);

CREATE INDEX review_by_product_idx ON review (product_id) WHERE status = 'ACTIVE';
CREATE INDEX review_by_vendor_idx ON review (vendor_id) WHERE status = 'ACTIVE';
