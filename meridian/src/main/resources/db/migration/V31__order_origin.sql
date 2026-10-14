-- BR-SEC-03: device and session characteristics against each order, sufficient to identify
-- repeated activity from a single origin. Captured once, at checkout — the request that created
-- the order — never updated afterward, matching parent_order's other write-once fields.
ALTER TABLE parent_order ADD COLUMN source_address TEXT;
ALTER TABLE parent_order ADD COLUMN user_agent TEXT;

CREATE INDEX parent_order_source_address_idx ON parent_order (source_address);
