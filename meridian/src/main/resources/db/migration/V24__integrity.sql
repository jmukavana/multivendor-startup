-- BR-SEC-01/02/07/08.

-- The fixed-window rate-limit counter. rate_limit_key avoids the reserved word "key".
create table rate_limit_bucket (
    id uuid primary key,
    scope varchar(64) not null,
    rate_limit_key varchar(255) not null,
    window_start timestamptz not null,
    count int not null default 0,
    constraint rate_limit_bucket_unique unique (scope, rate_limit_key, window_start)
);

-- BR-SEC-07: a reversible restriction, never an account termination — see integrity.Restriction.
create table integrity_restriction (
    id uuid primary key,
    scope varchar(64) not null,
    rate_limit_key varchar(255) not null,
    reason text not null,
    status varchar(20) not null,
    created_at timestamptz not null,
    lifted_at timestamptz,
    lifted_by uuid,
    lift_reason text
);

create index integrity_restriction_status_idx on integrity_restriction (status);

-- BR-SEC-08: every automated restriction and every manual override, immutable — no update or
-- delete statement is ever issued against this table.
create table integrity_event (
    id uuid primary key,
    type varchar(40) not null,
    scope varchar(64) not null,
    rate_limit_key varchar(255) not null,
    detail text,
    actor_account_id uuid,
    occurred_at timestamptz not null
);

create index integrity_event_scope_key_idx on integrity_event (scope, rate_limit_key);
