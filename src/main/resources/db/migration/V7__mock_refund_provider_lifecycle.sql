alter table payment_refunds add column provider varchar(40);
alter table payment_refunds add column provider_refund_id varchar(100);
alter table payment_refunds add column failure_reason varchar(500);
alter table payment_refunds add column confirmed_at timestamptz;
alter table payment_refunds add column updated_at timestamptz not null default now();

update payment_refunds
set provider = 'internal_legacy',
    provider_refund_id = 'legacy_' || id,
    confirmed_at = created_at;

alter table payment_refunds alter column provider set not null;
alter table payment_refunds alter column provider_refund_id set not null;
alter table payment_refunds add constraint uq_payment_refunds_provider_refund_id unique (provider_refund_id);
