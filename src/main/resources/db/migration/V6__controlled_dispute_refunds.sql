create table payment_refunds (
    id uuid primary key default gen_random_uuid(),
    dispute_id uuid not null unique references dispute_cases(id) on delete restrict,
    order_id uuid not null references orders(id) on delete restrict,
    executed_by_admin_id uuid not null references users(id) on delete restrict,
    amount integer not null check (amount > 0),
    reason varchar(500) not null,
    status varchar(30) not null,
    creator_clawback_amount integer not null default 0,
    created_at timestamptz not null default now()
);

create index idx_payment_refunds_order on payment_refunds(order_id);
create index idx_payment_refunds_created on payment_refunds(created_at desc);
