-- BR-PAY-11: refund initiation, "with full traceability to the original transaction" — hence
-- payment_transaction_id, not parent_order_id, as the foreign key: a parent order's payment is one
-- row here, but BR-RTN-04's partial refunds mean several refund rows can trace back to it, one per
-- return or dispute resolution. Refund is its own append-only entity, not a status flag on
-- payment_transaction (see the BRD's own 15.1 note: "Refund is distinct from Return... modelling
-- refund as an attribute leaves nowhere to record the money movement" — the same reasoning applies
-- to folding it into PaymentTransaction).

CREATE TABLE refund (
    id                          UUID   NOT NULL,
    payment_transaction_id      UUID   NOT NULL,
    amount_minor_units          BIGINT NOT NULL,
    currency                    TEXT   NOT NULL,
    reason                      TEXT   NOT NULL,
    status                      TEXT   NOT NULL
        CONSTRAINT refund_status_check CHECK (status IN ('INITIATED', 'CONFIRMED', 'FAILED')),
    provider_refund_reference   TEXT,
    initiated_at                TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    resolved_at                 TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT refund_pkey PRIMARY KEY (id),
    CONSTRAINT refund_payment_transaction_fkey FOREIGN KEY (payment_transaction_id) REFERENCES payment_transaction (id),
    CONSTRAINT refund_amount_positive CHECK (amount_minor_units > 0)
);

CREATE INDEX refund_by_payment_transaction_idx ON refund (payment_transaction_id);
