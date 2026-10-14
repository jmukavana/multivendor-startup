-- Orders — walking-skeleton stand-in for BR-ORD-01 to 04, 08, 10.
--
-- Cart, multi-vendor splitting (BR-CHK, Phase 4), inventory reservation (BR-INV, Phase 2) and
-- real payment collection (BR-PAY, Phase 3) do not exist yet. This is the minimum that lets one
-- order complete end to end in staging, per the Phase 1 exit criterion in BRD 18: a buyer orders
-- one product from one vendor, payment is marked manually, and the state transition is real.
--
-- Parent and sub-order are both created here even though Phase 1 never has more than one
-- sub-order per parent (there is exactly one vendor). Splitting the tables now, rather than
-- collapsing them into one until Phase 4 needs multiple, means Phase 4 adds sub-orders rather
-- than inventing the table under a schema migration that also has to rewrite every foreign key
-- pointing at "order".

CREATE TABLE parent_order (
    id                UUID         NOT NULL,
    reference         TEXT         NOT NULL,
    buyer_account_id  UUID         NOT NULL,
    status            TEXT         NOT NULL
        CONSTRAINT parent_order_status_check CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED')),
    total_minor_units BIGINT       NOT NULL,
    currency          TEXT         NOT NULL,
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT parent_order_pkey PRIMARY KEY (id),
    CONSTRAINT parent_order_buyer_fkey FOREIGN KEY (buyer_account_id) REFERENCES account (id)
);

-- BR-ORD-01: reference is unique and buyer-facing (order lookup, support reference).
CREATE UNIQUE INDEX parent_order_reference_key ON parent_order (reference);
CREATE INDEX parent_order_by_buyer_idx ON parent_order (buyer_account_id, created_at DESC);

CREATE TABLE sub_order (
    id                   UUID         NOT NULL,
    parent_order_id      UUID         NOT NULL,
    vendor_id            UUID         NOT NULL,
    status               TEXT         NOT NULL
        CONSTRAINT sub_order_status_check CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED')),
    subtotal_minor_units BIGINT       NOT NULL,
    currency             TEXT         NOT NULL,
    created_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT sub_order_pkey PRIMARY KEY (id),
    CONSTRAINT sub_order_parent_fkey FOREIGN KEY (parent_order_id) REFERENCES parent_order (id),
    CONSTRAINT sub_order_vendor_fkey FOREIGN KEY (vendor_id) REFERENCES vendor (id)
);

CREATE INDEX sub_order_by_parent_idx ON sub_order (parent_order_id);
CREATE INDEX sub_order_by_vendor_idx ON sub_order (vendor_id, created_at DESC);

CREATE TABLE order_line (
    id                     UUID   NOT NULL,
    sub_order_id           UUID   NOT NULL,
    product_id             UUID   NOT NULL,
    -- Snapshotted at order time: BR-CAT-09 lets a vendor change price after the fact, and an order
    -- must keep showing what the buyer actually paid, not what the product costs today.
    product_name_snapshot  TEXT   NOT NULL,
    unit_price_minor_units BIGINT NOT NULL,
    quantity               INTEGER NOT NULL,
    line_total_minor_units BIGINT NOT NULL,
    currency               TEXT   NOT NULL,
    CONSTRAINT order_line_pkey PRIMARY KEY (id),
    CONSTRAINT order_line_sub_order_fkey FOREIGN KEY (sub_order_id) REFERENCES sub_order (id),
    CONSTRAINT order_line_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX order_line_by_sub_order_idx ON order_line (sub_order_id);

-- BR-ORD-10: immutable audit trail of every order state change, with actor and timestamp.
-- Append-only, matching auth_event's pattern in V2.
CREATE TABLE order_event (
    id              UUID         NOT NULL,
    parent_order_id UUID         NOT NULL,
    event_type      TEXT         NOT NULL,
    previous_status TEXT,
    new_status      TEXT,
    actor_account_id UUID,
    occurred_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT order_event_pkey PRIMARY KEY (id),
    CONSTRAINT order_event_parent_fkey FOREIGN KEY (parent_order_id) REFERENCES parent_order (id)
);

CREATE INDEX order_event_by_parent_idx ON order_event (parent_order_id, occurred_at);
