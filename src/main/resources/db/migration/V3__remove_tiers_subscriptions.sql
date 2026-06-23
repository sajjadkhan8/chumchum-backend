-- Remove package tiers and subscriptions (MVP simplification)
drop table if exists package_tiers;
drop table if exists subscriptions;
alter table packages drop column if exists type;
alter table packages drop column if exists subscription_interval;
alter table packages drop column if exists subscription_duration;
