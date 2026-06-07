set search_path to core;

alter table users
    add column if not exists email_verified boolean not null default false,
    add column if not exists google_subject varchar(128);

create unique index if not exists uk_users_google_subject
    on users (google_subject)
    where google_subject is not null;

