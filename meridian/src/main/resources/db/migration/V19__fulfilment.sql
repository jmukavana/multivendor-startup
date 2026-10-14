-- BR-FUL-01 to 08: carriers, delivery zones, versioned rate cards, and the delivery record itself.
--
-- Zone and rate-card configuration exists here as real, independently admin-manageable data
-- (BR-FUL-08) even though nothing charges against a rate card yet — checkout does not select a
-- delivery method or add its cost to an order total (BR-CHK-05/06/07, still deferred; see
-- checkout's own note on why). Building the configuration correctly now, ahead of the consumer
-- that will read it, matches how commission_configuration (V10) predated order's own use of it.

CREATE TABLE carrier (
    id         UUID    NOT NULL,
    name       TEXT    NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT carrier_pkey PRIMARY KEY (id)
);

-- BR-FUL-03, AC-FUL-03: a second carrier is a second row here, nothing else — the mechanism this
-- proves. Seeded with one, matching every other mock provider's "at least one, real when a
-- contract exists" treatment (OPEN-03's sibling for logistics).
INSERT INTO carrier (id, name, active, created_at) VALUES (gen_random_uuid(), 'MockLogistics KE', TRUE, now());

CREATE TABLE delivery_zone (
    id         UUID    NOT NULL,
    name       TEXT    NOT NULL,
    county     TEXT    NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT delivery_zone_pkey PRIMARY KEY (id)
);

INSERT INTO delivery_zone (id, name, county, active, created_at) VALUES (gen_random_uuid(), 'Nairobi Metro', 'Nairobi', TRUE, now());

-- DAT-07: a delivery rate is configuration that determines an amount owed, versioned exactly like
-- commission_configuration (V10) — effective-from, never updated in place.
CREATE TABLE delivery_rate_card (
    id               UUID   NOT NULL,
    carrier_id       UUID   NOT NULL,
    zone_id          UUID   NOT NULL,
    method           TEXT   NOT NULL
        CONSTRAINT delivery_rate_card_method_check CHECK (method IN ('DOOR', 'PICKUP_POINT')),
    price_minor_units BIGINT NOT NULL,
    currency         TEXT   NOT NULL,
    effective_from   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT delivery_rate_card_pkey PRIMARY KEY (id),
    CONSTRAINT delivery_rate_card_carrier_fkey FOREIGN KEY (carrier_id) REFERENCES carrier (id),
    CONSTRAINT delivery_rate_card_zone_fkey FOREIGN KEY (zone_id) REFERENCES delivery_zone (id),
    CONSTRAINT delivery_rate_card_price_non_negative CHECK (price_minor_units >= 0)
);

CREATE INDEX delivery_rate_card_current_idx ON delivery_rate_card (carrier_id, zone_id, method, effective_from DESC);

-- BR-FUL-02's door-delivery default, indicative pending OPEN-06 (who sets delivery pricing, still
-- open per the BRD's own tracker) — not a real quote, the same "indicative, not configured"
-- treatment this codebase gives every other BRD-sourced figure it has not yet been told to price.
INSERT INTO delivery_rate_card (id, carrier_id, zone_id, method, price_minor_units, currency, effective_from, created_at)
SELECT gen_random_uuid(), c.id, z.id, 'DOOR', 25000, 'KES', now(), now()
FROM carrier c, delivery_zone z;

-- BR-FUL-01/02/04/06/07: one row per sub-order needing delivery. Created eagerly on
-- OrderPaymentConfirmed (PENDING), not on a buyer's delivery-method choice at checkout — that
-- choice does not exist yet either (BR-CHK-05/06/07); see fulfilment.internal
-- .OrderReadinessListener.
CREATE TABLE delivery (
    id                          UUID   NOT NULL,
    sub_order_id                UUID   NOT NULL,
    parent_order_id             UUID   NOT NULL,
    vendor_id                   UUID   NOT NULL,
    buyer_account_id            UUID   NOT NULL,
    carrier_id                  UUID,
    zone_id                     UUID,
    method                      TEXT
        CONSTRAINT delivery_method_check CHECK (method IN ('DOOR', 'PICKUP_POINT')),
    status                      TEXT   NOT NULL
        CONSTRAINT delivery_status_check CHECK (status IN
            ('PENDING', 'BOOKED', 'DISPATCHED', 'IN_TRANSIT', 'DELIVERED', 'FAILED')),
    tracking_reference          TEXT,
    proof_of_delivery_reference TEXT,
    created_at                  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    booked_at                   TIMESTAMP(6) WITH TIME ZONE,
    dispatched_at               TIMESTAMP(6) WITH TIME ZONE,
    delivered_at                TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT delivery_pkey PRIMARY KEY (id),
    CONSTRAINT delivery_sub_order_unique UNIQUE (sub_order_id),
    CONSTRAINT delivery_carrier_fkey FOREIGN KEY (carrier_id) REFERENCES carrier (id),
    CONSTRAINT delivery_zone_fkey FOREIGN KEY (zone_id) REFERENCES delivery_zone (id)
);

CREATE INDEX delivery_by_vendor_idx ON delivery (vendor_id);
CREATE INDEX delivery_by_sub_order_idx ON delivery (sub_order_id);
