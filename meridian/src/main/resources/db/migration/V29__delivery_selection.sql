-- BR-CHK-05/06/07: the buyer's delivery choice, captured at checkout — before the sub-order it
-- belongs to even exists yet as far as fulfilment is concerned. checkout writes one row per
-- sub-order right after order.OrderCreation.create returns real sub-order ids; fulfilment's
-- OrderReadinessListener reads it back when OrderPaymentConfirmed arrives, and books the delivery
-- immediately with the carrier, zone and method the buyer actually chose and paid for, instead of
-- leaving it PENDING for a vendor to book manually. See fulfilment.internal.DeliverySelectionsAdapter.
--
-- A row here is deliberately not the delivery table itself: this is the buyer's *choice*, recorded
-- before payment and before a Delivery row can exist (BR-FUL-01 creates one only on
-- OrderPaymentConfirmed); the two are different things at different points in the order's life.
CREATE TABLE delivery_selection (
    sub_order_id     UUID NOT NULL,
    carrier_id       UUID NOT NULL,
    zone_id          UUID NOT NULL,
    method           TEXT NOT NULL
        CONSTRAINT delivery_selection_method_check CHECK (method IN ('DOOR', 'PICKUP_POINT')),
    rate_card_id     UUID NOT NULL,
    selected_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT delivery_selection_pkey PRIMARY KEY (sub_order_id),
    CONSTRAINT delivery_selection_carrier_fkey FOREIGN KEY (carrier_id) REFERENCES carrier (id),
    CONSTRAINT delivery_selection_zone_fkey FOREIGN KEY (zone_id) REFERENCES delivery_zone (id),
    CONSTRAINT delivery_selection_rate_card_fkey FOREIGN KEY (rate_card_id) REFERENCES delivery_rate_card (id)
);

-- BR-FUL-02: a real, buyer-selectable pickup-point rate alongside the seeded DOOR one — otherwise
-- every existing zone would only ever quote DOOR, which is not what "first-class option" means.
INSERT INTO delivery_rate_card (id, carrier_id, zone_id, method, price_minor_units, currency, effective_from, created_at)
SELECT gen_random_uuid(), c.id, z.id, 'PICKUP_POINT', 15000, 'KES', now(), now()
FROM carrier c, delivery_zone z;
