-- BR-STL-01 to 12: settlement runs, their per-vendor lines, and manual holds.
--
-- BR-STL-04 (withhold orders still inside the return window) and BR-STL-05 (release holdback when
-- that window closes without a claim) are NOT modelled here. Both require the `returns` module
-- (BRD Phase 5), which does not exist yet — the BRD's own v2.2 roadmap (18, "why returns precede
-- settlement") says this sequence is not fully exercisable without it. This pass takes the same
-- explicit, documented reduction this codebase has taken at every other cross-phase dependency gap
-- (delivery cost in ledger_entry, refunds in the same table) rather than blocking on Phase 5:
-- every sub-order's holdback sits in HOLDBACK_RESERVE indefinitely, with no automatic release path,
-- until `returns` lands and BR-STL-05's trigger exists to call ledger.LedgerPosting.postHoldbackRelease.

-- BR-STL-10/11: a run passes reconciliation before it has any lines, and passes finance approval
-- before any line disburses. BLOCKED is a run reconciliation refused outright (AC-STL-02) — it
-- carries no lines and nothing can ever move it forward; a new run must be initiated instead.
CREATE TABLE settlement_run (
    id                      UUID   NOT NULL,
    currency                TEXT   NOT NULL,
    status                  TEXT   NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT settlement_run_status_check CHECK (status IN
            ('BLOCKED', 'DRAFT', 'APPROVED', 'COMPLETED')),
    created_at              TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    approved_by_account_id  UUID,
    approved_at             TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT settlement_run_pkey PRIMARY KEY (id)
);

-- One line per vendor per run. idempotency_key (BR-STL-12) is generated once at line creation and
-- never changes — every disbursement attempt for this line, including every retry, sends the same
-- key, which is what "at most one disbursement regardless of retry" actually requires: the key has
-- to be stable across attempts, not merely present.
CREATE TABLE settlement_line (
    id                      UUID   NOT NULL,
    settlement_run_id       UUID   NOT NULL,
    vendor_id               UUID   NOT NULL,
    amount_minor_units      BIGINT NOT NULL,
    currency                TEXT   NOT NULL,
    status                  TEXT   NOT NULL
        CONSTRAINT settlement_line_status_check CHECK (status IN
            ('PENDING', 'EXCLUDED', 'DISBURSING', 'DISBURSED', 'FAILED', 'ESCALATED')),
    exclusion_reason        TEXT,
    idempotency_key         TEXT   NOT NULL,
    attempt_count           INTEGER NOT NULL DEFAULT 0,
    disbursement_reference  TEXT,
    resolved_at             TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT settlement_line_pkey PRIMARY KEY (id),
    CONSTRAINT settlement_line_run_fkey FOREIGN KEY (settlement_run_id) REFERENCES settlement_run (id),
    CONSTRAINT settlement_line_amount_non_negative CHECK (amount_minor_units >= 0),
    CONSTRAINT settlement_line_idempotency_key_unique UNIQUE (idempotency_key)
);

CREATE INDEX settlement_line_by_run_idx ON settlement_line (settlement_run_id);
CREATE INDEX settlement_line_by_vendor_idx ON settlement_line (vendor_id);
-- BR-STL-07: the retry sweep scans exactly this.
CREATE INDEX settlement_line_retryable_idx ON settlement_line (status) WHERE status = 'FAILED';

-- BR-STL-09: a manual hold, with reason capture. A vendor may have more than one hold row over
-- time (released_at closes one), but at most one active — enforced in SettlementService, not here,
-- the same choice made for stock_reservation's status transitions.
CREATE TABLE settlement_hold (
    id                      UUID   NOT NULL,
    vendor_id               UUID   NOT NULL,
    reason                  TEXT   NOT NULL,
    created_by_account_id   UUID   NOT NULL,
    created_at              TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    released_by_account_id  UUID,
    released_at             TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT settlement_hold_pkey PRIMARY KEY (id)
);

CREATE INDEX settlement_hold_active_idx ON settlement_hold (vendor_id) WHERE released_at IS NULL;
