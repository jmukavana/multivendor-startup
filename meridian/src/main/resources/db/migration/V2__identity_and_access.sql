-- Identity and access (BR-IAM-01 to BR-IAM-09).
--
-- Accounts are identified by phone number or email address, either of which may be absent:
-- BR-IAM-01 allows registration by one or the other, and the Kenyan market is phone-first.
-- Uniqueness is therefore enforced per identifier rather than on a single username column.

CREATE TABLE account (
    id                UUID         NOT NULL,
    phone_number      TEXT,
    email             TEXT,
    display_name      TEXT         NOT NULL,
    status            TEXT         NOT NULL
        CONSTRAINT account_status_check CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'CLOSED')),
    phone_verified_at TIMESTAMP(6) WITH TIME ZONE,
    email_verified_at TIMESTAMP(6) WITH TIME ZONE,
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT account_pkey PRIMARY KEY (id),
    -- An account with neither identifier can never authenticate, so the database refuses it
    -- rather than leaving an unreachable row for someone to discover later.
    CONSTRAINT account_identifier_present CHECK (phone_number IS NOT NULL OR email IS NOT NULL)
);

-- Partial unique indexes rather than plain UNIQUE: NULLs are distinct in PostgreSQL, so a plain
-- constraint would permit many accounts with no phone number but would silently do nothing to
-- enforce the rule we actually want, which is uniqueness among the values that exist.
CREATE UNIQUE INDEX account_phone_number_key ON account (phone_number) WHERE phone_number IS NOT NULL;
CREATE UNIQUE INDEX account_email_key ON account (LOWER(email)) WHERE email IS NOT NULL;

-- BR-IAM-03: role-based access control covering the actors in BRD Section 8. Roles are rows, not
-- a column, because an account can hold more than one — a vendor owner who also buys.
CREATE TABLE account_role (
    account_id UUID NOT NULL,
    role       TEXT NOT NULL
        CONSTRAINT account_role_check CHECK (role IN (
            'BUYER', 'VENDOR_OWNER', 'VENDOR_STAFF', 'PLATFORM_ADMIN', 'FINANCE_ADMIN', 'SUPER_ADMIN')),
    granted_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT account_role_pkey PRIMARY KEY (account_id, role),
    CONSTRAINT account_role_account_fkey FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE
);

-- Opaque session tokens, per ADR-008. The plaintext token exists only in the response that issues
-- it; this table holds an SHA-256 hash.
CREATE TABLE session_token (
    id             UUID         NOT NULL,
    account_id     UUID         NOT NULL,
    token_hash     TEXT         NOT NULL,
    issued_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expires_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked_at     TIMESTAMP(6) WITH TIME ZONE,
    last_used_at   TIMESTAMP(6) WITH TIME ZONE,
    source_address TEXT,
    user_agent     TEXT,
    CONSTRAINT session_token_pkey PRIMARY KEY (id),
    CONSTRAINT session_token_account_fkey FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE
);

-- Every authenticated request resolves a token by hash, so this index is on the hot path.
CREATE UNIQUE INDEX session_token_hash_key ON session_token (token_hash);
-- Supports "sign out everywhere" and admin-forced revocation for one account.
CREATE INDEX session_token_by_account_idx ON session_token (account_id) WHERE revoked_at IS NULL;

-- BR-IAM-02: phone verification by one-time code before checkout.
CREATE TABLE otp_challenge (
    id          UUID         NOT NULL,
    account_id  UUID         NOT NULL,
    purpose     TEXT         NOT NULL
        CONSTRAINT otp_challenge_purpose_check CHECK (purpose IN ('PHONE_VERIFICATION', 'EMAIL_VERIFICATION', 'SIGN_IN')),
    code_hash   TEXT         NOT NULL,
    issued_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expires_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP(6) WITH TIME ZONE,
    attempts    INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT otp_challenge_pkey PRIMARY KEY (id),
    CONSTRAINT otp_challenge_account_fkey FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE
);

-- Verification looks up the newest live challenge for an account and purpose.
CREATE INDEX otp_challenge_live_idx ON otp_challenge (account_id, purpose, issued_at DESC) WHERE consumed_at IS NULL;

-- BR-IAM-06: log all authentication events with timestamp, actor and source address.
--
-- Append-only, and deliberately not foreign-keyed to account: a failed sign-in for an identifier
-- that does not exist is exactly the event worth recording, and a foreign key would forbid it.
CREATE TABLE auth_event (
    id             UUID         NOT NULL,
    account_id     UUID,
    event_type     TEXT         NOT NULL,
    occurred_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    source_address TEXT,
    user_agent     TEXT,
    -- Never the identifier itself in plaintext beyond what is needed to investigate, and never a
    -- code or token.
    detail         TEXT,
    CONSTRAINT auth_event_pkey PRIMARY KEY (id)
);

CREATE INDEX auth_event_by_account_idx ON auth_event (account_id, occurred_at DESC);
CREATE INDEX auth_event_by_time_idx ON auth_event (occurred_at DESC);
