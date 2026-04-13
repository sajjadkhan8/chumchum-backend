create table users (
    id uuid primary key,
    username varchar(40) not null unique,
    email varchar(120) not null unique,
    password_hash varchar(255) not null,
    image varchar(500),
    city varchar(80) not null,
    phone varchar(30) not null,
    description varchar(1000) not null,
    seller boolean not null default false,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table gigs (
    id uuid primary key,
    seller_id uuid not null references users(id) on delete cascade,
    title varchar(120) not null,
    description varchar(2500) not null,
    total_stars integer not null default 0,
    star_number integer not null default 0,
    category varchar(80) not null,
    price numeric(10,2) not null,
    cover varchar(500) not null,
    short_title varchar(120) not null,
    short_description varchar(600) not null,
    delivery_time varchar(20) not null,
    revision_number integer not null,
    sales integer not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table gig_images (
    gig_id uuid not null references gigs(id) on delete cascade,
    image_url varchar(500)
);

create table gig_features (
    gig_id uuid not null references gigs(id) on delete cascade,
    feature varchar(255)
);

create table reviews (
    id uuid primary key,
    gig_id uuid not null references gigs(id) on delete cascade,
    reviewer_id uuid not null references users(id) on delete cascade,
    star integer not null check (star between 1 and 5),
    description varchar(1000) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_review_user_gig unique (gig_id, reviewer_id)
);

create table orders (
    id uuid primary key,
    gig_id uuid not null references gigs(id) on delete restrict,
    seller_id uuid not null references users(id) on delete restrict,
    buyer_id uuid not null references users(id) on delete restrict,
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
    seller_id uuid not null references users(id) on delete cascade,
    buyer_id uuid not null references users(id) on delete cascade,
    read_by_seller boolean not null,
    read_by_buyer boolean not null,
    last_message varchar(2000),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_conversation_pair unique (seller_id, buyer_id)
);

create table messages (
    id uuid primary key,
    conversation_id uuid not null references conversations(id) on delete cascade,
    sender_id uuid not null references users(id) on delete cascade,
    description varchar(2000) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_gigs_seller_id on gigs (seller_id);
create index idx_gigs_category_title on gigs (category, title);
create index idx_orders_seller_completed on orders (seller_id, completed);
create index idx_orders_buyer_completed on orders (buyer_id, completed);
create index idx_conversations_seller on conversations (seller_id, updated_at desc);
create index idx_conversations_buyer on conversations (buyer_id, updated_at desc);
create index idx_messages_conversation_created on messages (conversation_id, created_at);

