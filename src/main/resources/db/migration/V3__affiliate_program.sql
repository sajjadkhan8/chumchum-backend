set search_path to core;

create table affiliate_links (
    id uuid primary key default gen_random_uuid(),
    owner_user_id uuid not null references users(id) on delete cascade,
    code varchar(24) not null unique,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_affiliate_links_owner unique (owner_user_id)
);

create table affiliate_attributions (
    id uuid primary key default gen_random_uuid(),
    affiliate_link_id uuid not null references affiliate_links(id) on delete restrict,
    affiliate_owner_user_id uuid not null references users(id) on delete restrict,
    referred_creator_id uuid not null references creators(id) on delete cascade,
    source_code varchar(24) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_affiliate_attributions_referred_creator unique (referred_creator_id)
);

create table affiliate_commissions (
    id uuid primary key default gen_random_uuid(),
    affiliate_owner_user_id uuid not null references users(id) on delete restrict,
    earning_creator_id uuid not null references creators(id) on delete restrict,
    order_id uuid not null references orders(id) on delete restrict,
    base_amount integer not null,
    rate_basis_points integer not null,
    commission_amount integer not null,
    status varchar(40) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_affiliate_commissions_order unique (order_id)
);

create index idx_affiliate_links_owner on affiliate_links(owner_user_id);
create index idx_affiliate_links_code on affiliate_links(lower(code));
create index idx_affiliate_attributions_owner on affiliate_attributions(affiliate_owner_user_id);
create index idx_affiliate_commissions_owner_date on affiliate_commissions(affiliate_owner_user_id, created_at desc);
create index idx_affiliate_commissions_creator on affiliate_commissions(earning_creator_id);
