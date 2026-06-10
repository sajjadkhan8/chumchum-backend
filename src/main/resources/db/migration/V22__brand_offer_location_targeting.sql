alter table core.brand_offers
    add column if not exists location_targeting_mode varchar(30),
    add column if not exists target_cities text,
    add column if not exists target_region varchar(100);

update core.brand_offers
set location_targeting_mode = case
    when lower(coalesce(target_city, '')) in ('remote / online only', 'remote-only', 'remote') then 'remote_only'
    when coalesce(target_city, '') = '' then 'nationwide'
    else 'cities'
end
where location_targeting_mode is null;

update core.brand_offers
set target_cities = target_city
where location_targeting_mode = 'cities'
  and coalesce(target_city, '') <> ''
  and coalesce(target_cities, '') = '';

