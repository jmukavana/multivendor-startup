-- BR-NTF-01 to 05: the notification outbox and marketing opt-out preference.

create table notification_message (
    id uuid primary key,
    channel varchar(10) not null,
    recipient varchar(255) not null,
    subject varchar(255),
    body text not null,
    status varchar(20) not null,
    attempt_count int not null default 0,
    created_at timestamptz not null,
    sent_at timestamptz
);

-- BR-NTF-05: the retry sweeper and the admin exception queue both filter on status.
create index notification_message_status_idx on notification_message (status);

-- BR-NTF-04: retained per account; absence means "not opted out" (the default), matching
-- notification_preference having no row rather than a row with a false flag for every account
-- that has never touched the setting.
create table notification_preference (
    account_id uuid primary key,
    marketing_opt_out boolean not null default false,
    updated_at timestamptz not null
);
