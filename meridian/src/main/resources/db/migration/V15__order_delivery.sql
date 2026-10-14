-- BR-ORD-03 (partial): adds DELIVERED to the sub-order state machine, and the timestamp BR-RTN-01's
-- return window is measured from. This is not the full lifecycle BR-ORD-03 names (Accepted,
-- Dispatched, In Transit are still absent — they need vendor sub-order acceptance and Phase 7's
-- carrier tracking, neither of which exists) — see order.OrderStatus's javadoc for what triggers
-- this transition in the meantime and why.

ALTER TABLE sub_order ADD COLUMN delivered_at TIMESTAMP(6) WITH TIME ZONE;

ALTER TABLE sub_order DROP CONSTRAINT sub_order_status_check;
ALTER TABLE sub_order ADD CONSTRAINT sub_order_status_check
    CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'DELIVERED', 'CANCELLED'));
