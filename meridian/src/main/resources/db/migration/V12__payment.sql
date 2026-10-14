-- BR-PAY-01/02/04/05/07/10/12: collection through an aggregator, never confirmed except by a
-- verified callback, idempotent on the provider's own transaction reference.
--
-- BR-PAY-06 (poll when no callback arrives), BR-PAY-08 (M-Pesa's per-transaction limit), BR-PAY-09
-- (daily reconciliation against a provider settlement report) and BR-PAY-11 (refund) are not
-- implemented in this migration or the module built on it — OPEN-03 means no aggregator is
-- selected, so there is no real provider to poll, limit against or reconcile with yet. What is
-- here is the core state machine and the idempotency guarantee, which do not depend on which
-- provider is eventually chosen (ADR-006).

CREATE TABLE payment_transaction (
    id                            UUID   NOT NULL,
    parent_order_id               UUID   NOT NULL,
    amount_minor_units            BIGINT NOT NULL,
    currency                      TEXT   NOT NULL,
    buyer_account_id              UUID   NOT NULL,
    buyer_phone_number             TEXT   NOT NULL,
    status                        TEXT   NOT NULL
        CONSTRAINT payment_transaction_status_check CHECK (status IN
            ('INITIATED', 'AWAITING_CONFIRMATION', 'CONFIRMED', 'FAILED', 'TIMED_OUT', 'REVERSED')),
    -- Set at initiation: the gateway's own handle for this attempt, used to correlate an inbound
    -- callback that may arrive with only this reference and no knowledge of our transaction id.
    gateway_reference              TEXT,
    -- BR-PAY-12: the provider's transaction reference, for buyer and support reference. Distinct
    -- from gateway_reference — the STK push handle and the settled transaction's own reference are
    -- not always the same value depending on provider.
    provider_transaction_reference TEXT,
    initiated_at                   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    resolved_at                    TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT payment_transaction_pkey PRIMARY KEY (id),
    CONSTRAINT payment_transaction_order_fkey FOREIGN KEY (parent_order_id) REFERENCES parent_order (id),
    CONSTRAINT payment_transaction_amount_positive CHECK (amount_minor_units > 0)
);

CREATE INDEX payment_transaction_by_order_idx ON payment_transaction (parent_order_id);

-- BR-PAY-05: idempotency on the provider's own reference. A callback replayed by the provider (a
-- common failure-recovery behaviour on their side, not an attack) must resolve to the same
-- transaction rather than being processed twice — this constraint is what makes that guaranteed
-- rather than merely intended. NULL is excluded (partial index) because a transaction has no
-- provider reference until the first callback assigns one.
CREATE UNIQUE INDEX payment_transaction_provider_reference_key
    ON payment_transaction (provider_transaction_reference)
    WHERE provider_transaction_reference IS NOT NULL;
