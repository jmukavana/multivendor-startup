-- REG-04 to 08: eTIMS tax invoices and credit notes.
--
-- One invoice per PARENT order, not per sub-order — 14.1 states Section 10 assumes principal VAT
-- classification (the platform is the deemed supplier to the buyer, not merely a facilitator), so
-- the platform issues one consolidated invoice per order, matching REG-04's own wording ("for
-- every confirmed order") and UC-11's title ("System Emits a Tax Invoice", singular).
--
-- Credit Note is filed under the Returns module in BRD 15.1's entity table, but REG-08's
-- transmission and retry obligations are identical to REG-05/06/07's for invoices, and a credit
-- note has to reference the invoice it corrects — the same "a credit note needs the tax
-- infrastructure, not the returns infrastructure" reasoning that already put Refund under Payments
-- rather than Returns (15.1's own note on that). Both documents live here, in `tax`.

CREATE TABLE tax_invoice (
    id                        UUID   NOT NULL,
    parent_order_id           UUID   NOT NULL,
    invoice_number            TEXT   NOT NULL,
    amount_minor_units        BIGINT NOT NULL,
    currency                  TEXT   NOT NULL,
    status                    TEXT   NOT NULL
        CONSTRAINT tax_invoice_status_check CHECK (status IN
            ('PENDING', 'TRANSMITTED', 'FAILED', 'DEAD_LETTERED')),
    acknowledgement_reference TEXT,
    attempt_count             INTEGER NOT NULL DEFAULT 0,
    created_at                TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    transmitted_at            TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT tax_invoice_pkey PRIMARY KEY (id),
    CONSTRAINT tax_invoice_parent_order_unique UNIQUE (parent_order_id),
    CONSTRAINT tax_invoice_number_unique UNIQUE (invoice_number),
    CONSTRAINT tax_invoice_amount_positive CHECK (amount_minor_units > 0)
);

-- REG-05/06: the retry sweep and the admin exception queue both scan this.
CREATE INDEX tax_invoice_retryable_idx ON tax_invoice (status) WHERE status = 'FAILED';
CREATE INDEX tax_invoice_dead_letter_idx ON tax_invoice (status) WHERE status = 'DEAD_LETTERED';

-- REG-08: BR-ORD-13/14's buyer cancellation and BR-RTN-05's return refund both produce these —
-- see tax.internal's two listeners. reference_type/reference_id record which one, for audit only;
-- nothing keys off it.
CREATE TABLE credit_note (
    id                        UUID   NOT NULL,
    tax_invoice_id            UUID   NOT NULL,
    parent_order_id           UUID   NOT NULL,
    amount_minor_units        BIGINT NOT NULL,
    currency                  TEXT   NOT NULL,
    reason                    TEXT   NOT NULL,
    reference_type            TEXT   NOT NULL
        CONSTRAINT credit_note_reference_type_check CHECK (reference_type IN ('CANCELLATION', 'RETURN')),
    status                    TEXT   NOT NULL
        CONSTRAINT credit_note_status_check CHECK (status IN
            ('PENDING', 'TRANSMITTED', 'FAILED', 'DEAD_LETTERED')),
    acknowledgement_reference TEXT,
    attempt_count             INTEGER NOT NULL DEFAULT 0,
    created_at                TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    transmitted_at            TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT credit_note_pkey PRIMARY KEY (id),
    CONSTRAINT credit_note_invoice_fkey FOREIGN KEY (tax_invoice_id) REFERENCES tax_invoice (id),
    CONSTRAINT credit_note_amount_positive CHECK (amount_minor_units > 0)
);

CREATE INDEX credit_note_by_invoice_idx ON credit_note (tax_invoice_id);
CREATE INDEX credit_note_retryable_idx ON credit_note (status) WHERE status = 'FAILED';
CREATE INDEX credit_note_dead_letter_idx ON credit_note (status) WHERE status = 'DEAD_LETTERED';
