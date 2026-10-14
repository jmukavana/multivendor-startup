-- COM-05, DAT-08: every sub-order records which commission configuration version was in force at
-- creation, so settlement resolves the rate through this stored id rather than "the current rate"
-- — see pricing.PricingConfiguration's javadoc. NOT NULL: an order created without a resolvable
-- commission rate is a bug at creation time, not a nullable fact to discover later.
ALTER TABLE sub_order
    ADD COLUMN commission_configuration_id UUID NOT NULL
        CONSTRAINT sub_order_commission_configuration_fkey
            REFERENCES commission_configuration (id);
