create extension if not exists "pgcrypto";

create schema if not exists core;
set search_path to core;

create table users (
    id uuid primary key default gen_random_uuid(),
    username varchar(40) not null unique,
    email varchar(120) not null unique,
    password_hash varchar(255) not null,
    role varchar(20) not null constraint ck_users_role check (role in ('CREATOR', 'BRAND', 'ADMIN')),
    image varchar(500),
    city varchar(80),
    phone varchar(30),
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table creators (
    id uuid primary key default gen_random_uuid(),
    user_id uuid unique references users(id) on delete cascade,
    bio varchar(1000),
    category varchar(100),
    tiktok_url varchar(255),
    instagram_url varchar(255),
    youtube_url varchar(255),
    followers int not null default 0,
    avg_views int not null default 0,
    engagement_rate numeric(5,2),
    rating numeric(3,2) not null default 0,
    total_reviews int not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table brands (
    id uuid primary key default gen_random_uuid(),
    user_id uuid unique references users(id) on delete cascade,
    company_name varchar(150) not null,
    website varchar(255),
    industry varchar(100),
    description varchar(1000),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table packages (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null references creators(id) on delete cascade,
    title varchar(150) not null,
    description varchar(2000),
    platform varchar(50) not null,
    category varchar(80),
    type varchar(30) not null, -- ONE_TIME / SUBSCRIPTION
    pricing_type varchar(30) not null default 'PAID', -- PAID / BARTER
    barter_details varchar(1000),    -- e.g. "Free meal for 2 worth PKR 5000"
    price numeric(10,2) not null,
    currency varchar(10) default 'PKR',
    deliverables varchar(1000) not null,
    delivery_days int not null,
    duration_days int,
    revisions int default 1,
    cover_image varchar(500),
    media_urls text[],
    tags text[],
    is_active boolean default true,
    is_featured boolean default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table package_tiers (
    id uuid primary key default gen_random_uuid(),
    package_id uuid references packages(id) on delete cascade,
    name varchar(50) not null, -- BASIC / STANDARD / PREMIUM
    price numeric(10,2),
    deliverables varchar(1000),
    delivery_days int,
    revisions int default 1,
    created_at timestamptz default now()
);

create table reviews (
    id uuid primary key,
    package_id uuid not null references packages(id) on delete cascade,
    reviewer_id uuid not null references users(id) on delete cascade,
    star integer not null check (star between 1 and 5),
    description varchar(1000) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_review_user_package unique (package_id, reviewer_id)
);

create table orders (
    id uuid primary key,
    package_id uuid not null references packages(id) on delete restrict,
    creator_id uuid not null references creators(id) on delete restrict,
    brand_id uuid not null references users(id) on delete restrict,
    image varchar(500),
    title varchar(255) not null,
    price numeric(10,2) not null,
    completed boolean not null default false,
    payment_intent varchar(150) not null unique,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table conversations (
    id uuid primary key,
    creator_id uuid not null references users(id) on delete cascade,
    brand_id uuid not null references users(id) on delete cascade,
    read_by_creator boolean not null,
    read_by_brand boolean not null,
    last_message varchar(2000),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_conversation_pair unique (creator_id, brand_id)
);

create table messages (
    id uuid primary key,
    conversation_id uuid not null references conversations(id) on delete cascade,
    sender_id uuid not null references users(id) on delete cascade,
    description varchar(2000) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_packages_creator_id on packages (creator_id);
create index idx_packages_category_title on packages (category, title);
create index idx_package_tiers_package_id on package_tiers (package_id);
create index idx_creators_user_id on creators (user_id);
create index idx_brands_user_id on brands (user_id);
create index idx_orders_creator_completed on orders (creator_id, completed);
create index idx_orders_brand_completed on orders (brand_id, completed);
create index idx_conversations_creator on conversations (creator_id, updated_at desc);
create index idx_conversations_brand on conversations (brand_id, updated_at desc);
create index idx_messages_conversation_created on messages (conversation_id, created_at);

