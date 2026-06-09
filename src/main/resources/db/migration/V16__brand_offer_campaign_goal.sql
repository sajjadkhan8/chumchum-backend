set search_path to core;

alter table brand_offers
    add column if not exists campaign_goal varchar(150);

