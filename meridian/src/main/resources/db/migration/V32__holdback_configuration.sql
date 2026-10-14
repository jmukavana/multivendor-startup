-- BR-ADM-06, DAT-07: versioned holdback-percentage configuration, the same mechanism
-- V10__pricing.sql built for commission rate — a configuration table that is updated in place
-- cannot satisfy "reproduce, for any historical order, the exact rate applied" (documents/
-- project-meridian-brd-v2.2.md section 15.2), so this is a new immutable row per change, never an
-- UPDATE.
--
-- 0.10 as the seeded starting default matches BRD 5.9's indicative figure, previously a bare
-- BigDecimal("0.10") constant inside ledger.internal.LedgerService (BR-VND-09's own comment there
-- called it out as "not yet a configured value (OPEN-05)"); this migration is what makes it one.

CREATE TABLE holdback_configuration (
    id               UUID   NOT NULL,
    rate             NUMERIC(6, 5) NOT NULL,
    effective_from   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT holdback_configuration_pkey PRIMARY KEY (id),
    CONSTRAINT holdback_configuration_rate_valid CHECK (rate >= 0 AND rate < 1)
);

CREATE INDEX holdback_configuration_by_effective_idx ON holdback_configuration (effective_from DESC);

INSERT INTO holdback_configuration (id, rate, effective_from, created_at)
VALUES (gen_random_uuid(), 0.10000, now(), now());
