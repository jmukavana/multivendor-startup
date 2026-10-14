-- BR-LDG-01 to 07: the double-entry ledger.
--
-- Every business event that moves money posts a balanced set of entries — one row per account
-- touched, debits summing to credits within the set (BR-LDG-03: atomic with the causing
-- transaction; BR-LDG-05: the trial balance across the whole table sums to zero at any point,
-- which holds trivially if every posting balances on its own). posting_id groups the rows that
-- belong to one posting; nothing besides that grouping distinguishes them from any other entry.
--
-- Four accounts for this pass, matching what payment confirmation and settlement disbursement
-- actually move (see ledger.internal.LedgerAccount's javadoc for the full reasoning and what is
-- deliberately not modelled yet — delivery cost, BR-CHK-06, and refunds, BR-RTN, both deferred
-- along with the modules that would produce their entries).

CREATE TABLE ledger_entry (
    id             UUID   NOT NULL,
    posting_id     UUID   NOT NULL,
    account        TEXT   NOT NULL
        CONSTRAINT ledger_entry_account_check CHECK (account IN
            ('COLLECTIONS_CLEARING', 'COMMISSION_REVENUE', 'VENDOR_PAYABLE', 'HOLDBACK_RESERVE')),
    -- Populated only for the two per-vendor accounts (VENDOR_PAYABLE, HOLDBACK_RESERVE) — BR-LDG-06
    -- derives a vendor's balance by summing entries on those accounts filtered to this column.
    vendor_id      UUID,
    is_debit       BOOLEAN NOT NULL,
    amount_minor_units BIGINT NOT NULL,
    currency       TEXT   NOT NULL,
    reference_type TEXT   NOT NULL,
    reference_id   UUID   NOT NULL,
    posted_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT ledger_entry_pkey PRIMARY KEY (id),
    CONSTRAINT ledger_entry_amount_positive CHECK (amount_minor_units > 0)
);

CREATE INDEX ledger_entry_by_posting_idx ON ledger_entry (posting_id);
CREATE INDEX ledger_entry_by_vendor_idx ON ledger_entry (vendor_id, account) WHERE vendor_id IS NOT NULL;
CREATE INDEX ledger_entry_by_reference_idx ON ledger_entry (reference_type, reference_id);

-- BR-LDG-04: no UPDATE or DELETE — corrections are contra entries, a new posting, never a
-- mutation of this one. Enforced at the application layer (no repository method exposes either),
-- matching every other append-only table in this codebase (auth_event, order_event,
-- payout_detail_access_event); not a database-level trigger, for the same reason none of those
-- have one either.
