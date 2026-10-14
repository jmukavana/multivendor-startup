-- BR-ADM-06, DAT-07, OPEN-04: versioned settlement-cycle configuration. BRD section 5.6: "Settlement
-- frequency is a business decision; the system must support configuration between daily and
-- monthly" — cycle_days therefore ranges 1 (daily) to 31 (monthly). OPEN-04 records the actual
-- frequency as not yet agreed by Finance; 7 (weekly) is only the seeded starting default, matching
-- the "Batched by settlement cycle" worked example in BRD section 5.5, and is expected to change
-- once OPEN-04 resolves — which is exactly what this table, versioned rather than updated in
-- place, is for.

CREATE TABLE settlement_cycle_configuration (
    id               UUID NOT NULL,
    cycle_days       INTEGER NOT NULL,
    effective_from   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT settlement_cycle_configuration_pkey PRIMARY KEY (id),
    CONSTRAINT settlement_cycle_configuration_days_valid CHECK (cycle_days >= 1 AND cycle_days <= 31)
);

CREATE INDEX settlement_cycle_configuration_by_effective_idx ON settlement_cycle_configuration (effective_from DESC);

INSERT INTO settlement_cycle_configuration (id, cycle_days, effective_from, created_at)
VALUES (gen_random_uuid(), 7, now(), now());
