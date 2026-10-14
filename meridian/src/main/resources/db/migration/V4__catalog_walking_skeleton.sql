-- Catalog — walking-skeleton stand-in.
--
-- BR-CAT-01 to 12 describe variants, categories, moderation states and price history; that
-- arrives with vendor onboarding in Phase 2 (see the module javadoc). Phase 1 needs only a
-- product a vendor can list and a buyer can order: one price, one currency, no variants.
--
-- DAT-01 applies from the first row: price is minor units with an explicit currency, never a
-- floating-point column.

CREATE TABLE product (
    id                UUID         NOT NULL,
    vendor_id         UUID         NOT NULL,
    name              TEXT         NOT NULL,
    description       TEXT,
    price_minor_units BIGINT       NOT NULL,
    currency          TEXT         NOT NULL,
    status            TEXT         NOT NULL
        CONSTRAINT product_status_check CHECK (status IN ('ACTIVE', 'DELISTED')),
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT product_pkey PRIMARY KEY (id),
    CONSTRAINT product_vendor_fkey FOREIGN KEY (vendor_id) REFERENCES vendor (id),
    CONSTRAINT product_price_non_negative CHECK (price_minor_units >= 0)
);

-- BR-SCH-04: default listing excludes delisted products, and is the query this index serves —
-- newest active products first, the obvious default ordering for a catalog with no search yet.
CREATE INDEX product_active_by_created_idx ON product (created_at DESC) WHERE status = 'ACTIVE';

CREATE INDEX product_by_vendor_idx ON product (vendor_id);
