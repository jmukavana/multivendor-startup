-- BR-INV-01 to 08: stock tracking, reservation and the movement ledger.
--
-- Per-product, not per-variant: BR-CAT-04 (variants) is deferred — see catalog's ProductStatus
-- javadoc for the same reduction applied there. There is one stock row per product.
--
-- Two counters, not one, because BR-INV-02/03/05/06 describe three different things happening to
-- the same number: reserving must not touch what a vendor physically has (a reservation that
-- expires must restore exactly what it held), and only a confirmed payment may actually reduce
-- stock on hand (BR-INV-05: "reservation is consumed rather than released, and total stock is
-- reduced exactly once" — AC-INV-03). A single counter cannot represent both without an
-- additional column recording what was reserved, which is what reserved_quantity is.
--
--   available to buyers = total_quantity - reserved_quantity   (never stored — always computed,
--   so it cannot drift out of sync with the two counters that are its actual source of truth)

CREATE TABLE stock (
    product_id        UUID   NOT NULL,
    total_quantity     INTEGER NOT NULL,
    reserved_quantity  INTEGER NOT NULL DEFAULT 0,
    updated_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT stock_pkey PRIMARY KEY (product_id),
    CONSTRAINT stock_product_fkey FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT stock_total_non_negative CHECK (total_quantity >= 0),
    CONSTRAINT stock_reserved_non_negative CHECK (reserved_quantity >= 0),
    -- The invariant this whole design exists to protect: reservations can never exceed what is
    -- physically on hand. Both reserve and confirm below update this row with a WHERE clause that
    -- re-checks the invariant atomically, so this constraint should never actually fire — it is
    -- the last line of defence if it ever does.
    CONSTRAINT stock_reserved_within_total CHECK (reserved_quantity <= total_quantity)
);

-- BR-INV-02: a reservation, with expiry. BR-INV-05/06: consumed (CONFIRMED) or given back
-- (RELEASED) — never deleted, so a reservation's outcome is itself part of the audit trail.
CREATE TABLE stock_reservation (
    id            UUID   NOT NULL,
    product_id    UUID   NOT NULL,
    quantity      INTEGER NOT NULL,
    status        TEXT   NOT NULL
        CONSTRAINT stock_reservation_status_check CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED')),
    -- The parent order this reservation belongs to. Set at creation, by checkout, once the order
    -- that will hold it exists — see checkout.internal.CheckoutService.
    reference_id  UUID   NOT NULL,
    expires_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    resolved_at   TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT stock_reservation_pkey PRIMARY KEY (id),
    CONSTRAINT stock_reservation_product_fkey FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT stock_reservation_quantity_positive CHECK (quantity > 0)
);

-- BR-INV-03: the expiry sweep scans exactly this.
CREATE INDEX stock_reservation_expiring_idx ON stock_reservation (expires_at) WHERE status = 'RESERVED';
CREATE INDEX stock_reservation_by_reference_idx ON stock_reservation (reference_id);

-- BR-INV-08, AC-INV-04: every stock change, with a reason and a reference to what caused it.
-- Append-only, matching this codebase's other audit-log tables.
CREATE TABLE stock_movement (
    id             UUID   NOT NULL,
    product_id     UUID   NOT NULL,
    quantity_delta INTEGER NOT NULL,
    reason         TEXT   NOT NULL
        CONSTRAINT stock_movement_reason_check CHECK (reason IN
            ('RESTOCK', 'RESERVED', 'RELEASED', 'CONFIRMED', 'EXPIRED')),
    reference_id   UUID,
    occurred_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT stock_movement_pkey PRIMARY KEY (id),
    CONSTRAINT stock_movement_product_fkey FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE INDEX stock_movement_by_product_idx ON stock_movement (product_id, occurred_at DESC);
