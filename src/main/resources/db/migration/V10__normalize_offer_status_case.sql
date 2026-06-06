-- Normalize historical lowercase values so JPA enum mapping is stable.
update quick_deal_offers
set status = upper(status)
where status is not null
  and status <> upper(status);

-- Align default with enum constant casing.
alter table quick_deal_offers
    alter column status set default 'PENDING';

