-- BR-SEC-06: recorded at the exact point BR-INV-04's database constraint already refuses a
-- reservation that would exceed available stock — the near-miss that constraint exists to prevent
-- from ever becoming a row inconsistency. See inventory.internal.OversellAttemptEntity.
create table oversell_attempt (
    id uuid primary key,
    product_id uuid not null,
    requested_quantity int not null,
    available_quantity int not null,
    reference_id uuid not null,
    occurred_at timestamptz not null
);

create index oversell_attempt_product_idx on oversell_attempt (product_id);
