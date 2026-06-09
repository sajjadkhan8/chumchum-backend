set search_path to core;

alter table brand_offers
    add column if not exists content_formats varchar(300),
    add column if not exists target_platforms varchar(300),
    add column if not exists categories varchar(400),
    add column if not exists niches varchar(400),
    add column if not exists tags varchar(400),
    add column if not exists reference_urls text,
    add column if not exists cover_image_url varchar(600),
    add column if not exists preferred_delivery_days integer,
    add column if not exists slots integer,
    add column if not exists visibility varchar(20) not null default 'public';

alter table brand_offers
    add constraint ck_brand_offers_preferred_delivery_days
        check (preferred_delivery_days is null or preferred_delivery_days > 0),
    add constraint ck_brand_offers_slots
        check (slots is null or slots > 0),
    add constraint ck_brand_offers_visibility
        check (visibility in ('public', 'private'));

