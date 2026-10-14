-- BR-ORD-05: ACCEPTED joins sub_order_status_check's allowed values, the same drop-and-recreate
-- V15__order_delivery.sql already used to add DELIVERED.
ALTER TABLE sub_order DROP CONSTRAINT sub_order_status_check;
ALTER TABLE sub_order ADD CONSTRAINT sub_order_status_check
    CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'ACCEPTED', 'DELIVERED', 'CANCELLED'));
