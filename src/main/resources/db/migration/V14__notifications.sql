set search_path to core;

-- In-app notifications table
create table if not exists notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    type varchar(60) not null,
    title varchar(200) not null,
    body varchar(500),
    entity_type varchar(60),
    entity_id uuid,
    is_read boolean not null default false,
    created_at timestamptz not null default now()
);

create index if not exists idx_notifications_user_unread on notifications(user_id, is_read, created_at desc);
create index if not exists idx_notifications_user_created on notifications(user_id, created_at desc);

