alter table creators add column badge_level varchar(30) not null default 'NONE';

update creators
set badge_level = 'VERIFIED'
where is_verified = true;

create index idx_creators_badge_level on creators(badge_level);
