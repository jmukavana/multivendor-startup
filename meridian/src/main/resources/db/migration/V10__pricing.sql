-- COM-01 to 05, DAT-07/08: versioned commission configuration.
--
-- One platform-wide default rate for this pass — BR-CAT-03's category taxonomy and any per-vendor
-- or per-product override (5.2's full precedence chain: product, vendor, category, platform
-- default) are deferred; this implements the default tier only, versioned exactly as ADR-005
-- describes, so the mechanism COM-05 depends on is real even though the full precedence chain is
-- not yet built on top of it.

CREATE TABLE commission_configuration (
    id               UUID   NOT NULL,
    rate             NUMERIC(6, 5) NOT NULL,
    effective_from   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT commission_configuration_pkey PRIMARY KEY (id),
    CONSTRAINT commission_configuration_rate_valid CHECK (rate >= 0 AND rate < 1)
);

CREATE INDEX commission_configuration_by_effective_idx ON commission_configuration (effective_from DESC);

-- BRD 5.2's mid-margin band, 9%, as the seeded starting default — matches Section 5.3's own
-- worked example. Real category-band rates await OPEN-01.
INSERT INTO commission_configuration (id, rate, effective_from, created_at)
VALUES (gen_random_uuid(), 0.09000, now(), now());
