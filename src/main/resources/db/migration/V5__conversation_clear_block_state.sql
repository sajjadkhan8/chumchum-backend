alter table conversations
    add column if not exists last_message_at timestamptz,
    add column if not exists cleared_at_creator timestamptz,
    add column if not exists cleared_at_brand timestamptz,
    add column if not exists blocked_at_creator timestamptz,
    add column if not exists blocked_at_brand timestamptz;

alter table messages
    add column if not exists attachment_original_name varchar(255);
