-- BR-CHK-06/07: delivery cost, kept separate from subtotal_minor_units (items only) so the
-- breakdown BR-CHK-07 asks for — items, delivery, total — is derivable without recomputing
-- anything. Shares sub_order's own currency column; Money never mixes currencies within one
-- sub-order, and delivery is quoted in the same currency the order is priced in.
ALTER TABLE sub_order ADD COLUMN delivery_cost_minor_units BIGINT NOT NULL DEFAULT 0;
