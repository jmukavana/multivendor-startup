-- BR-ORD-05: the vendor acceptance window. Nullable — set only while a sub-order sits CONFIRMED,
-- awaiting a vendor's accept(); meaningless (and left stale, never cleared) once it moves past
-- that state, the same treatment payment_transaction.resolved_at gets once a transaction settles.
ALTER TABLE sub_order ADD COLUMN acceptance_deadline TIMESTAMPTZ;
