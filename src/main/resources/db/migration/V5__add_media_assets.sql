create table media_assets (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references users(id) on delete cascade,
    scope varchar(40) not null,
    entity_type varchar(40),
    entity_id uuid,
    public_id varchar(300) not null,
    asset_id varchar(100),
    resource_type varchar(20) not null,
    format varchar(20),
    secure_url varchar(800) not null,
    app_path varchar(800),
    thumbnail_url varchar(800),
    original_filename varchar(255),
    content_type varchar(120),
    bytes bigint not null,
    width integer,
    height integer,
    duration double precision,
    deleted boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_media_assets_owner on media_assets(owner_id, deleted, created_at desc);
create index idx_media_assets_entity on media_assets(owner_id, entity_type, entity_id, deleted);
create index idx_media_assets_app_path on media_assets(app_path) where app_path is not null and deleted = false;
create unique index idx_media_assets_public_id on media_assets(public_id) where deleted = false;
