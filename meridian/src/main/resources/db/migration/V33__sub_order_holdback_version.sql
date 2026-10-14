-- BR-ADM-06, DAT-08: every sub-order records which holdback configuration version was in force at
-- creation, the same COM-05 mechanism V11__order_commission_version.sql built for commission rate
-- — settlement and refund posting resolve the rate through this stored id rather than "the current
-- rate". NOT NULL for the same reason as commission_configuration_id: an order created without a
-- resolvable holdback rate is a bug at creation time, not a nullable fact to discover later.
ALTER TABLE sub_order
    ADD COLUMN holdback_configuration_id UUID NOT NULL
        CONSTRAINT sub_order_holdback_configuration_fkey
            REFERENCES holdback_configuration (id);
