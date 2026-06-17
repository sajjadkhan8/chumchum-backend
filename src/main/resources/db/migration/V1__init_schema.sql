create extension if not exists "pgcrypto";

create schema if not exists core;
set search_path to core;

create table users (
    id uuid primary key default gen_random_uuid(),
    username varchar(40) not null unique,
    email varchar(120) unique,
    password_hash varchar(255),
    role varchar(20) not null constraint ck_users_role check (role in ('CREATOR', 'BRAND', 'PLATFORM_ADMIN')),
    name varchar(100),
    image varchar(500),
    avatar_url varchar(500),
    city varchar(80),
    phone varchar(30),
    creator_program_status varchar(40) not null default 'NONE',
    email_verified boolean not null default false,
    google_subject varchar(128),
    is_active boolean not null default true,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table auth_refresh_tokens (
    token_hash varchar(64) primary key,
    user_id uuid not null references users(id) on delete cascade,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now()
);

create table auth_password_reset_tokens (
    token_hash varchar(64) primary key,
    user_id uuid not null references users(id) on delete cascade,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);

create table auth_otp_challenges (
    phone varchar(30) primary key,
    otp_hash varchar(64) not null,
    attempts integer not null default 0,
    expires_at timestamptz not null,
    sent_at timestamptz not null default now()
);

create table auth_rate_limits (
    id uuid primary key default gen_random_uuid(),
    action varchar(40) not null,
    identifier varchar(160) not null,
    attempts integer not null default 0,
    window_started_at timestamptz not null,
    blocked_until timestamptz,
    unique (action, identifier)
);

create table creators (
    id uuid primary key references users(id) on delete cascade,
    username varchar(50),
    bio varchar(1000),
    category varchar(100),
    cover_image_url varchar(500),
    website varchar(300),
    niche varchar(100),
    availability_status varchar(30) constraint ck_creators_availability_status check (availability_status in ('AVAILABLE', 'BUSY', 'UNAVAILABLE', 'ON_VACATION')),
    response_time varchar(50),
    min_price integer,
    max_price integer,
    tiktok_url varchar(255),
    instagram_url varchar(255),
    youtube_url varchar(255),
    facebook_url varchar(255),
    followers int not null default 0,
    avg_views int not null default 0,
    engagement_rate numeric(5,2),
    is_verified boolean not null default false,
    is_trending boolean not null default false,
    is_fast_responder boolean not null default false,
    rating numeric(3,2) not null default 0,
    total_reviews int not null default 0,
    completed_deals integer not null default 0,
    badge_level varchar(30) not null default 'NONE',
    accepts_barter boolean not null default true,
    accepts_hybrid_deals boolean not null default true,
    minimum_budget integer,
    preferred_industries text,
    languages jsonb not null default '[]'::jsonb,
    categories jsonb not null default '[]'::jsonb
);

create table brands (
    id uuid primary key references users(id) on delete cascade,
    name varchar(150) not null,
    logo_url varchar(500),
    website varchar(255),
    industry varchar(100),
    description varchar(1000),
    monthly_budget integer,
    preferred_creator_categories varchar(500),
    target_cities varchar(500),
    target_platforms varchar(500),
    campaign_budget_range varchar(150),
    business_verification_status varchar(50),
    verification_contact_email varchar(255),
    verification_phone_number varchar(50),
    plan_tier varchar(20) not null default 'STARTER'
);

create table creator_payout_preferences (
    creator_id uuid primary key references creators(id) on delete cascade,
    auto_withdraw_enabled boolean not null default false,
    payout_schedule varchar(20) not null default 'MANUAL' constraint ck_creator_payout_schedule check (payout_schedule in ('WEEKLY', 'BIWEEKLY', 'MONTHLY', 'MANUAL')),
    minimum_payout_amount integer not null default 5000,
    account_holder_name varchar(120) not null default '',
    ntn_number varchar(30) not null default '',
    cnic_last4 varchar(4) not null default '',
    earnings_notifications_enabled boolean not null default true,
    weekly_digest_enabled boolean not null default false,
    updated_at timestamptz not null default now()
);

create table brand_wallets (
    brand_id uuid primary key references brands(id) on delete cascade,
    wallet_balance integer not null default 0,
    monthly_spend integer not null default 0,
    pending_escrow integer not null default 0,
    processing_payouts integer not null default 0,
    next_invoice_date timestamptz,
    updated_at timestamptz not null default now()
);

create table brand_payment_methods (
    id uuid primary key default gen_random_uuid(),
    brand_id uuid not null references brands(id) on delete cascade,
    type varchar(30) not null constraint ck_brand_payment_method_type check (type in ('CARD', 'BANK_TRANSFER', 'JAZZCASH', 'EASYPAISA', 'SADAPAY', 'NAYAPAY')),
    label varchar(100) not null,
    account_mask varchar(120) not null,
    holder_name varchar(120) not null,
    is_default boolean not null default false,
    status varchar(30) not null default 'ACTIVE' constraint ck_brand_payment_method_status check (status in ('ACTIVE', 'PENDING_VERIFICATION', 'DISABLED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table brand_invoices (
    id uuid primary key default gen_random_uuid(),
    brand_id uuid not null references brands(id) on delete cascade,
    period_label varchar(40) not null,
    amount integer not null,
    status varchar(20) not null default 'DUE' constraint ck_brand_invoice_status check (status in ('PAID', 'DUE', 'OVERDUE')),
    issued_at timestamptz not null,
    due_at timestamptz not null,
    created_at timestamptz not null default now()
);

create table packages (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null references creators(id) on delete cascade,
    name varchar(100) not null default 'Unnamed Package',
    title varchar(150) not null,
    description varchar(2000),
    platform varchar(50) not null constraint ck_packages_platform check (platform in ('YOUTUBE', 'INSTAGRAM', 'TIKTOK', 'FACEBOOK')),
    category varchar(50) constraint ck_packages_category check (category in ('FASHION_BEAUTY', 'FOOD_BEVERAGE', 'TECHNOLOGY_GADGETS', 'FITNESS_HEALTH', 'TRAVEL_LIFESTYLE', 'ENTERTAINMENT_COMEDY', 'EDUCATION_CAREER', 'BUSINESS_FINANCE', 'HOME_DECOR', 'GAMING', 'PARENTING_FAMILY', 'SPORTS', 'AUTOMOTIVE', 'RELIGIOUS_SPIRITUAL', 'GENERAL', 'QUICK_DEAL')),
    type varchar(30),
    pricing_type varchar(30) not null default 'PAID',
    deal_type varchar(30),
    barter_details varchar(1000),
    price integer,
    currency varchar(10) default 'PKR',
    deliverables jsonb not null default '[]'::jsonb,
    delivery_days int not null,
    duration_days int,
    revisions int default 1,
    cover_image varchar(500),
    thumbnail_url varchar(500),
    media_urls text[],
    tags jsonb not null default '[]'::jsonb,
    status varchar(30) not null default 'draft',
    visibility varchar(30) not null default 'public',
    short_description varchar(300),
    full_description text,
    barter_description text,
    barter_category varchar(100),
    estimated_barter_value integer,
    hybrid_cash_amount integer,
    hybrid_barter_value integer,
    creator_expectations text,
    is_active boolean default true,
    is_featured boolean default false,
    is_popular boolean not null default false,
    orders_completed integer not null default 0,
    response_time varchar(50),
    subscription_interval varchar(20),
    subscription_duration int,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table subscriptions (
    id uuid primary key default gen_random_uuid(),
    brand_id uuid not null references brands(id) on delete cascade,
    package_id uuid not null references packages(id) on delete cascade,
    status varchar(20) not null default 'ACTIVE',
    interval varchar(20) not null,
    duration int not null,
    cycles_completed int not null default 0,
    next_renewal_at timestamptz not null,
    cancelled_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table package_tiers (
    id uuid primary key default gen_random_uuid(),
    package_id uuid not null references packages(id) on delete cascade,
    name varchar(50) not null,
    description text,
    price integer,
    deliverables jsonb not null default '[]'::jsonb,
    delivery_days int,
    revisions int default 1,
    position integer not null default 0,
    is_primary boolean not null default false,
    created_at timestamptz default now(),
    updated_at timestamptz not null default now()
);

create table orders (
    id uuid primary key,
    package_id uuid not null references packages(id) on delete restrict,
    creator_id uuid not null references creators(id) on delete restrict,
    brand_id uuid not null references brands(id) on delete restrict,
    order_number varchar(20),
    deal_type varchar(30) not null default 'paid',
    amount integer,
    barter_details text,
    message text,
    status varchar(30) not null default 'pending',
    progress integer not null default 0,
    delivery_date date,
    deadline_date timestamptz,
    -- Optional client-supplied key (UUID or similar) to make order creation idempotent.
    -- Duplicate requests with the same key return the original order without re-charging escrow.
    idempotency_key varchar(64),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table brand_disbursements (
    id uuid primary key default gen_random_uuid(),
    brand_id uuid not null references brands(id) on delete cascade,
    creator_id uuid references creators(id) on delete set null,
    order_id uuid references orders(id) on delete set null,
    campaign_name varchar(180) not null,
    amount integer not null,
    status varchar(30) not null default 'SCHEDULED' constraint ck_brand_disbursement_status check (status in ('SCHEDULED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    release_date timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table brand_payout_controls (
    brand_id uuid primary key references brands(id) on delete cascade,
    require_two_approvals boolean not null default true,
    auto_release_after_days integer not null default 5,
    low_balance_alert_threshold integer not null default 300000,
    updated_at timestamptz not null default now()
);

create table brand_payment_access (
    id uuid primary key default gen_random_uuid(),
    brand_id uuid not null references brands(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    role varchar(20) not null constraint ck_brand_payment_access_role check (role in ('OWNER', 'ADMIN', 'FINANCE', 'VIEWER')),
    created_at timestamptz not null default now(),
    unique (brand_id, user_id)
);

create table payment_audit_logs (
    id uuid primary key default gen_random_uuid(),
    actor_id uuid not null references users(id) on delete restrict,
    brand_id uuid references brands(id) on delete set null,
    action varchar(80) not null,
    target_type varchar(60) not null,
    target_id varchar(100),
    details text,
    created_at timestamptz not null default now()
);

create table reviews (
    id uuid primary key,
    package_id uuid not null references packages(id) on delete cascade,
    reviewer_id uuid not null references users(id) on delete cascade,
    order_id uuid references orders(id) on delete set null,
    creator_id uuid references creators(id) on delete cascade,
    brand_id uuid references brands(id) on delete cascade,
    star integer check (star between 1 and 5),
    rating integer,
    description varchar(1000) not null,
    comment text,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table conversations (
    id uuid primary key,
    creator_id uuid not null references creators(id) on delete cascade,
    brand_id uuid not null references brands(id) on delete cascade,
    read_by_creator boolean not null,
    read_by_brand boolean not null,
    unread_count_creator integer not null default 0,
    unread_count_brand integer not null default 0,
    last_message varchar(2000),
    last_message_id uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_conversation_pair unique (creator_id, brand_id)
);

create table messages (
    id uuid primary key,
    conversation_id uuid not null references conversations(id) on delete cascade,
    sender_id uuid not null references users(id) on delete cascade,
    content varchar(2000),
    description varchar(2000),
    type varchar(30) not null default 'TEXT',
    is_read boolean not null default false,
    attachment_url varchar(500),
    sender_type varchar(20),
    offer_deal_type varchar(30),
    offer_amount integer,
    offer_barter_details text,
    offer_barter_category varchar(100),
    offer_estimated_barter_value integer,
    offer_creator_expectation text,
    offer_message text,
    offer_status varchar(30),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table deliverables (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null references orders(id) on delete cascade,
    name varchar(200),
    status varchar(30) not null default 'PENDING',
    file_url varchar(500),
    submitted_at timestamptz,
    revision_note varchar(1000),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table social_accounts (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null references creators(id) on delete cascade,
    platform varchar(30) not null,
    username varchar(100) not null,
    profile_url varchar(500),
    followers integer not null default 0,
    avg_views integer,
    engagement_rate numeric(5,2) not null default 0,
    is_verified boolean not null default false,
    -- SELF = creator-entered; PLATFORM_REVIEWED = manually checked by team; API_CONNECTED = pulled via OAuth
    verified_by varchar(30) not null default 'SELF',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table content_previews (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null references creators(id) on delete cascade,
    type varchar(20) not null,
    thumbnail_url varchar(500) not null,
    media_url varchar(500) not null,
    platform varchar(30) not null,
    views integer,
    likes integer,
    created_at timestamptz not null default now()
);

create table package_analytics (
    package_id uuid primary key references packages(id) on delete cascade,
    views integer not null default 0,
    clicks integer not null default 0,
    inquiries integer not null default 0,
    conversion_rate numeric(5,2) not null default 0,
    completion_rate numeric(5,2) not null default 0,
    repeat_brands integer not null default 0,
    engagement_performance numeric(5,2) not null default 0,
    updated_at timestamptz not null default now()
);

create table wallets (
    creator_id uuid primary key references creators(id) on delete cascade,
    total_earned integer not null default 0,
    available_balance integer not null default 0,
    pending_balance integer not null default 0,
    updated_at timestamptz not null default now()
);

create table transactions (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null references creators(id) on delete cascade,
    order_id uuid references orders(id) on delete set null,
    type varchar(30) not null,
    amount integer not null,
    description varchar(300) not null,
    status varchar(30) not null default 'pending',
    created_at timestamptz not null default now()
);

create table payout_methods (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null references creators(id) on delete cascade,
    type varchar(30) not null,
    name varchar(100) not null,
    account_details varchar(300) not null,
    is_default boolean not null default false,
    created_at timestamptz not null default now()
);

create table withdrawal_requests (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null references creators(id) on delete cascade,
    payout_method_id uuid not null references payout_methods(id) on delete restrict,
    amount integer not null,
    status varchar(30) not null default 'pending',
    processed_at timestamptz,
    created_at timestamptz not null default now()
);

create table ambassador_applications (
    id uuid primary key default gen_random_uuid(),
    creator_id uuid not null unique references creators(id) on delete cascade,
    status varchar(30) not null default 'draft',
    submitted_at timestamptz,
    identity_verified boolean not null default false,
    engagement_verified boolean not null default false,
    content_review_passed boolean not null default false,
    background_check_passed boolean not null default false,
    notes text,
    rejection_reason text,
    approved_at timestamptz,
    reviewed_by uuid references users(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table ambassador_scores (
    creator_id uuid primary key references creators(id) on delete cascade,
    total integer not null default 0,
    delivery_score integer not null default 0,
    account_age_score integer not null default 0,
    rating_score integer not null default 0,
    cancellation_score integer not null default 0,
    profile_completeness_score integer not null default 0,
    consistency_score integer not null default 0,
    tier varchar(50) not null default 'rising_creator',
    percentile_rank integer not null default 0,
    strengths jsonb not null default '[]'::jsonb,
    improvements jsonb not null default '[]'::jsonb,
    calculated_at timestamptz not null default now()
);

create table saved_creators (
    brand_id uuid not null references brands(id) on delete cascade,
    creator_id uuid not null references creators(id) on delete cascade,
    saved_at timestamptz not null default now(),
    primary key (brand_id, creator_id)
);

create table notification_preferences (
    user_id uuid primary key references users(id) on delete cascade,
    new_orders boolean not null default true,
    messages boolean not null default true,
    reviews boolean not null default true,
    marketing boolean not null default false,
    weekly_digest boolean not null default true,
    push_notifications boolean not null default true,
    email_notifications boolean not null default true,
    sms_notifications boolean not null default false
);

create table quick_deal_offers (
    id uuid primary key default gen_random_uuid(),
    message_id uuid references messages(id) on delete set null,
    conversation_id uuid not null references conversations(id) on delete cascade,
    deal_type varchar(30) not null,
    amount integer,
    barter_details text,
    barter_category varchar(100),
    estimated_barter_value integer,
    creator_expectation text,
    message text not null,
    status varchar(30) not null default 'PENDING',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table brand_campaigns (
    id uuid primary key default gen_random_uuid(),
    brand_id uuid not null references brands(id) on delete cascade,
    title varchar(160) not null,
    brief varchar(2000) not null,
    offer_type varchar(40) not null,
    budget_min integer not null,
    budget_max integer not null,
    currency varchar(10) not null default 'PKR',
    deliverables text,
    deadline_date date,
    target_city varchar(100),
    target_language varchar(100),
    status varchar(30) not null default 'DRAFT',
    published_at timestamptz,
    closed_at timestamptz,
    content_formats varchar(300),
    target_platforms varchar(300),
    categories varchar(400),
    niches varchar(400),
    reference_urls text,
    cover_image_url varchar(600),
    visibility varchar(20) not null default 'public',
    campaign_goal varchar(150),
    budget_type varchar(30),
    payment_structure varchar(30),
    barter_product_desc text,
    barter_estimated_value integer,
    travel_costs_covered boolean not null default false,
    creator_type varchar(50),
    follower_range varchar(50),
    creator_gender_preference varchar(20),
    min_age int,
    max_age int,
    application_type varchar(50),
    max_applicants int,
    proposal_required boolean not null default false,
    portfolio_required boolean not null default false,
    custom_screening_questions text,
    content_submission_deadline date,
    go_live_date date,
    campaign_duration int,
    key_message text,
    dos_and_donts text,
    hashtags_mentions text,
    usage_rights text,
    terms_and_conditions text,
    expected_outcomes text,
    location_targeting_mode varchar(30),
    target_cities text,
    target_region varchar(100),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_brand_campaigns_budget check (budget_min >= 0 and budget_max >= 0 and budget_min <= budget_max),
    constraint ck_brand_campaigns_status check (status in ('DRAFT', 'PUBLISHED', 'PAUSED', 'CLOSED', 'ARCHIVED')),
    constraint ck_brand_campaigns_visibility check (visibility in ('public', 'private'))
);

create table brand_campaign_reactions (
    id uuid primary key default gen_random_uuid(),
    campaign_id uuid not null references brand_campaigns(id) on delete cascade,
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
    constraint ck_campaign_reactions_type check (reaction_type in ('INTERESTED', 'PROPOSAL', 'QUESTION', 'DECLINE')),
    constraint ck_campaign_reactions_status check (status in ('SUBMITTED', 'SHORTLISTED', 'IN_REVIEW', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    constraint ck_campaign_reactions_proposed_price check (proposed_price is null or proposed_price >= 0),
    constraint ck_campaign_reactions_proposed_days check (proposed_delivery_days is null or proposed_delivery_days > 0),
    constraint uk_campaign_creator unique (campaign_id, creator_id)
);

create table notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    type varchar(60) not null,
    title varchar(200) not null,
    body varchar(500),
    entity_type varchar(60),
    entity_id uuid,
    is_read boolean not null default false,
    created_at timestamptz not null default now()
);

create table dispute_cases (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null references orders(id) on delete cascade,
    title varchar(200) not null,
    description text not null,
    status varchar(40) not null default 'OPEN',
    priority varchar(20) not null default 'normal',
    assigned_admin_id uuid references users(id) on delete set null,
    resolution varchar(40) not null default 'NONE',
    resolution_notes text,
    resolved_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table admin_audit_logs (
    id uuid primary key default gen_random_uuid(),
    admin_id uuid not null references users(id) on delete restrict,
    action varchar(80) not null,
    target_type varchar(60) not null,
    target_id varchar(100),
    details text,
    created_at timestamptz not null default now()
);

create table payment_refunds (
    id uuid primary key default gen_random_uuid(),
    dispute_id uuid not null unique references dispute_cases(id) on delete restrict,
    order_id uuid not null references orders(id) on delete restrict,
    executed_by_admin_id uuid not null references users(id) on delete restrict,
    amount integer not null check (amount > 0),
    reason varchar(500) not null,
    status varchar(30) not null,
    creator_clawback_amount integer not null default 0,
    provider varchar(40) not null,
    provider_refund_id varchar(100) not null,
    failure_reason varchar(500),
    confirmed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_payment_refunds_provider_refund_id unique (provider_refund_id)
);

create index idx_packages_creator_id on packages (creator_id);
create index idx_packages_category_title on packages (category, title);
create index idx_packages_status on packages (status);
create index idx_packages_deal_type on packages (deal_type);
create index idx_packages_creator_status on packages (creator_id, status);
create index idx_package_tiers_package_id on package_tiers (package_id);
create index idx_package_tiers_package_id_position on package_tiers(package_id, position);
create index idx_orders_creator_status on orders (creator_id, status);
create index idx_orders_brand_status on orders (brand_id, status);
create unique index uk_orders_idempotency_key on orders (idempotency_key) where idempotency_key is not null;
create index idx_conversations_creator on conversations (creator_id, updated_at desc);
create index idx_conversations_brand on conversations (brand_id, updated_at desc);
create index idx_messages_conversation_created on messages (conversation_id, created_at);
create index idx_users_deleted_at on users (deleted_at) where deleted_at is not null;
create index idx_auth_refresh_tokens_user on auth_refresh_tokens(user_id);
create index idx_auth_refresh_tokens_expires on auth_refresh_tokens(expires_at);
create index idx_auth_password_reset_tokens_user on auth_password_reset_tokens(user_id);
create index idx_auth_password_reset_tokens_expires on auth_password_reset_tokens(expires_at);
create index idx_auth_rate_limits_blocked on auth_rate_limits(blocked_until);
create index idx_deliverables_order_id on deliverables(order_id);
create index idx_social_accounts_creator_id on social_accounts(creator_id);
create index idx_content_previews_creator_id on content_previews(creator_id);
create index idx_transactions_creator_type on transactions(creator_id, type);
create index idx_transactions_creator_date on transactions(creator_id, created_at desc);
create index idx_payout_methods_creator_id on payout_methods(creator_id);
create index idx_withdrawals_creator_id on withdrawal_requests(creator_id);
create index idx_saved_creators_brand_id on saved_creators(brand_id);
create index idx_quick_deals_conversation on quick_deal_offers(conversation_id);
create unique index uk_users_google_subject on users(google_subject) where google_subject is not null;
create index idx_creators_badge_level on creators(badge_level);
create index idx_brand_wallets_brand_id on brand_wallets(brand_id);
create index idx_brand_payment_methods_brand_id on brand_payment_methods(brand_id);
create index idx_brand_payment_methods_default on brand_payment_methods(brand_id, is_default);
create index idx_brand_invoices_brand_id on brand_invoices(brand_id, due_at desc);
create index idx_brand_disbursements_brand_id on brand_disbursements(brand_id, release_date desc);
create index idx_brand_payment_access_lookup on brand_payment_access(user_id, brand_id);
create index idx_payment_audit_logs_brand_created on payment_audit_logs(brand_id, created_at desc);
create index idx_brand_campaigns_brand_status on brand_campaigns(brand_id, status, created_at desc);
create index idx_brand_campaigns_feed on brand_campaigns(status, deadline_date, published_at desc);
create index idx_brand_campaign_reactions_offer on brand_campaign_reactions(campaign_id, created_at desc);
create index idx_brand_campaign_reactions_creator on brand_campaign_reactions(creator_id, updated_at desc);
create index idx_notifications_user_unread on notifications(user_id, is_read, created_at desc);
create index idx_notifications_user_created on notifications(user_id, created_at desc);
create index idx_dispute_cases_status_created on dispute_cases(status, created_at desc);
create index idx_dispute_cases_order on dispute_cases(order_id);
create index idx_admin_audit_logs_created on admin_audit_logs(created_at desc);
create index idx_admin_audit_logs_action on admin_audit_logs(action);
create index idx_payment_refunds_order on payment_refunds(order_id);
create index idx_payment_refunds_created on payment_refunds(created_at desc);
