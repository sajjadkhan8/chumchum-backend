-- Persist bank institution name for payout methods (B7)
alter table payout_methods add column bank_name varchar(100);
