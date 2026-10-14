-- BR-ADM-07: an immutable audit log. No update or delete statement is ever issued against this
-- table anywhere in this codebase.
create table audit_log_entry (
    id uuid primary key,
    actor_account_id uuid,
    action varchar(64) not null,
    entity_type varchar(64) not null,
    entity_id varchar(64) not null,
    detail text,
    occurred_at timestamptz not null
);

create index audit_log_entry_entity_idx on audit_log_entry (entity_type, entity_id);
create index audit_log_entry_occurred_at_idx on audit_log_entry (occurred_at desc);
