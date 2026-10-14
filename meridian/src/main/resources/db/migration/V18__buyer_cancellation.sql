-- BR-ORD-13 to 16: buyer-initiated cancellation, and the two inventory outcomes it can produce
-- depending on how far the sub-order had got. A reservation still RESERVED (payment never
-- confirmed) is simply released, matching BR-INV-03's expiry path exactly. A reservation already
-- CONFIRMED (BR-INV-05 already deducted it from total_quantity) needs the physical stock actually
-- given back — a new outcome, CANCELLED, distinct from RELEASED so the audit trail keeps "held but
-- never shipped" and "was on its way out, un-shipped by a cancellation" visibly different.

ALTER TABLE stock_reservation DROP CONSTRAINT stock_reservation_status_check;
ALTER TABLE stock_reservation ADD CONSTRAINT stock_reservation_status_check
    CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED', 'CANCELLED'));

ALTER TABLE stock_movement DROP CONSTRAINT stock_movement_reason_check;
ALTER TABLE stock_movement ADD CONSTRAINT stock_movement_reason_check
    CHECK (reason IN ('RESTOCK', 'RESERVED', 'RELEASED', 'CONFIRMED', 'EXPIRED', 'CANCELLED'));
