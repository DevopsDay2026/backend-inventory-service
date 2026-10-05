create table stock (
    id        uuid primary key,
    sku       varchar(64) not null unique,
    available integer     not null check (available >= 0),
    version   bigint      not null
);

create table outbox (
    id           uuid primary key,
    aggregate_id uuid         not null,
    event_type   varchar(64)  not null,
    topic        varchar(128) not null,
    message_key  varchar(64)  not null,
    payload      text         not null,
    created_at   timestamptz  not null,
    sent_at      timestamptz
);

create index idx_outbox_pending on outbox (created_at) where sent_at is null;

create table processed_event (
    event_id     uuid primary key,
    processed_at timestamptz not null
);
