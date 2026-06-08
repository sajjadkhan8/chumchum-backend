set search_path to core;

create table if not exists brand_offers (
    id uuid primary key default gen_random_uuid(),
    brand_id uuid not null references brands(id) on delete cascade,
    title varchar(160) not null,
    brief varchar(2000) not null,
    offer_type varchar(40) not null,
    budget_min integer not null,
    budget_max integer not null,
    currency varchar(10) not null default 'PKR',
    deliverables text,
    requirements text,
    deadline_date date,
    target_city varchar(100),
    target_language varchar(100),
    min_followers integer,
    min_engagement_rate numeric(5,2),
    status varchar(30) not null default 'DRAFT',
    published_at timestamptz,
    closed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_brand_offers_budget check (budget_min >= 0 and budget_max >= 0 and budget_min <= budget_max),
    constraint ck_brand_offers_status check (status in ('DRAFT', 'PUBLISHED', 'PAUSED', 'CLOSED', 'ARCHIVED')),
    constraint ck_brand_offers_min_engagement_rate check (min_engagement_rate is null or (min_engagement_rate >= 0 and min_engagement_rate <= 100))
);

create table if not exists brand_offer_reactions (
    id uuid primary key default gen_random_uuid(),
    offer_id uuid not null references brand_offers(id) on delete cascade,
    creator_id uuid not null references creators(id) on delete cascade,
    reaction_type varchar(30) not null,
    status varchar(30) not null default 'SUBMITTED',
    message text,
    proposed_price integer,
    proposed_currency varchar(10) not null default 'PKR',
    proposed_delivery_days integer,
    brand_note text,
    creator_note text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_offer_reactions_type check (reaction_type in ('INTERESTED', 'PROPOSAL', 'QUESTION', 'DECLINE')),
    constraint ck_offer_reactions_status check (status in ('SUBMITTED', 'SHORTLISTED', 'IN_REVIEW', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    constraint ck_offer_reactions_proposed_price check (proposed_price is null or proposed_price >= 0),
    constraint ck_offer_reactions_proposed_days check (proposed_delivery_days is null or proposed_delivery_days > 0),
    constraint uk_offer_creator unique (offer_id, creator_id)
);

create index if not exists idx_brand_offers_brand_status on brand_offers(brand_id, status, created_at desc);
create index if not exists idx_brand_offers_feed on brand_offers(status, deadline_date, published_at desc);
create index if not exists idx_brand_offer_reactions_offer on brand_offer_reactions(offer_id, created_at desc);
create index if not exists idx_brand_offer_reactions_creator on brand_offer_reactions(creator_id, updated_at desc);

