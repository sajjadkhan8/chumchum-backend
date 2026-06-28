set search_path to core;

-- Pakistan launch seed data with realistic creator/brand profiles and connected business activity.
-- This script is idempotent by using stable UUIDs and ON CONFLICT DO NOTHING.

insert into users (id, username, email, password_hash, role, name, image, avatar_url, city, phone, creator_program_status, is_active)
values
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'ali.rehmani', 'ali.rehmani@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Ali Rehmani', 'https://randomuser.me/api/portraits/men/11.jpg', 'https://randomuser.me/api/portraits/thumb/men/11.jpg', 'Karachi', '+92-300-1122334', 'IN_PATH', true),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'sana.waqar', 'sana.waqar@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Sana Waqar', 'https://randomuser.me/api/portraits/women/12.jpg', 'https://randomuser.me/api/portraits/thumb/women/12.jpg', 'Lahore', '+92-333-4455667', 'IN_PATH', true),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10103', 'hamza.tariq', 'hamza.tariq@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Hamza Tariq', 'https://randomuser.me/api/portraits/men/13.jpg', 'https://randomuser.me/api/portraits/thumb/men/13.jpg', 'Islamabad', '+92-321-7788990', 'IN_PATH', true),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10104', 'zoya.naseem', 'zoya.naseem@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Zoya Naseem', 'https://randomuser.me/api/portraits/women/14.jpg', 'https://randomuser.me/api/portraits/thumb/women/14.jpg', 'Faisalabad', '+92-345-9988776', 'NONE', true),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'usman.shahid', 'usman.shahid@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Usman Shahid', 'https://randomuser.me/api/portraits/men/15.jpg', 'https://randomuser.me/api/portraits/thumb/men/15.jpg', 'Peshawar', '+92-302-5544332', 'NONE', true),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'nishat.pr', 'marketing@nishatstyle.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Nishat Style Team', 'https://images.chumchum.pk/brands/nishat.jpg', 'https://images.chumchum.pk/brands/nishat-avatar.jpg', 'Lahore', '+92-42-111-111-111', 'NONE', true),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', 'jdot.digital', 'campaigns@jdot.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'J. Digital Campaigns', 'https://images.chumchum.pk/brands/jdot.jpg', 'https://images.chumchum.pk/brands/jdot-avatar.jpg', 'Karachi', '+92-21-111-222-333', 'NONE', true),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20103', 'foodpanda.pk', 'influencer@foodpanda.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Foodpanda Pakistan', 'https://images.chumchum.pk/brands/foodpanda.jpg', 'https://images.chumchum.pk/brands/foodpanda-avatar.jpg', 'Karachi', '+92-21-111-111-372', 'NONE', true),
    ('d4c3e0ab-6dd3-4f66-9c01-0e1fd3f90101', 'platform.ops', 'ops@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'PLATFORM_ADMIN', 'ZingZing Platform Ops', null, null, 'Karachi', '+92-300-0000000', 'NONE', true)
on conflict (id) do nothing;

-- Normalize creator profile basics so CreatorResponse always has a username and a renderable image URL.
update users u
set
    username = s.username,
    image = s.image,
    avatar_url = s.avatar_url
from (
    values
        ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10101'::uuid, 'ali.rehmani', 'https://randomuser.me/api/portraits/men/11.jpg', 'https://randomuser.me/api/portraits/thumb/men/11.jpg'),
        ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10102'::uuid, 'sana.waqar', 'https://randomuser.me/api/portraits/women/12.jpg', 'https://randomuser.me/api/portraits/thumb/women/12.jpg'),
        ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10103'::uuid, 'hamza.tariq', 'https://randomuser.me/api/portraits/men/13.jpg', 'https://randomuser.me/api/portraits/thumb/men/13.jpg'),
        ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10104'::uuid, 'zoya.naseem', 'https://randomuser.me/api/portraits/women/14.jpg', 'https://randomuser.me/api/portraits/thumb/women/14.jpg'),
        ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10105'::uuid, 'usman.shahid', 'https://randomuser.me/api/portraits/men/15.jpg', 'https://randomuser.me/api/portraits/thumb/men/15.jpg'),
        ('10000000-0000-4000-8000-000000000001'::uuid, 'hira.ashraf', 'https://randomuser.me/api/portraits/women/21.jpg', 'https://randomuser.me/api/portraits/thumb/women/21.jpg'),
        ('10000000-0000-4000-8000-000000000002'::uuid, 'bilal.naeem', 'https://randomuser.me/api/portraits/men/22.jpg', 'https://randomuser.me/api/portraits/thumb/men/22.jpg'),
        ('10000000-0000-4000-8000-000000000003'::uuid, 'mariam.ikram', 'https://randomuser.me/api/portraits/women/23.jpg', 'https://randomuser.me/api/portraits/thumb/women/23.jpg'),
        ('10000000-0000-4000-8000-000000000004'::uuid, 'ahmed.sheraz', 'https://randomuser.me/api/portraits/men/24.jpg', 'https://randomuser.me/api/portraits/thumb/men/24.jpg'),
        ('10000000-0000-4000-8000-000000000005'::uuid, 'kinza.rana', 'https://randomuser.me/api/portraits/women/25.jpg', 'https://randomuser.me/api/portraits/thumb/women/25.jpg'),
        ('10000000-0000-4000-8000-000000000006'::uuid, 'daniyal.qureshi', 'https://randomuser.me/api/portraits/men/26.jpg', 'https://randomuser.me/api/portraits/thumb/men/26.jpg'),
        ('10000000-0000-4000-8000-000000000007'::uuid, 'ayesha.sami', 'https://randomuser.me/api/portraits/women/27.jpg', 'https://randomuser.me/api/portraits/thumb/women/27.jpg'),
        ('10000000-0000-4000-8000-000000000008'::uuid, 'talha.javed', 'https://randomuser.me/api/portraits/men/28.jpg', 'https://randomuser.me/api/portraits/thumb/men/28.jpg'),
        ('10000000-0000-4000-8000-000000000009'::uuid, 'meesha.khalid', 'https://randomuser.me/api/portraits/women/29.jpg', 'https://randomuser.me/api/portraits/thumb/women/29.jpg'),
        ('10000000-0000-4000-8000-000000000010'::uuid, 'farhan.aslam', 'https://randomuser.me/api/portraits/men/30.jpg', 'https://randomuser.me/api/portraits/thumb/men/30.jpg'),
        ('10000000-0000-4000-8000-000000000011'::uuid, 'iqra.hassan', 'https://randomuser.me/api/portraits/women/31.jpg', 'https://randomuser.me/api/portraits/thumb/women/31.jpg'),
        ('10000000-0000-4000-8000-000000000012'::uuid, 'saad.farooq', 'https://randomuser.me/api/portraits/men/32.jpg', 'https://randomuser.me/api/portraits/thumb/men/32.jpg'),
        ('10000000-0000-4000-8000-000000000013'::uuid, 'nida.kamran', 'https://randomuser.me/api/portraits/women/33.jpg', 'https://randomuser.me/api/portraits/thumb/women/33.jpg'),
        ('10000000-0000-4000-8000-000000000014'::uuid, 'umair.latif', 'https://randomuser.me/api/portraits/men/34.jpg', 'https://randomuser.me/api/portraits/thumb/men/34.jpg'),
        ('10000000-0000-4000-8000-000000000015'::uuid, 'rabia.yousaf', 'https://randomuser.me/api/portraits/women/35.jpg', 'https://randomuser.me/api/portraits/thumb/women/35.jpg'),
        ('10000000-0000-4000-8000-000000000016'::uuid, 'adnan.maqsood', 'https://randomuser.me/api/portraits/men/36.jpg', 'https://randomuser.me/api/portraits/thumb/men/36.jpg'),
        ('10000000-0000-4000-8000-000000000017'::uuid, 'mahnoor.zahid', 'https://randomuser.me/api/portraits/women/37.jpg', 'https://randomuser.me/api/portraits/thumb/women/37.jpg'),
        ('10000000-0000-4000-8000-000000000018'::uuid, 'shahzaib.rauf', 'https://randomuser.me/api/portraits/men/38.jpg', 'https://randomuser.me/api/portraits/thumb/men/38.jpg'),
        ('10000000-0000-4000-8000-000000000019'::uuid, 'areeba.naz', 'https://randomuser.me/api/portraits/women/39.jpg', 'https://randomuser.me/api/portraits/thumb/women/39.jpg'),
        ('10000000-0000-4000-8000-000000000020'::uuid, 'hassan.imran', 'https://randomuser.me/api/portraits/men/40.jpg', 'https://randomuser.me/api/portraits/thumb/men/40.jpg'),
        ('31000000-0000-4000-8000-000000000002'::uuid, 'sara.islamabad', 'https://randomuser.me/api/portraits/women/41.jpg', 'https://randomuser.me/api/portraits/thumb/women/41.jpg')
) as s(id, username, image, avatar_url)
where u.id = s.id;

insert into creators (
    id, username, bio, availability_status, response_time,
    min_price, max_price, followers, avg_views, engagement_rate,
    is_verified, is_trending, is_fast_responder, rating, total_reviews, completed_deals,
    collaboration_preferences, minimum_budget, languages, categories
)
values
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'ali.rehmani', 'Tech reviewer focused on practical smartphone and gadget content for Urdu-speaking audiences.', 'AVAILABLE', 'within_6_hours', 45000, 250000, 785000, 162000, 6.40, true, true, true, 4.80, 132, 286, '["paid", "barter", "hybrid"]'::jsonb, 30000, '["Urdu", "English"]'::jsonb, '["TECH"]'::jsonb),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'sana.waqar', 'Beauty and lifestyle creator with a strong female audience in Lahore and Islamabad.', 'AVAILABLE', 'within_12_hours', 35000, 180000, 612000, 128000, 7.10, true, true, false, 4.70, 94, 211, '["paid", "barter", "hybrid"]'::jsonb, 25000, '["Urdu", "Punjabi", "English"]'::jsonb, '["BEAUTY", "LIFESTYLE"]'::jsonb),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10103', 'hamza.tariq', 'Fitness coach and nutrition creator producing short-form workout plans and diet guides.', 'BUSY', 'within_24_hours', 30000, 140000, 458000, 97000, 5.80, true, false, true, 4.60, 73, 167, '["paid", "barter", "hybrid"]'::jsonb, 20000, '["Urdu", "English"]'::jsonb, '["FITNESS", "HEALTH"]'::jsonb),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10104', 'zoya.naseem', 'Home decor and modest fashion content creator with strong regional reach in Punjab.', 'AVAILABLE', 'within_24_hours', 20000, 90000, 239000, 53000, 4.90, false, false, false, 4.40, 29, 81, '["paid", "barter", "hybrid"]'::jsonb, 15000, '["Urdu", "Punjabi"]'::jsonb, '["HOME_DECOR", "FASHION"]'::jsonb),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'usman.shahid', 'Food vlogger covering street food and family dining spots across KPK and Islamabad.', 'AVAILABLE', 'within_12_hours', 25000, 120000, 371000, 89000, 6.10, false, true, true, 4.50, 41, 104, '["paid", "barter", "hybrid"]'::jsonb, 18000, '["Urdu", "Pashto", "English"]'::jsonb, '["FOOD", "TRAVEL"]'::jsonb)
on conflict (id) do nothing;

insert into brands (id, name, logo_url, website, category, description, monthly_budget)
values
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'Nishat Linen', 'https://images.chumchum.pk/brands/logos/nishat.png', 'https://nishatlinen.com', 'FASHION', 'Leading Pakistani apparel brand focused on seasonal collections and digital-first campaigns.', 4500000),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', 'J.', 'https://images.chumchum.pk/brands/logos/jdot.png', 'https://junaidjamshed.com', 'FASHION', 'National lifestyle brand running creator-led launches for fragrances and festive wear.', 3200000),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20103', 'Foodpanda Pakistan', 'https://images.chumchum.pk/brands/logos/foodpanda.png', 'https://www.foodpanda.pk', 'FOOD', 'Delivery platform focusing on city-specific conversion campaigns and repeat orders.', 6000000)
on conflict (id) do nothing;

insert into packages (
    id, creator_id, name, title, description, platform, category, pricing_type, deal_type,
    price, currency, deliverables, delivery_days, revisions, status, visibility,
    short_description, full_description, is_active, is_featured, is_popular, orders_completed
)
values
    ('c3b5d1f4-88de-4ac2-9101-3f01b8d90101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'YouTube Deep Review', 'Urdu Smartphone Deep-Dive Review', 'Detailed 8-10 minute review with benchmark tests and buying advice tailored for Pakistan market.', 'YOUTUBE', 'TECH', 'PAID', 'PAID', 185000, 'PKR', '["8-10 min YouTube video", "Product links in description", "48-hour pinned comment support"]'::jsonb, 7, 2, 'ACTIVE', 'public', 'Long-form video review with conversion intent', 'Includes product testing, feature breakdown, and value comparison with local alternatives.', true, true, true, 72),
    ('c3b5d1f4-88de-4ac2-9101-3f01b8d90102', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'Instagram Reels Bundle', '2 Tech Reels + 3 Story Frames', 'High-retention reels and swipe-up stories focused on discount and launch messaging.', 'INSTAGRAM', 'TECH', 'PAID', 'PAID', 95000, 'PKR', '["2 Instagram reels", "3 story frames", "Campaign hashtag placement"]'::jsonb, 4, 1, 'ACTIVE', 'public', 'Short-form launch package', 'Optimized for first-week launch traction and limited-time offer CTAs.', true, false, true, 118),
    ('c3b5d1f4-88de-4ac2-9101-3f01b8d90103', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'Beauty Reels Campaign', 'Skincare Routine Reel + Stories', 'Routine-focused content demonstrating practical skincare use in humid weather conditions.', 'INSTAGRAM', 'FASHION', 'PAID', 'PAID', 88000, 'PKR', '["1 reel", "4 story frames", "Product mention in caption"]'::jsonb, 5, 2, 'ACTIVE', 'public', 'High trust skincare storytelling', 'Includes before/after shots and audience Q&A sticker to improve engagement.', true, true, true, 83),
    ('c3b5d1f4-88de-4ac2-9101-3f01b8d90104', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10103', 'Fitness Challenge', '30-Second Workout Challenge Reel', 'Motivational workout challenge designed for mass participation and hashtag growth.', 'INSTAGRAM', 'FITNESS', 'PAID', 'PAID', 76000, 'PKR', '["1 challenge reel", "2 reminder stories", "comment moderation for 24h"]'::jsonb, 4, 1, 'ACTIVE', 'public', 'Challenge format with strong completion rate', 'Built for sportswear and nutrition brands targeting urban youth segments.', true, false, false, 54),
    ('c3b5d1f4-88de-4ac2-9101-3f01b8d90105', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10104', 'Home Styling Reel', 'Living Room Styling Reel + Carousel', 'Budget-conscious decor transformation with product callouts.', 'INSTAGRAM', 'HOME_DECOR', 'PAID', 'PAID', 54000, 'PKR', '["1 reel", "1 carousel post", "product tags"]'::jsonb, 6, 1, 'ACTIVE', 'public', 'Affordable home decor storytelling', 'Targets first-time homeowners and newly married audience cohorts.', true, false, false, 25),
    ('c3b5d1f4-88de-4ac2-9101-3f01b8d90106', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'Food Vlog Feature', 'Restaurant Feature Video + Reels Teaser', 'On-location restaurant coverage with taste notes, pricing, and ordering tips.', 'YOUTUBE', 'FOOD', 'PAID', 'PAID', 102000, 'PKR', '["5-7 min vlog", "1 teaser reel", "location tag + menu highlights"]'::jsonb, 6, 2, 'ACTIVE', 'public', 'City-focused restaurant spotlight', 'Works well for dine-in and delivery brands launching city-level offers.', true, true, true, 61)
on conflict (id) do nothing;


insert into orders (
    id, package_id, creator_id, brand_id, order_number, deal_type, amount, barter_details, message,
    status, progress, delivery_date, deadline_date, created_at, updated_at
)
values
    ('e5a0a01c-3f34-4e61-9901-0d1ce9b30101', 'c3b5d1f4-88de-4ac2-9101-3f01b8d90103', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'ORD-PK-24011', 'PAID', 88000, null, 'Please align with Eid campaign color palette and CTA.', 'COMPLETED', 100, '2026-03-24', '2026-03-25', '2026-03-18 10:15:00+05', '2026-03-24 19:10:00+05'),
    ('e5a0a01c-3f34-4e61-9901-0d1ce9b30102', 'c3b5d1f4-88de-4ac2-9101-3f01b8d90102', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', 'ORD-PK-24019', 'PAID', 95000, null, 'Drive fragrance launch with swipe-up pre-order link.', 'IN_PROGRESS', 70, null, '2026-05-07', '2026-05-02 14:45:00+05', '2026-05-05 11:00:00+05'),
    ('e5a0a01c-3f34-4e61-9901-0d1ce9b30103', 'c3b5d1f4-88de-4ac2-9101-3f01b8d90106', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20103', 'ORD-PK-24023', 'PAID', 102000, null, 'Focus on family meal value and app-exclusive vouchers.', 'COMPLETED', 100, '2026-04-28', '2026-04-29', '2026-04-21 16:30:00+05', '2026-04-28 21:12:00+05')
on conflict (id) do nothing;

insert into reviews (id, package_id, reviewer_id, order_id, creator_id, brand_id, star, rating, description, comment, created_at, updated_at)
values
    ('f6c1b22d-47cd-42db-8801-1acdbce40101', 'c3b5d1f4-88de-4ac2-9101-3f01b8d90103', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'e5a0a01c-3f34-4e61-9901-0d1ce9b30101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 5, 5, 'Strong storytelling and high save-rate performance.', 'The reel stayed on-brand and helped us exceed expected profile visits.', '2026-03-26 12:00:00+05', '2026-03-26 12:00:00+05'),
    ('f6c1b22d-47cd-42db-8801-1acdbce40102', 'c3b5d1f4-88de-4ac2-9101-3f01b8d90106', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20103', 'e5a0a01c-3f34-4e61-9901-0d1ce9b30103', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20103', 4, 4, 'Great on-location energy and clear voucher mention.', 'Delivery was on time; next campaign we may test two shorter cuts.', '2026-04-30 11:20:00+05', '2026-04-30 11:20:00+05')
on conflict (id) do nothing;

insert into conversations (
    id, creator_id, brand_id,
    unread_count_creator, unread_count_brand, last_message, last_message_id, created_at, updated_at
)
values
    ('aa7d7f61-5dd2-4bdb-a501-bd1f4f220101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', 0, 1, 'Looks good, please share final caption by tonight.', 'ab8e68e2-7e1f-47d0-a601-4a0e6f330102', '2026-05-01 09:30:00+05', '2026-05-05 11:00:00+05'),
    ('aa7d7f61-5dd2-4bdb-a501-bd1f4f220102', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20103', 0, 0, 'Voucher mention added in opening 20 seconds.', 'ab8e68e2-7e1f-47d0-a601-4a0e6f330104', '2026-04-21 16:35:00+05', '2026-04-28 20:50:00+05')
on conflict (id) do nothing;

insert into messages (
    id, conversation_id, sender_id, content, description, type, is_read, attachment_url, sender_type,
    offer_deal_type, offer_amount, offer_barter_details, offer_barter_category, offer_estimated_barter_value,
    offer_creator_expectation, offer_message, offer_status, created_at, updated_at
)
values
    ('ab8e68e2-7e1f-47d0-a601-4a0e6f330101', 'aa7d7f61-5dd2-4bdb-a501-bd1f4f220101', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', 'Can we lock this package for Friday launch?', null, 'TEXT', true, null, 'BRAND', null, null, null, null, null, null, null, null, '2026-05-01 09:32:00+05', '2026-05-01 09:32:00+05'),
    ('ab8e68e2-7e1f-47d0-a601-4a0e6f330102', 'aa7d7f61-5dd2-4bdb-a501-bd1f4f220101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'Looks good, please share final caption by tonight.', null, 'TEXT', false, null, 'CREATOR', null, null, null, null, null, null, null, null, '2026-05-05 11:00:00+05', '2026-05-05 11:00:00+05'),
    ('ab8e68e2-7e1f-47d0-a601-4a0e6f330103', 'aa7d7f61-5dd2-4bdb-a501-bd1f4f220102', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20103', 'Need stronger mention of app voucher in first 15 seconds.', null, 'TEXT', true, null, 'BRAND', null, null, null, null, null, null, null, null, '2026-04-27 18:40:00+05', '2026-04-27 18:40:00+05'),
    ('ab8e68e2-7e1f-47d0-a601-4a0e6f330104', 'aa7d7f61-5dd2-4bdb-a501-bd1f4f220102', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'Voucher mention added in opening 20 seconds.', null, 'TEXT', true, null, 'CREATOR', null, null, null, null, null, null, null, null, '2026-04-28 20:50:00+05', '2026-04-28 20:50:00+05')
on conflict (id) do nothing;

insert into quick_deal_offers (
    id, message_id, conversation_id, deal_type, amount, barter_details, barter_category,
    estimated_barter_value, creator_expectation, message, status, created_at, updated_at
)
values
    ('be1fd4e3-8f90-4d8b-b701-72f94a550101', 'ab8e68e2-7e1f-47d0-a601-4a0e6f330101', 'aa7d7f61-5dd2-4bdb-a501-bd1f4f220101', 'PAID', 95000, null, null, null, 'Caption approval within 24h', 'Fast-track booking for Friday launch slot.', 'ACCEPTED', '2026-05-01 09:33:00+05', '2026-05-01 12:05:00+05')
on conflict (id) do nothing;

insert into deliverables (id, order_id, name, status, file_url, submitted_at, created_at, updated_at)
values
    ('c80d99fc-c77e-43e3-a901-a58fb6700101', 'e5a0a01c-3f34-4e61-9901-0d1ce9b30101', 'Final Reel Export', 'APPROVED', 'https://deliverables.chumchum.pk/orders/ORD-PK-24011/final-reel.mp4', '2026-03-24 17:35:00+05', '2026-03-20 11:00:00+05', '2026-03-24 19:10:00+05'),
    ('c80d99fc-c77e-43e3-a901-a58fb6700102', 'e5a0a01c-3f34-4e61-9901-0d1ce9b30103', 'Restaurant Vlog Final Cut', 'APPROVED', 'https://deliverables.chumchum.pk/orders/ORD-PK-24023/vlog-final.mp4', '2026-04-28 19:05:00+05', '2026-04-23 12:20:00+05', '2026-04-28 21:12:00+05')
on conflict (id) do nothing;

insert into social_accounts (id, creator_id, platform, username, profile_url, followers, avg_views, engagement_rate, is_verified)
values
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'youtube',    'AliRehmaniTech',     'https://youtube.com/@AliRehmaniTech',     421000, 176000, 5.90, true),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00102', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'instagram',  'ali.rehmani',        'https://instagram.com/ali.rehmani',       242000, 95000,  6.70, true),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00103', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'instagram',  'sana.waqar',         'https://instagram.com/sana.waqar',        389000, 121000, 7.40, true),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00104', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'tiktok',     'usman.shahid',       'https://tiktok.com/@usman.shahid',        214000, 87000,  6.30, false),
    -- ali.rehmani: tiktok + facebook
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00105', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'tiktok',     'alirehmani',         'https://www.tiktok.com/@alirehmani',      0, 0, 0, false),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00106', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'facebook',   'alirehmanitech',     'https://www.facebook.com/alirehmanitech', 0, 0, 0, false),
    -- sana.waqar: tiktok + youtube + facebook
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00107', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'tiktok',     'sanawaqar.official', 'https://www.tiktok.com/@sanawaqar.official', 0, 0, 0, false),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00108', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'youtube',    'SanaWaqarOfficial',  'https://www.youtube.com/@SanaWaqarOfficial', 0, 0, 0, false),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00109', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'facebook',   'sanawaqarofficial',  'https://www.facebook.com/sanawaqarofficial', 0, 0, 0, false),
    -- hamza.tariq: all 4
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00110', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10103', 'tiktok',     'hamzatariq.fit',     'https://www.tiktok.com/@hamzatariq.fit',     0, 0, 0, false),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00111', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10103', 'instagram',  'hamza.tariq.fit',    'https://www.instagram.com/hamza.tariq.fit',  0, 0, 0, false),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00112', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10103', 'youtube',    'HamzaTariqFit',      'https://www.youtube.com/@HamzaTariqFit',     0, 0, 0, false),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00113', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10103', 'facebook',   'hamzatariqfit',      'https://www.facebook.com/hamzatariqfit',     0, 0, 0, false),
    -- zoya.naseem: all 4
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00114', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10104', 'tiktok',     'zoya.naseem.home',   'https://www.tiktok.com/@zoya.naseem.home',   0, 0, 0, false),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00115', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10104', 'instagram',  'zoya.naseem',        'https://www.instagram.com/zoya.naseem',      0, 0, 0, false),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00116', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10104', 'youtube',    'ZoyaNaseemHome',     'https://www.youtube.com/@ZoyaNaseemHome',    0, 0, 0, false),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00117', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10104', 'facebook',   'zoyanaseemhome',     'https://www.facebook.com/zoyanaseemhome',    0, 0, 0, false),
    -- usman.shahid: instagram + youtube + facebook
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00118', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'instagram',  'usman.shahid.food',  'https://www.instagram.com/usman.shahid.food', 0, 0, 0, false),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00119', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'youtube',    'UsmanShahidFood',    'https://www.youtube.com/@UsmanShahidFood',    0, 0, 0, false),
    ('d2b056d1-4890-4bb0-b801-6f0bb9a00120', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'facebook',   'usmanshahidfood',    'https://www.facebook.com/usmanshahidfood',    0, 0, 0, false)
on conflict (id) do nothing;

insert into content_previews (id, creator_id, type, thumbnail_url, media_url, platform, views, likes)
values
    ('e779e9aa-5e34-49be-b901-436f5b430101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'VIDEO', 'https://images.chumchum.pk/previews/ali-tech-thumb.jpg', 'https://media.chumchum.pk/previews/ali-tech.mp4', 'YOUTUBE', 214000, 17800),
    ('e779e9aa-5e34-49be-b901-436f5b430102', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'REEL', 'https://images.chumchum.pk/previews/sana-skin-thumb.jpg', 'https://media.chumchum.pk/previews/sana-skin.mp4', 'INSTAGRAM', 164000, 12600),
    ('e779e9aa-5e34-49be-b901-436f5b430103', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'VIDEO', 'https://images.chumchum.pk/previews/usman-food-thumb.jpg', 'https://media.chumchum.pk/previews/usman-food.mp4', 'YOUTUBE', 132000, 9800)
on conflict (id) do nothing;

insert into package_analytics (
    package_id, views, clicks, inquiries, conversion_rate, completion_rate,
    repeat_brands, engagement_performance, updated_at
)
values
    ('c3b5d1f4-88de-4ac2-9101-3f01b8d90101', 98300, 14210, 411, 2.89, 94.20, 8, 91.40, '2026-05-10 09:00:00+05'),
    ('c3b5d1f4-88de-4ac2-9101-3f01b8d90102', 124500, 18430, 539, 2.92, 96.10, 11, 93.80, '2026-05-10 09:00:00+05'),
    ('c3b5d1f4-88de-4ac2-9101-3f01b8d90103', 88700, 13220, 390, 2.95, 95.40, 7, 90.20, '2026-05-10 09:00:00+05'),
    ('c3b5d1f4-88de-4ac2-9101-3f01b8d90106', 76500, 12040, 318, 2.64, 92.80, 6, 88.70, '2026-05-10 09:00:00+05')
on conflict (package_id) do nothing;

insert into wallets (creator_id, total_earned, available_balance, pending_balance, updated_at)
values
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 6920000, 840000, 120000, '2026-05-10 09:00:00+05'),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 5110000, 620000, 95000, '2026-05-10 09:00:00+05'),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10103', 3680000, 410000, 68000, '2026-05-10 09:00:00+05'),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10104', 1420000, 190000, 42000, '2026-05-10 09:00:00+05'),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 2960000, 355000, 76000, '2026-05-10 09:00:00+05')
on conflict (creator_id) do nothing;

insert into creator_payout_preferences (
    creator_id, auto_withdraw_enabled, payout_schedule, minimum_payout_amount,
    account_holder_name, ntn_number, cnic_last4, updated_at
)
values
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', true, 'WEEKLY', 10000, 'Ali Rehmani', '3491827-5', '1122', '2026-05-10 09:00:00+05'),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', false, 'MANUAL', 5000, 'Sana Waqar', '', '6677', '2026-05-10 09:00:00+05')
on conflict (creator_id) do nothing;

insert into brand_wallets (
    brand_id, wallet_balance, monthly_spend, pending_escrow, processing_payouts, next_invoice_date, updated_at
)
values
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 840000, 1265000, 475000, 135000, '2026-06-10 00:00:00+05', '2026-06-01 09:00:00+05'),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', 620000, 980000, 320000, 95000, '2026-06-12 00:00:00+05', '2026-06-01 09:00:00+05')
on conflict (brand_id) do nothing;

insert into brand_payment_methods (
    id, brand_id, type, label, account_mask, holder_name, is_default, status, created_at, updated_at
)
values
    ('d9100000-0000-4000-8000-000000000001', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'CARD', 'Visa Corporate Card', '**** **** **** 4242', 'Nishat Linen', true, 'ACTIVE', '2026-04-01 09:00:00+05', '2026-04-01 09:00:00+05'),
    ('d9100000-0000-4000-8000-000000000002', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'BANK_TRANSFER', 'HBL Operating Account', 'PK36HABB0001123456789911', 'Nishat Linen', false, 'ACTIVE', '2026-04-01 09:15:00+05', '2026-04-01 09:15:00+05')
on conflict (id) do nothing;

insert into brand_invoices (id, brand_id, period_label, amount, status, issued_at, due_at, created_at)
values
    ('e9200000-0000-4000-8000-000000000001', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'May 2026', 1420000, 'DUE', '2026-06-01 00:00:00+05', '2026-06-10 00:00:00+05', '2026-06-01 00:00:00+05'),
    ('e9200000-0000-4000-8000-000000000002', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'April 2026', 1190000, 'PAID', '2026-05-01 00:00:00+05', '2026-05-10 00:00:00+05', '2026-05-01 00:00:00+05')
on conflict (id) do nothing;

insert into brand_disbursements (
    id, brand_id, creator_id, order_id, campaign_name, amount, status, release_date, created_at, updated_at
)
values
    ('f9300000-0000-4000-8000-000000000001', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', null, 'Summer Launch UGC', 95000, 'PROCESSING', '2026-06-06 11:00:00+05', '2026-06-05 16:00:00+05', '2026-06-06 11:00:00+05'),
    ('f9300000-0000-4000-8000-000000000002', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', null, 'TikTok Creator Sprint', 65000, 'SCHEDULED', '2026-06-08 12:00:00+05', '2026-06-06 09:00:00+05', '2026-06-06 09:00:00+05')
on conflict (id) do nothing;

insert into brand_payout_controls (
    brand_id, require_two_approvals, auto_release_after_days, low_balance_alert_threshold, updated_at
)
values
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', true, 5, 350000, '2026-06-01 09:00:00+05'),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', true, 7, 250000, '2026-06-01 09:00:00+05')
on conflict (brand_id) do nothing;

insert into users (id, username, email, password_hash, role, name, city, phone, creator_program_status, is_active)
values
    ('b7e2aa31-47f1-4c8d-b201-1a8cb4f20111', 'nishat.finance', 'finance@nishatstyle.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Nishat Finance Manager', 'Lahore', '+92-42-55550001', 'NONE', true),
    ('b7e2aa31-47f1-4c8d-b201-1a8cb4f20112', 'nishat.ops', 'ops@nishatstyle.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Nishat Campaign Admin', 'Lahore', '+92-42-55550002', 'NONE', true),
    ('b7e2aa31-47f1-4c8d-b201-1a8cb4f20113', 'jdot.finance', 'finance@jdot.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'J. Finance Manager', 'Karachi', '+92-21-55550003', 'NONE', true)
on conflict (id) do nothing;

insert into brand_payment_access (brand_id, user_id, role)
values
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'OWNER'),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'b7e2aa31-47f1-4c8d-b201-1a8cb4f20111', 'FINANCE'),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'b7e2aa31-47f1-4c8d-b201-1a8cb4f20112', 'ADMIN'),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', 'OWNER'),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', 'b7e2aa31-47f1-4c8d-b201-1a8cb4f20113', 'FINANCE'),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20103', 'b7e2aa31-47f1-4c8d-b101-1a8cb4f20103', 'OWNER')
on conflict (brand_id, user_id) do nothing;

insert into transactions (id, creator_id, order_id, type, amount, description, status, created_at)
values
    ('f91d8d67-54af-4e9a-8d01-2ecfcf8b0101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'e5a0a01c-3f34-4e61-9901-0d1ce9b30101', 'ORDER_PAYMENT', 88000, 'Order ORD-PK-24011 payout credit', 'COMPLETED', '2026-03-25 10:30:00+05'),
    ('f91d8d67-54af-4e9a-8d01-2ecfcf8b0102', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', 'e5a0a01c-3f34-4e61-9901-0d1ce9b30103', 'ORDER_PAYMENT', 102000, 'Order ORD-PK-24023 payout credit', 'COMPLETED', '2026-04-29 09:45:00+05')
on conflict (id) do nothing;

insert into payout_methods (id, creator_id, type, name, account_details, is_default, created_at)
values
    ('a2fc3e5b-25e1-4dca-b901-59f33b530101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'BANK_TRANSFER', 'Meezan Bank - Main', 'IBAN: PK36MEZN0001200001234567', true, '2026-01-05 12:00:00+05'),
    ('a2fc3e5b-25e1-4dca-b901-59f33b530102', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'BANK_TRANSFER', 'HBL Lahore', 'IBAN: PK94HABB0005400009876543', true, '2026-01-08 15:20:00+05')
on conflict (id) do nothing;

insert into withdrawal_requests (id, creator_id, payout_method_id, amount, status, processed_at, created_at)
values
    ('b3ac2f4c-64d1-4f8d-a201-5be44d640101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 'a2fc3e5b-25e1-4dca-b901-59f33b530102', 150000, 'approved', '2026-05-04 16:40:00+05', '2026-05-03 09:10:00+05')
on conflict (id) do nothing;

insert into ambassador_applications (
    id, creator_id, status, submitted_at, identity_verified, engagement_verified,
    content_review_passed, background_check_passed, notes, approved_at, reviewed_by, created_at, updated_at
)
values
    ('c4de6a73-9e4a-42b2-b301-8cb55e750101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 'approved', '2026-02-12 11:00:00+05', true, true, true, true, 'Consistent performance and high brief compliance across premium campaigns.', '2026-02-20 17:30:00+05', 'd4c3e0ab-6dd3-4f66-9c01-0e1fd3f90101', '2026-02-10 14:00:00+05', '2026-02-20 17:30:00+05')
on conflict (id) do nothing;

insert into ambassador_scores (
    creator_id, total, delivery_score, account_age_score, rating_score, cancellation_score,
    profile_completeness_score, consistency_score, tier, percentile_rank, strengths, improvements, calculated_at
)
values
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', 92, 19, 15, 19, 14, 12, 13, 'elite_creator', 96, '["delivery reliability", "campaign communication", "audience trust"]'::jsonb, '["expand barter offer templates"]'::jsonb, '2026-05-10 08:30:00+05'),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', 88, 18, 14, 18, 14, 12, 12, 'top_creator', 90, '["engagement quality", "creative direction"]'::jsonb, '["reduce first draft turnaround variance"]'::jsonb, '2026-05-10 08:30:00+05')
on conflict (creator_id) do nothing;

insert into saved_creators (brand_id, creator_id, saved_at)
values
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', '2026-03-12 10:00:00+05'),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', '2026-04-29 18:00:00+05'),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20103', 'a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', '2026-04-19 16:25:00+05')
on conflict (brand_id, creator_id) do nothing;

insert into notification_preferences (
    user_id, new_orders, messages, reviews, marketing,
    weekly_digest, push_notifications, email_notifications, sms_notifications
)
values
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10101', true, true, true, false, true, true, true, false),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10102', true, true, true, false, true, true, true, false),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10103', true, true, true, false, true, true, true, false),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10104', true, true, false, false, true, true, true, false),
    ('a1f8db5c-6a4b-4d76-a001-0fd5f9c10105', true, true, true, false, true, true, true, false),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20101', false, true, true, true, true, true, true, false),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20102', false, true, true, true, true, true, true, false),
    ('b7e2aa31-47f1-4c8d-b101-1a8cb4f20103', false, true, true, true, true, true, true, false),
    ('d4c3e0ab-6dd3-4f66-9c01-0e1fd3f90101', true, true, true, false, true, true, true, false)
on conflict (user_id) do nothing;


-- FK bootstrap for expansion section: referenced creators/brands must exist before package inserts.
insert into users (id, username, role, name)
select s.id, s.username, s.role, s.name
from (
    values
        ('10000000-0000-4000-8000-000000000001'::uuid, 'hira.ashraf', 'CREATOR', 'Hira Ashraf'),
        ('10000000-0000-4000-8000-000000000002'::uuid, 'bilal.naeem', 'CREATOR', 'Bilal Naeem'),
        ('10000000-0000-4000-8000-000000000003'::uuid, 'mariam.ikram', 'CREATOR', 'Mariam Ikram'),
        ('10000000-0000-4000-8000-000000000004'::uuid, 'ahmed.sheraz', 'CREATOR', 'Ahmed Sheraz'),
        ('10000000-0000-4000-8000-000000000005'::uuid, 'kinza.rana', 'CREATOR', 'Kinza Rana'),
        ('10000000-0000-4000-8000-000000000006'::uuid, 'daniyal.qureshi', 'CREATOR', 'Daniyal Qureshi'),
        ('10000000-0000-4000-8000-000000000007'::uuid, 'ayesha.sami', 'CREATOR', 'Ayesha Sami'),
        ('10000000-0000-4000-8000-000000000008'::uuid, 'talha.javed', 'CREATOR', 'Talha Javed'),
        ('10000000-0000-4000-8000-000000000009'::uuid, 'meesha.khalid', 'CREATOR', 'Meesha Khalid'),
        ('10000000-0000-4000-8000-000000000010'::uuid, 'farhan.aslam', 'CREATOR', 'Farhan Aslam'),
        ('10000000-0000-4000-8000-000000000011'::uuid, 'iqra.hassan', 'CREATOR', 'Iqra Hassan'),
        ('10000000-0000-4000-8000-000000000012'::uuid, 'saad.farooq', 'CREATOR', 'Saad Farooq'),
        ('10000000-0000-4000-8000-000000000013'::uuid, 'nida.kamran', 'CREATOR', 'Nida Kamran'),
        ('10000000-0000-4000-8000-000000000014'::uuid, 'umair.latif', 'CREATOR', 'Umair Latif'),
        ('10000000-0000-4000-8000-000000000015'::uuid, 'rabia.yousaf', 'CREATOR', 'Rabia Yousaf'),
        ('10000000-0000-4000-8000-000000000016'::uuid, 'adnan.maqsood', 'CREATOR', 'Adnan Maqsood'),
        ('10000000-0000-4000-8000-000000000017'::uuid, 'mahnoor.zahid', 'CREATOR', 'Mahnoor Zahid'),
        ('10000000-0000-4000-8000-000000000018'::uuid, 'shahzaib.rauf', 'CREATOR', 'Shahzaib Rauf'),
        ('10000000-0000-4000-8000-000000000019'::uuid, 'areeba.naz', 'CREATOR', 'Areeba Naz'),
        ('10000000-0000-4000-8000-000000000020'::uuid, 'hassan.imran', 'CREATOR', 'Hassan Imran'),
        ('20000000-0000-4000-8000-000000000001'::uuid, 'khaadi.pr', 'BRAND', 'Khaadi PR Team'),
        ('20000000-0000-4000-8000-000000000002'::uuid, 'gulahmed.media', 'BRAND', 'Gul Ahmed Media'),
        ('20000000-0000-4000-8000-000000000003'::uuid, 'servis.shoes', 'BRAND', 'Servis Shoes Digital'),
        ('20000000-0000-4000-8000-000000000004'::uuid, 'daraz.partnerships', 'BRAND', 'Daraz Partnerships'),
        ('20000000-0000-4000-8000-000000000005'::uuid, 'pakwheels.brands', 'BRAND', 'PakWheels Brand Studio'),
        ('20000000-0000-4000-8000-000000000006'::uuid, 'jubilee.life', 'BRAND', 'Jubilee Life Digital'),
        ('20000000-0000-4000-8000-000000000007'::uuid, 'nestle.pk', 'BRAND', 'Nestle Pakistan'),
        ('20000000-0000-4000-8000-000000000008'::uuid, 'tapal.tea', 'BRAND', 'Tapal Tea Digital'),
        ('20000000-0000-4000-8000-000000000009'::uuid, 'bankalfalah.digital', 'BRAND', 'Bank Alfalah Digital'),
        ('20000000-0000-4000-8000-000000000010'::uuid, 'careem.food', 'BRAND', 'Careem Food Marketing')
) as s(id, username, role, name)
on conflict (id) do nothing;


insert into brands (id, name)
select s.id, s.name
from (
    values
        ('20000000-0000-4000-8000-000000000001'::uuid, 'Khaadi'),
        ('20000000-0000-4000-8000-000000000002'::uuid, 'Gul Ahmed'),
        ('20000000-0000-4000-8000-000000000003'::uuid, 'Servis'),
        ('20000000-0000-4000-8000-000000000004'::uuid, 'Daraz Pakistan'),
        ('20000000-0000-4000-8000-000000000005'::uuid, 'PakWheels'),
        ('20000000-0000-4000-8000-000000000006'::uuid, 'Jubilee Life'),
        ('20000000-0000-4000-8000-000000000007'::uuid, 'Nestle Pakistan'),
        ('20000000-0000-4000-8000-000000000008'::uuid, 'Tapal Tea'),
        ('20000000-0000-4000-8000-000000000009'::uuid, 'Bank Alfalah'),
        ('20000000-0000-4000-8000-000000000010'::uuid, 'Careem Food')
) as s(id, name)
on conflict (id) do nothing;

insert into creators (id)
select s.id
from (
    values
        ('10000000-0000-4000-8000-000000000001'::uuid),
        ('10000000-0000-4000-8000-000000000002'::uuid),
        ('10000000-0000-4000-8000-000000000003'::uuid),
        ('10000000-0000-4000-8000-000000000004'::uuid),
        ('10000000-0000-4000-8000-000000000005'::uuid),
        ('10000000-0000-4000-8000-000000000006'::uuid),
        ('10000000-0000-4000-8000-000000000007'::uuid),
        ('10000000-0000-4000-8000-000000000008'::uuid),
        ('10000000-0000-4000-8000-000000000009'::uuid),
        ('10000000-0000-4000-8000-000000000010'::uuid),
        ('10000000-0000-4000-8000-000000000011'::uuid),
        ('10000000-0000-4000-8000-000000000012'::uuid),
        ('10000000-0000-4000-8000-000000000013'::uuid),
        ('10000000-0000-4000-8000-000000000014'::uuid),
        ('10000000-0000-4000-8000-000000000015'::uuid),
        ('10000000-0000-4000-8000-000000000016'::uuid),
        ('10000000-0000-4000-8000-000000000017'::uuid),
        ('10000000-0000-4000-8000-000000000018'::uuid),
        ('10000000-0000-4000-8000-000000000019'::uuid),
        ('10000000-0000-4000-8000-000000000020'::uuid)
) as s(id)
on conflict (id) do nothing;


-- Expansion activity: additional packages, orders, reviews, and analytics.
insert into packages (
    id, creator_id, name, title, description, platform, category, pricing_type, deal_type,
    price, currency, deliverables, delivery_days, revisions, status, visibility,
    short_description, full_description, is_active, is_featured, is_popular, orders_completed
)
values
    ('30000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Festive Styling Reels', '2 Festive Outfit Reels + Stories', 'Festive and wedding-season outfit storytelling with conversion intent.', 'INSTAGRAM', 'FASHION', 'PAID', 'PAID', 92000, 'PKR', '["2 reels", "4 stories", "product tags"]'::jsonb, 5, 2, 'ACTIVE', 'public', 'Festive fashion conversion package', 'Designed for Eid and wedding campaign launches with strong CTR framing.', true, true, true, 68),
    ('30000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000002', 'Tech Launch Kit', 'YouTube Review + Reels Teaser', 'Launch-week full-funnel package for electronics and marketplace campaigns.', 'YOUTUBE', 'TECH', 'PAID', 'PAID', 165000, 'PKR', '["1 review video", "1 teaser reel", "comment support"]'::jsonb, 7, 2, 'ACTIVE', 'public', 'Launch week tech package', 'Combines long-form trust and short-form awareness for product launches.', true, true, true, 74),
    ('30000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000003', 'Family Routine Storytelling', 'Parenting Reel + Story Sequence', 'Routine-focused creative for household and parenting products.', 'INSTAGRAM', 'PARENTING_FAMILY', 'PAID', 'PAID', 61000, 'PKR', '["1 reel", "5 stories"]'::jsonb, 6, 2, 'ACTIVE', 'public', 'High-trust parenting audience', 'Narrative style content designed for family decision-makers.', true, false, false, 39),
    ('30000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000004', 'Car Ownership Explainer', 'Vehicle Review Reel + Ownership Tips', 'Automotive reel with ownership-cost and resale-value framing.', 'INSTAGRAM', 'AUTOMOTIVE', 'PAID', 'PAID', 84000, 'PKR', '["1 reel", "2 stories"]'::jsonb, 5, 1, 'ACTIVE', 'public', 'Auto trust-building package', 'Built for dealership, lubricant, and insurance campaign objectives.', true, false, false, 42),
    ('30000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000005', 'Skincare Conversion Reel', 'Routine Reel + Q&A Stories', 'Skincare routine content aligned with humid-weather concerns.', 'INSTAGRAM', 'FASHION', 'PAID', 'PAID', 79000, 'PKR', '["1 reel", "3 stories", "Q&A"]'::jsonb, 5, 2, 'ACTIVE', 'public', 'Beauty conversion focused package', 'Includes engagement hook and comment-response support.', true, true, true, 57),
    ('30000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000006', 'Food Spot Feature', 'Restaurant Feature Reel', 'Restaurant feature with menu and price sensitivity messaging.', 'INSTAGRAM', 'FOOD', 'PAID', 'PAID', 67000, 'PKR', '["1 reel", "offer mention", "location tag"]'::jsonb, 4, 1, 'ACTIVE', 'public', 'Restaurant growth unit', 'Best for app-delivery and local dine-in conversion pushes.', true, false, true, 49),
    ('30000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000007', 'Student Prep Series', 'Exam Productivity Reel + Notes Carousel', 'Student prep campaign with practical planning templates.', 'INSTAGRAM', 'EDUCATION', 'PAID', 'PAID', 52000, 'PKR', '["1 reel", "1 carousel post"]'::jsonb, 6, 1, 'ACTIVE', 'public', 'Student acquisition package', 'Useful for edtech trials and study-tool promotions.', true, false, false, 31),
    ('30000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000008', 'Northern Route Vlog', 'Travel Vlog + Itinerary Stories', 'Domestic travel itinerary content with budget visibility.', 'YOUTUBE', 'TRAVEL', 'PAID', 'PAID', 112000, 'PKR', '["1 vlog", "3 stories"]'::jsonb, 8, 2, 'ACTIVE', 'public', 'Domestic tourism storytelling', 'Strong for hospitality, transport and family-travel campaigns.', true, false, false, 36),
    ('30000000-0000-4000-8000-000000000009', '10000000-0000-4000-8000-000000000009', 'Home Organizing Reel', 'Pantry and Kitchen Organization Reel', 'Before-after format with practical home utility angles.', 'INSTAGRAM', 'HOME_DECOR', 'PAID', 'PAID', 56000, 'PKR', '["1 reel", "2 stories"]'::jsonb, 5, 1, 'ACTIVE', 'public', 'Home utility content package', 'Designed for household and grocery category launches.', true, false, false, 27),
    ('30000000-0000-4000-8000-000000000010', '10000000-0000-4000-8000-000000000010', 'Matchday Breakdown', 'Cricket Analysis Reel + Poll Stories', 'Matchday short-form campaign with prediction engagement loops.', 'INSTAGRAM', 'SPORTS', 'PAID', 'PAID', 86000, 'PKR', '["1 reel", "3 poll stories"]'::jsonb, 4, 1, 'ACTIVE', 'public', 'Cricket audience engagement', 'Strong match-cycle engagement for beverage and telecom brands.', true, true, true, 52),
    ('30000000-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000011', 'Daily Lifestyle Kit', 'Homecare Reel + Weekly Stories', 'Daily routine narrative for homecare and FMCG promotions.', 'INSTAGRAM', 'TRAVEL', 'PAID', 'PAID', 59000, 'PKR', '["1 reel", "5 stories"]'::jsonb, 6, 1, 'ACTIVE', 'public', 'Lifestyle consistency package', 'Fits recurring visibility campaigns and value messaging.', true, false, false, 33),
    ('30000000-0000-4000-8000-000000000012', '10000000-0000-4000-8000-000000000012', 'Finance Explainer', 'Credit Card & Savings Explainer', 'Trust-first explainer around digital banking and savings behavior.', 'YOUTUBE', 'BUSINESS_FINANCE', 'PAID', 'PAID', 138000, 'PKR', '["1 explainer video", "1 short cutdown"]'::jsonb, 7, 2, 'ACTIVE', 'public', 'Finance trust campaign package', 'Optimized for financial literacy and product adoption narratives.', true, true, false, 41),
    ('30000000-0000-4000-8000-000000000013', '10000000-0000-4000-8000-000000000013', 'Nutrition Habit Reel', 'Meal Plan Reel + Grocery Tips', 'Habit-focused wellness storytelling for family nutrition.', 'INSTAGRAM', 'FITNESS', 'PAID', 'PAID', 62000, 'PKR', '["1 reel", "3 tips stories"]'::jsonb, 5, 2, 'ACTIVE', 'public', 'Wellness routine package', 'Clear CTAs with practical daily implementation context.', true, false, false, 30),
    ('30000000-0000-4000-8000-000000000014', '10000000-0000-4000-8000-000000000014', 'Gaming Launch Push', 'Livestream Highlight Reel + CTA Stories', 'High-energy gaming creative with launch and discount callouts.', 'INSTAGRAM', 'GAMING', 'PAID', 'PAID', 98000, 'PKR', '["1 highlight reel", "4 stories"]'::jsonb, 4, 1, 'ACTIVE', 'public', 'Gaming launch acceleration', 'Ideal for high-frequency promo windows and midnight drops.', true, true, true, 58),
    ('30000000-0000-4000-8000-000000000015', '10000000-0000-4000-8000-000000000015', 'Weekend Recipe Reel', 'Traditional Recipe Reel + Ingredient List', 'Recipe-led format optimized for saves and shares.', 'INSTAGRAM', 'FOOD', 'PAID', 'PAID', 64000, 'PKR', '["1 reel", "ingredient list story"]'::jsonb, 5, 1, 'ACTIVE', 'public', 'Food recipe conversion package', 'Built for FMCG and grocery category conversion flows.', true, false, true, 46),
    ('30000000-0000-4000-8000-000000000016', '10000000-0000-4000-8000-000000000016', 'Agri Tips Series', 'Field Productivity Reel + Checklist', 'Practical agri workflow reel with educational positioning.', 'INSTAGRAM', 'GENERAL', 'PAID', 'PAID', 70000, 'PKR', '["1 reel", "1 checklist post"]'::jsonb, 6, 1, 'ACTIVE', 'public', 'Rural outreach package', 'Useful for B2B-lite rural awareness campaigns.', true, false, false, 22),
    ('30000000-0000-4000-8000-000000000017', '10000000-0000-4000-8000-000000000017', 'Book Club Reel', 'Book Review Reel + Study Hacks', 'Reading culture and productivity hybrid content.', 'INSTAGRAM', 'EDUCATION', 'PAID', 'PAID', 50000, 'PKR', '["1 reel", "2 story cards"]'::jsonb, 5, 1, 'ACTIVE', 'public', 'Education-adjacent package', 'Works for publishing, stationery and student-centric offers.', true, false, false, 25),
    ('30000000-0000-4000-8000-000000000018', '10000000-0000-4000-8000-000000000018', 'Bike Safety Feature', 'Motorbike Safety Reel + Gear Breakdown', 'Safety-first creator unit for mobility brands.', 'INSTAGRAM', 'AUTOMOTIVE', 'PAID', 'PAID', 61000, 'PKR', '["1 reel", "2 safety stories"]'::jsonb, 5, 1, 'ACTIVE', 'public', 'Bike safety campaign package', 'Combines utility education with brand trust messaging.', true, false, false, 29),
    ('30000000-0000-4000-8000-000000000019', '10000000-0000-4000-8000-000000000019', 'Bridal Drop Campaign', 'Festive Collection Reel + Styling Stories', 'High-intent festive fashion storytelling for purchase windows.', 'INSTAGRAM', 'FASHION', 'PAID', 'PAID', 121000, 'PKR', '["1 reel", "4 styling stories"]'::jsonb, 5, 2, 'ACTIVE', 'public', 'Bridal/festive demand package', 'Optimized for limited-period drops and collection launches.', true, true, true, 61),
    ('30000000-0000-4000-8000-000000000020', '10000000-0000-4000-8000-000000000020', 'Comedy Ad Integration', 'Brand Integration Comedy Sketch', 'Native ad integration with brand-safe humor and CTA.', 'INSTAGRAM', 'ENTERTAINMENT', 'PAID', 'PAID', 110000, 'PKR', '["1 sketch reel", "2 stories"]'::jsonb, 4, 1, 'ACTIVE', 'public', 'Mass reach entertainment package', 'High-completion format with broad demographic coverage.', true, true, true, 73)
on conflict (id) do nothing;

insert into orders (
    id, package_id, creator_id, brand_id, order_number, deal_type, amount, message,
    status, progress, delivery_date, deadline_date, created_at, updated_at
)
values
    ('40000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'ORD-PK-24031', 'PAID', 92000, 'Focus on Eid edit style and stitched fabric closeups.', 'COMPLETED', 100, '2026-04-11', '2026-04-12', '2026-04-04 11:20:00+05', '2026-04-11 19:00:00+05'),
    ('40000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000004', 'ORD-PK-24032', 'PAID', 165000, 'Include pricing context and warranty mention.', 'COMPLETED', 100, '2026-04-20', '2026-04-21', '2026-04-12 13:00:00+05', '2026-04-20 18:10:00+05'),
    ('40000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000007', 'ORD-PK-24033', 'PAID', 79000, 'Highlight hydrating range for summer skin.', 'IN_PROGRESS', 60, null, '2026-05-18', '2026-05-13 10:05:00+05', '2026-05-16 14:30:00+05'),
    ('40000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000006', '20000000-0000-4000-8000-000000000010', 'ORD-PK-24034', 'PAID', 67000, 'Emphasize free delivery threshold in Multan.', 'COMPLETED', 100, '2026-04-27', '2026-04-28', '2026-04-22 16:30:00+05', '2026-04-27 20:15:00+05'),
    ('40000000-0000-4000-8000-000000000005', '30000000-0000-4000-8000-000000000010', '10000000-0000-4000-8000-000000000010', '20000000-0000-4000-8000-000000000008', 'ORD-PK-24035', 'PAID', 86000, 'Tie into PSL match week messaging.', 'COMPLETED', 100, '2026-03-29', '2026-03-30', '2026-03-24 09:50:00+05', '2026-03-29 18:40:00+05'),
    ('40000000-0000-4000-8000-000000000006', '30000000-0000-4000-8000-000000000012', '10000000-0000-4000-8000-000000000012', '20000000-0000-4000-8000-000000000009', 'ORD-PK-24036', 'PAID', 138000, 'Explain cashback flow for app users.', 'IN_PROGRESS', 75, null, '2026-05-20', '2026-05-14 15:25:00+05', '2026-05-17 12:00:00+05'),
    ('40000000-0000-4000-8000-000000000007', '30000000-0000-4000-8000-000000000014', '10000000-0000-4000-8000-000000000014', '20000000-0000-4000-8000-000000000004', 'ORD-PK-24037', 'PAID', 98000, 'Push midnight sale CTA for gaming accessories.', 'COMPLETED', 100, '2026-05-03', '2026-05-04', '2026-04-28 12:10:00+05', '2026-05-03 23:10:00+05'),
    ('40000000-0000-4000-8000-000000000008', '30000000-0000-4000-8000-000000000019', '10000000-0000-4000-8000-000000000019', '20000000-0000-4000-8000-000000000002', 'ORD-PK-24038', 'PAID', 121000, 'Show two styling variants for mehndi and baraat.', 'COMPLETED', 100, '2026-04-15', '2026-04-16', '2026-04-09 11:00:00+05', '2026-04-15 21:00:00+05'),
    ('40000000-0000-4000-8000-000000000009', '30000000-0000-4000-8000-000000000020', '10000000-0000-4000-8000-000000000020', '20000000-0000-4000-8000-000000000007', 'ORD-PK-24039', 'PAID', 110000, 'Need Urdu and Roman Urdu subtitles.', 'COMPLETED', 100, '2026-04-22', '2026-04-23', '2026-04-17 14:05:00+05', '2026-04-22 19:30:00+05'),
    ('40000000-0000-4000-8000-000000000010', '30000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000005', 'ORD-PK-24040', 'PAID', 84000, 'Add resale value note for used car buyers.', 'IN_PROGRESS', 55, null, '2026-05-24', '2026-05-19 10:45:00+05', '2026-05-20 17:30:00+05')
on conflict (id) do nothing;

insert into reviews (id, package_id, reviewer_id, order_id, creator_id, brand_id, star, rating, description, comment, created_at, updated_at)
values
    ('50000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 5, 5, 'Excellent visual quality and high saves-to-reach ratio.', 'Campaign delivered clear product discovery and strong click-through.', '2026-04-13 12:15:00+05', '2026-04-13 12:15:00+05'),
    ('50000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000004', '40000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000004', 5, 5, 'Detailed review with practical buyer guidance and strong retention.', 'Helped increase launch-week product page sessions.', '2026-04-22 10:10:00+05', '2026-04-22 10:10:00+05'),
    ('50000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000006', '20000000-0000-4000-8000-000000000010', '40000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000006', '20000000-0000-4000-8000-000000000010', 4, 4, 'Strong local relevance and clear promo messaging.', 'Good conversion in Multan zone; we will rebook next quarter.', '2026-04-29 11:00:00+05', '2026-04-29 11:00:00+05'),
    ('50000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000010', '20000000-0000-4000-8000-000000000008', '40000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000010', '20000000-0000-4000-8000-000000000008', 5, 5, 'Great timing with matchday context and high comment activity.', 'Excellent campaign fit for sports audience.', '2026-03-31 09:50:00+05', '2026-03-31 09:50:00+05'),
    ('50000000-0000-4000-8000-000000000005', '30000000-0000-4000-8000-000000000014', '20000000-0000-4000-8000-000000000004', '40000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000014', '20000000-0000-4000-8000-000000000004', 4, 4, 'Fast turnaround and strong CTA placement.', 'Gamers responded well to the midnight sale hook.', '2026-05-05 13:30:00+05', '2026-05-05 13:30:00+05'),
    ('50000000-0000-4000-8000-000000000006', '30000000-0000-4000-8000-000000000019', '20000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000019', '20000000-0000-4000-8000-000000000002', 5, 5, 'Strong festive storytelling with clear purchase intent.', 'Traffic to festive collection pages rose materially.', '2026-04-17 10:25:00+05', '2026-04-17 10:25:00+05')
on conflict (id) do nothing;

insert into package_analytics (
    package_id, views, clicks, inquiries, conversion_rate, completion_rate,
    repeat_brands, engagement_performance, updated_at
)
values
    ('30000000-0000-4000-8000-000000000001', 84500, 12680, 352, 2.78, 95.10, 6, 90.70, '2026-05-10 09:00:00+05'),
    ('30000000-0000-4000-8000-000000000002', 93500, 14230, 404, 2.84, 94.80, 7, 91.40, '2026-05-10 09:00:00+05'),
    ('30000000-0000-4000-8000-000000000005', 78900, 11970, 315, 2.63, 93.90, 5, 89.60, '2026-05-10 09:00:00+05'),
    ('30000000-0000-4000-8000-000000000010', 88700, 13620, 372, 2.73, 95.00, 6, 90.90, '2026-05-10 09:00:00+05'),
    ('30000000-0000-4000-8000-000000000019', 81200, 12450, 341, 2.74, 94.60, 6, 90.20, '2026-05-10 09:00:00+05')
on conflict (package_id) do nothing;

-- Pakistan expansion set (phase 2): 20 additional creators and 12 additional brands.
insert into users (id, username, email, password_hash, role, name, city, phone, creator_program_status, is_active)
values
    ('10000000-0000-4000-8000-000000000001', 'hira.ashraf', 'hira.ashraf@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Hira Ashraf', 'Karachi', '+92-300-5001001', 'IN_PATH', true),
    ('10000000-0000-4000-8000-000000000002', 'bilal.naeem', 'bilal.naeem@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Bilal Naeem', 'Lahore', '+92-333-5001002', 'IN_PATH', true),
    ('10000000-0000-4000-8000-000000000003', 'mariam.ikram', 'mariam.ikram@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Mariam Ikram', 'Islamabad', '+92-321-5001003', 'IN_PATH', true),
    ('10000000-0000-4000-8000-000000000004', 'ahmed.sheraz', 'ahmed.sheraz@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Ahmed Sheraz', 'Rawalpindi', '+92-302-5001004', 'IN_PATH', true),
    ('10000000-0000-4000-8000-000000000005', 'kinza.rana', 'kinza.rana@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Kinza Rana', 'Faisalabad', '+92-345-5001005', 'IN_PATH', true),
    ('10000000-0000-4000-8000-000000000006', 'daniyal.qureshi', 'daniyal.qureshi@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Daniyal Qureshi', 'Multan', '+92-300-5001006', 'NONE', true),
    ('10000000-0000-4000-8000-000000000007', 'ayesha.sami', 'ayesha.sami@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Ayesha Sami', 'Peshawar', '+92-333-5001007', 'NONE', true),
    ('10000000-0000-4000-8000-000000000008', 'talha.javed', 'talha.javed@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Talha Javed', 'Quetta', '+92-321-5001008', 'NONE', true),
    ('10000000-0000-4000-8000-000000000009', 'meesha.khalid', 'meesha.khalid@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Meesha Khalid', 'Hyderabad', '+92-302-5001009', 'NONE', true),
    ('10000000-0000-4000-8000-000000000010', 'farhan.aslam', 'farhan.aslam@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Farhan Aslam', 'Sialkot', '+92-345-5001010', 'IN_PATH', true),
    ('10000000-0000-4000-8000-000000000011', 'iqra.hassan', 'iqra.hassan@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Iqra Hassan', 'Gujranwala', '+92-300-5001011', 'NONE', true),
    ('10000000-0000-4000-8000-000000000012', 'saad.farooq', 'saad.farooq@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Saad Farooq', 'Lahore', '+92-333-5001012', 'IN_PATH', true),
    ('10000000-0000-4000-8000-000000000013', 'nida.kamran', 'nida.kamran@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Nida Kamran', 'Karachi', '+92-321-5001013', 'NONE', true),
    ('10000000-0000-4000-8000-000000000014', 'umair.latif', 'umair.latif@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Umair Latif', 'Islamabad', '+92-302-5001014', 'IN_PATH', true),
    ('10000000-0000-4000-8000-000000000015', 'rabia.yousaf', 'rabia.yousaf@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Rabia Yousaf', 'Multan', '+92-345-5001015', 'NONE', true),
    ('10000000-0000-4000-8000-000000000016', 'adnan.maqsood', 'adnan.maqsood@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Adnan Maqsood', 'Faisalabad', '+92-300-5001016', 'NONE', true),
    ('10000000-0000-4000-8000-000000000017', 'mahnoor.zahid', 'mahnoor.zahid@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Mahnoor Zahid', 'Rawalpindi', '+92-333-5001017', 'IN_PATH', true),
    ('10000000-0000-4000-8000-000000000018', 'shahzaib.rauf', 'shahzaib.rauf@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Shahzaib Rauf', 'Peshawar', '+92-321-5001018', 'NONE', true),
    ('10000000-0000-4000-8000-000000000019', 'areeba.naz', 'areeba.naz@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Areeba Naz', 'Lahore', '+92-302-5001019', 'IN_PATH', true),
    ('10000000-0000-4000-8000-000000000020', 'hassan.imran', 'hassan.imran@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Hassan Imran', 'Karachi', '+92-345-5001020', 'NONE', true),
    ('20000000-0000-4000-8000-000000000001', 'khaadi.pr', 'influencer@khaadi.com', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Khaadi PR Team', 'Karachi', '+92-21-111-222-444', 'NONE', true),
    ('20000000-0000-4000-8000-000000000002', 'gulahmed.media', 'digital@gulahmedshop.com', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Gul Ahmed Media', 'Karachi', '+92-21-111-001-002', 'NONE', true),
    ('20000000-0000-4000-8000-000000000003', 'servis.shoes', 'creator@servis.com', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Servis Shoes Digital', 'Lahore', '+92-42-111-737-847', 'NONE', true),
    ('20000000-0000-4000-8000-000000000004', 'daraz.partnerships', 'influencers@daraz.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Daraz Partnerships', 'Karachi', '+92-21-111-132-729', 'NONE', true),
    ('20000000-0000-4000-8000-000000000005', 'pakwheels.brands', 'campaigns@pakwheels.com', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'PakWheels Brand Studio', 'Lahore', '+92-42-111-943-357', 'NONE', true),
    ('20000000-0000-4000-8000-000000000006', 'jubilee.life', 'digital@jubileelife.com', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Jubilee Life Digital', 'Karachi', '+92-21-111-111-554', 'NONE', true),
    ('20000000-0000-4000-8000-000000000007', 'nestle.pk', 'influencer@pk.nestle.com', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Nestle Pakistan', 'Lahore', '+92-42-111-637-853', 'NONE', true),
    ('20000000-0000-4000-8000-000000000008', 'tapal.tea', 'digital@tapal.com', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Tapal Tea Digital', 'Karachi', '+92-21-111-827-257', 'NONE', true),
    ('20000000-0000-4000-8000-000000000009', 'bankalfalah.digital', 'social@bankalfalah.com', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Bank Alfalah Digital', 'Karachi', '+92-21-111-225-111', 'NONE', true),
    ('20000000-0000-4000-8000-000000000010', 'careem.food', 'influencer@careem.com', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Careem Food Marketing', 'Karachi', '+92-21-111-227-336', 'NONE', true),
    ('20000000-0000-4000-8000-000000000011', 'cheezious.media', 'influencer@cheezious.com', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Cheezious Media Team', 'Islamabad', '+92-51-111-111-119', 'NONE', true),
    ('20000000-0000-4000-8000-000000000012', 'jazz.digital', 'creators@jazz.com.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Jazz Digital Marketing', 'Islamabad', '+92-51-111-300-300', 'NONE', true)
on conflict (id) do update set
    email                   = excluded.email,
    password_hash           = excluded.password_hash,
    name                    = excluded.name,
    city                    = excluded.city,
    phone                   = excluded.phone,
    creator_program_status  = excluded.creator_program_status,
    is_active               = excluded.is_active;

insert into creators (
    id, username, bio, availability_status, response_time,
    min_price, max_price, followers, avg_views, engagement_rate,
    is_verified, is_trending, is_fast_responder, rating, total_reviews, completed_deals,
    collaboration_preferences, minimum_budget, languages, categories
)
values
    ('10000000-0000-4000-8000-000000000001', 'hira.ashraf',    'Modest fashion creator known for festive and everyday wardrobe styling.',                         'AVAILABLE', 'within_12_hours', 28000, 125000, 354000, 76000, 6.90, true,  true,  false, 4.70, 66, 149, '["paid", "barter", "hybrid"]'::jsonb, 20000, '["Urdu", "English"]'::jsonb,          '["FASHION"]'::jsonb),
    ('10000000-0000-4000-8000-000000000002', 'bilal.naeem',    'Consumer tech explainer focused on value-for-money gadgets and smartphones.',                    'AVAILABLE', 'within_6_hours',  32000, 165000, 428000, 94000, 6.20, true,  true,  true,  4.80, 71, 172, '["paid", "barter", "hybrid"]'::jsonb, 25000, '["Urdu", "English"]'::jsonb,          '["TECH"]'::jsonb),
    ('10000000-0000-4000-8000-000000000003', 'mariam.ikram',   'Parenting creator covering practical routines for young urban families.',                        'AVAILABLE', 'within_24_hours', 22000, 95000,  266000, 51000, 5.70, false, false, false, 4.50, 38, 96, '["paid", "barter", "hybrid"]'::jsonb, 15000, '["Urdu", "English"]'::jsonb,          '["PARENTING_FAMILY", "LIFESTYLE"]'::jsonb),
    ('10000000-0000-4000-8000-000000000004', 'ahmed.sheraz',   'Automotive reviewer producing ownership-cost and resale-value explainers.',                     'AVAILABLE', 'within_12_hours', 30000, 150000, 301000, 69000, 5.90, true,  false, true,  4.60, 47, 114, '["paid", "barter", "hybrid"]'::jsonb, 22000, '["Urdu", "English"]'::jsonb,          '["AUTOMOTIVE", "TECH"]'::jsonb),
    ('10000000-0000-4000-8000-000000000005', 'kinza.rana',     'Skincare educator creating climate-appropriate routines for Pakistan audiences.',               'AVAILABLE', 'within_12_hours', 26000, 110000, 332000, 74000, 7.00, true,  true,  false, 4.70, 59, 137, '["paid", "barter", "hybrid"]'::jsonb, 18000, '["Urdu", "Punjabi", "English"]'::jsonb, '["BEAUTY"]'::jsonb),
    ('10000000-0000-4000-8000-000000000006', 'daniyal.qureshi','Food discovery creator covering local dining and app-based offers in South Punjab.',           'AVAILABLE', 'within_24_hours', 24000, 98000,  281000, 58000, 6.00, false, true,  true,  4.50, 42, 101, '["paid", "barter", "hybrid"]'::jsonb, 17000, '["Urdu", "Saraiki", "English"]'::jsonb, '["FOOD"]'::jsonb),
    ('10000000-0000-4000-8000-000000000007', 'ayesha.sami',    'Education creator helping students with exam prep and productivity systems.',                   'AVAILABLE', 'within_24_hours', 18000, 76000,  219000, 47000, 5.40, false, false, false, 4.40, 31, 79, '["paid", "barter", "hybrid"]'::jsonb, 12000, '["Urdu", "English", "Pashto"]'::jsonb, '["EDUCATION"]'::jsonb),
    ('10000000-0000-4000-8000-000000000008', 'talha.javed',    'Travel vlogger focused on domestic routes, budgets, and itinerary planning.',                  'AVAILABLE', 'within_24_hours', 27000, 118000, 247000, 55000, 5.60, false, false, false, 4.50, 34, 88, '["paid", "barter", "hybrid"]'::jsonb, 18000, '["Urdu", "English"]'::jsonb,          '["TRAVEL", "LIFESTYLE"]'::jsonb),
    ('10000000-0000-4000-8000-000000000009', 'meesha.khalid',  'Home organization creator focused on affordable household optimization.',                       'AVAILABLE', 'within_24_hours', 19000, 86000,  208000, 46000, 5.10, false, false, false, 4.30, 26, 67, '["paid", "barter", "hybrid"]'::jsonb, 13000, '["Urdu", "Sindhi"]'::jsonb,           '["HOME_DECOR"]'::jsonb),
    ('10000000-0000-4000-8000-000000000010', 'farhan.aslam',   'Cricket analyst with high-retention short videos around match cycles.',                        'AVAILABLE', 'within_12_hours', 25000, 130000, 396000, 84000, 6.30, true,  true,  true,  4.70, 52, 126, '["paid", "barter", "hybrid"]'::jsonb, 19000, '["Urdu", "English"]'::jsonb,          '["SPORTS", "ENTERTAINMENT"]'::jsonb),
    ('10000000-0000-4000-8000-000000000011', 'iqra.hassan',    'Lifestyle creator with strong female audience in central Punjab.',                             'AVAILABLE', 'within_12_hours', 21000, 92000,  256000, 52000, 5.80, false, false, false, 4.50, 37, 90, '["paid", "barter", "hybrid"]'::jsonb, 14000, '["Urdu", "Punjabi"]'::jsonb,          '["LIFESTYLE", "HOME_DECOR"]'::jsonb),
    ('10000000-0000-4000-8000-000000000012', 'saad.farooq',    'Personal finance creator simplifying digital banking for salaried users.',                    'AVAILABLE', 'within_12_hours', 34000, 170000, 312000, 68000, 5.50, true,  false, true,  4.60, 45, 109, '["paid", "barter", "hybrid"]'::jsonb, 25000, '["Urdu", "English"]'::jsonb,          '["BUSINESS_FINANCE"]'::jsonb),
    ('10000000-0000-4000-8000-000000000013', 'nida.kamran',    'Nutrition creator sharing practical wellness habits for working families.',                    'AVAILABLE', 'within_24_hours', 23000, 98000,  241000, 49000, 5.90, false, false, false, 4.50, 33, 82, '["paid", "barter", "hybrid"]'::jsonb, 16000, '["Urdu", "English"]'::jsonb,          '["HEALTH", "FITNESS"]'::jsonb),
    ('10000000-0000-4000-8000-000000000014', 'umair.latif',    'Gaming creator covering mobile esports and gear recommendations.',                             'AVAILABLE', 'within_6_hours',  30000, 145000, 377000, 91000, 6.80, true,  true,  true,  4.70, 49, 121, '["paid", "barter", "hybrid"]'::jsonb, 22000, '["Urdu", "English"]'::jsonb,          '["GAMING"]'::jsonb),
    ('10000000-0000-4000-8000-000000000015', 'rabia.yousaf',   'Home-cooking creator focused on weekly meal planning and traditional recipes.',               'AVAILABLE', 'within_24_hours', 20000, 88000,  289000, 61000, 6.20, false, true,  false, 4.60, 40, 97, '["paid", "barter", "hybrid"]'::jsonb, 15000, '["Urdu", "Punjabi"]'::jsonb,          '["FOOD"]'::jsonb),
    ('10000000-0000-4000-8000-000000000016', 'adnan.maqsood',  'Agri creator producing practical content for modern farming workflows.',                      'AVAILABLE', 'within_24_hours', 26000, 116000, 184000, 39000, 5.00, false, false, false, 4.30, 24, 58, '["paid", "barter", "hybrid"]'::jsonb, 17000, '["Urdu", "Punjabi"]'::jsonb,          '["GENERAL", "TECH"]'::jsonb),
    ('10000000-0000-4000-8000-000000000017', 'mahnoor.zahid',  'Books and study productivity creator for university audiences.',                              'AVAILABLE', 'within_12_hours', 17000, 74000,  196000, 43000, 5.30, false, false, false, 4.40, 28, 72, '["paid", "barter", "hybrid"]'::jsonb, 11000, '["Urdu", "English"]'::jsonb,          '["EDUCATION"]'::jsonb),
    ('10000000-0000-4000-8000-000000000018', 'shahzaib.rauf',  'Motorbike creator focused on maintenance and rider safety.',                                  'AVAILABLE', 'within_12_hours', 22000, 100000, 267000, 57000, 5.70, false, false, true,  4.50, 35, 87, '["paid", "barter", "hybrid"]'::jsonb, 16000, '["Urdu", "Pashto"]'::jsonb,           '["AUTOMOTIVE"]'::jsonb),
    ('10000000-0000-4000-8000-000000000019', 'areeba.naz',     'Bridal and festive fashion creator known for high purchase-intent content.',                  'AVAILABLE', 'within_12_hours', 32000, 155000, 348000, 81000, 6.60, true,  true,  false, 4.70, 54, 132, '["paid", "barter", "hybrid"]'::jsonb, 24000, '["Urdu", "English"]'::jsonb,          '["FASHION", "BEAUTY"]'::jsonb),
    ('10000000-0000-4000-8000-000000000020', 'hassan.imran',   'Comedy creator with strong short-form reach in metro markets.',                               'AVAILABLE', 'within_6_hours',  28000, 138000, 503000, 125000,7.20, true,  true,  true,  4.80, 63, 158, '["paid", "barter", "hybrid"]'::jsonb, 21000, '["Urdu", "English"]'::jsonb,          '["ENTERTAINMENT"]'::jsonb)
on conflict (id) do update set
    username          = excluded.username,
    bio               = excluded.bio,
    availability_status = excluded.availability_status,
    response_time     = excluded.response_time,
    min_price         = excluded.min_price,
    max_price         = excluded.max_price,
    followers         = excluded.followers,
    avg_views         = excluded.avg_views,
    engagement_rate   = excluded.engagement_rate,
    is_verified       = excluded.is_verified,
    is_trending       = excluded.is_trending,
    is_fast_responder = excluded.is_fast_responder,
    rating            = excluded.rating,
    total_reviews     = excluded.total_reviews,
    completed_deals   = excluded.completed_deals,
    minimum_budget    = excluded.minimum_budget,
    languages         = excluded.languages,
    categories        = excluded.categories;

insert into social_accounts (id, creator_id, platform, username, profile_url, followers, avg_views, engagement_rate, is_verified)
values
    ('e1000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'tiktok',    'hira.ashraf.style',    'https://www.tiktok.com/@hira.ashraf.style',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'instagram', 'hira.ashraf',          'https://www.instagram.com/hira.ashraf',          0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', 'youtube',   'HiraAshrafStyle',      'https://www.youtube.com/@HiraAshrafStyle',       0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', 'facebook',  'hiraashrafstyle',      'https://www.facebook.com/hiraashrafstyle',       0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000002', 'tiktok',    'bilal.naeem.tech',     'https://www.tiktok.com/@bilal.naeem.tech',       0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000002', 'instagram', 'bilal.naeem',          'https://www.instagram.com/bilal.naeem',          0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000002', 'youtube',   'BilalNaeemTech',       'https://www.youtube.com/@BilalNaeemTech',        0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000002', 'facebook',  'bilalnaeemtech',       'https://www.facebook.com/bilalnaeemtech',        0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000009', '10000000-0000-4000-8000-000000000003', 'tiktok',    'mariam.ikram.family',  'https://www.tiktok.com/@mariam.ikram.family',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000010', '10000000-0000-4000-8000-000000000003', 'instagram', 'mariam.ikram',         'https://www.instagram.com/mariam.ikram',         0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000003', 'youtube',   'MariamIkramFamily',    'https://www.youtube.com/@MariamIkramFamily',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000012', '10000000-0000-4000-8000-000000000003', 'facebook',  'mariamikramfamily',    'https://www.facebook.com/mariamikramfamily',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000013', '10000000-0000-4000-8000-000000000004', 'tiktok',    'ahmed.sheraz.auto',    'https://www.tiktok.com/@ahmed.sheraz.auto',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000014', '10000000-0000-4000-8000-000000000004', 'instagram', 'ahmed.sheraz',         'https://www.instagram.com/ahmed.sheraz',         0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000015', '10000000-0000-4000-8000-000000000004', 'youtube',   'AhmedSherazAuto',      'https://www.youtube.com/@AhmedSherazAuto',       0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000016', '10000000-0000-4000-8000-000000000004', 'facebook',  'ahmedsherazauto',      'https://www.facebook.com/ahmedsherazauto',       0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000017', '10000000-0000-4000-8000-000000000005', 'tiktok',    'kinza.rana.skin',      'https://www.tiktok.com/@kinza.rana.skin',        0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000018', '10000000-0000-4000-8000-000000000005', 'instagram', 'kinza.rana',           'https://www.instagram.com/kinza.rana',           0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000019', '10000000-0000-4000-8000-000000000005', 'youtube',   'KinzaRanaSkincare',    'https://www.youtube.com/@KinzaRanaSkincare',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000020', '10000000-0000-4000-8000-000000000005', 'facebook',  'kinzaranaskincare',    'https://www.facebook.com/kinzaranaskincare',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000021', '10000000-0000-4000-8000-000000000006', 'tiktok',    'daniyal.qureshi.food', 'https://www.tiktok.com/@daniyal.qureshi.food',   0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000022', '10000000-0000-4000-8000-000000000006', 'instagram', 'daniyal.qureshi',      'https://www.instagram.com/daniyal.qureshi',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000023', '10000000-0000-4000-8000-000000000006', 'youtube',   'DaniyalQureshiFood',   'https://www.youtube.com/@DaniyalQureshiFood',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000024', '10000000-0000-4000-8000-000000000006', 'facebook',  'daniyalqureshifood',   'https://www.facebook.com/daniyalqureshifood',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000025', '10000000-0000-4000-8000-000000000007', 'tiktok',    'ayesha.sami.study',    'https://www.tiktok.com/@ayesha.sami.study',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000026', '10000000-0000-4000-8000-000000000007', 'instagram', 'ayesha.sami',          'https://www.instagram.com/ayesha.sami',          0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000027', '10000000-0000-4000-8000-000000000007', 'youtube',   'AyeshaSamiStudy',      'https://www.youtube.com/@AyeshaSamiStudy',       0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000028', '10000000-0000-4000-8000-000000000007', 'facebook',  'ayeshasamistudy',      'https://www.facebook.com/ayeshasamistudy',       0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000029', '10000000-0000-4000-8000-000000000008', 'tiktok',    'talha.javed.travel',   'https://www.tiktok.com/@talha.javed.travel',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000030', '10000000-0000-4000-8000-000000000008', 'instagram', 'talha.javed',          'https://www.instagram.com/talha.javed',          0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000031', '10000000-0000-4000-8000-000000000008', 'youtube',   'TalhaJavedTravel',     'https://www.youtube.com/@TalhaJavedTravel',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000032', '10000000-0000-4000-8000-000000000008', 'facebook',  'talhajavedtravel',     'https://www.facebook.com/talhajavedtravel',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000033', '10000000-0000-4000-8000-000000000009', 'tiktok',    'meesha.khalid.home',   'https://www.tiktok.com/@meesha.khalid.home',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000034', '10000000-0000-4000-8000-000000000009', 'instagram', 'meesha.khalid',        'https://www.instagram.com/meesha.khalid',        0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000035', '10000000-0000-4000-8000-000000000009', 'youtube',   'MeeshaKhalidHome',     'https://www.youtube.com/@MeeshaKhalidHome',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000036', '10000000-0000-4000-8000-000000000009', 'facebook',  'meeshakhalidhome',     'https://www.facebook.com/meeshakhalidhome',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000037', '10000000-0000-4000-8000-000000000010', 'tiktok',    'farhan.aslam.cricket', 'https://www.tiktok.com/@farhan.aslam.cricket',   0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000038', '10000000-0000-4000-8000-000000000010', 'instagram', 'farhan.aslam',         'https://www.instagram.com/farhan.aslam',         0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000039', '10000000-0000-4000-8000-000000000010', 'youtube',   'FarhanAslamCricket',   'https://www.youtube.com/@FarhanAslamCricket',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000040', '10000000-0000-4000-8000-000000000010', 'facebook',  'farhanaslamcricket',   'https://www.facebook.com/farhanaslamcricket',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000041', '10000000-0000-4000-8000-000000000011', 'tiktok',    'iqra.hassan.lifestyle','https://www.tiktok.com/@iqra.hassan.lifestyle',  0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000042', '10000000-0000-4000-8000-000000000011', 'instagram', 'iqra.hassan',          'https://www.instagram.com/iqra.hassan',          0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000043', '10000000-0000-4000-8000-000000000011', 'youtube',   'IqraHassanLifestyle',  'https://www.youtube.com/@IqraHassanLifestyle',   0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000044', '10000000-0000-4000-8000-000000000011', 'facebook',  'iqrahassanlifestyle',  'https://www.facebook.com/iqrahassanlifestyle',   0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000045', '10000000-0000-4000-8000-000000000012', 'tiktok',    'saad.farooq.finance',  'https://www.tiktok.com/@saad.farooq.finance',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000046', '10000000-0000-4000-8000-000000000012', 'instagram', 'saad.farooq',          'https://www.instagram.com/saad.farooq',          0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000047', '10000000-0000-4000-8000-000000000012', 'youtube',   'SaadFarooqFinance',    'https://www.youtube.com/@SaadFarooqFinance',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000048', '10000000-0000-4000-8000-000000000012', 'facebook',  'saadfarooqfinance',    'https://www.facebook.com/saadfarooqfinance',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000049', '10000000-0000-4000-8000-000000000013', 'tiktok',    'nida.kamran.health',   'https://www.tiktok.com/@nida.kamran.health',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000050', '10000000-0000-4000-8000-000000000013', 'instagram', 'nida.kamran',          'https://www.instagram.com/nida.kamran',          0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000051', '10000000-0000-4000-8000-000000000013', 'youtube',   'NidaKamranWellness',   'https://www.youtube.com/@NidaKamranWellness',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000052', '10000000-0000-4000-8000-000000000013', 'facebook',  'nidakamranwellness',   'https://www.facebook.com/nidakamranwellness',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000053', '10000000-0000-4000-8000-000000000014', 'tiktok',    'umair.latif.gaming',   'https://www.tiktok.com/@umair.latif.gaming',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000054', '10000000-0000-4000-8000-000000000014', 'instagram', 'umair.latif',          'https://www.instagram.com/umair.latif',          0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000055', '10000000-0000-4000-8000-000000000014', 'youtube',   'UmairLatifGaming',     'https://www.youtube.com/@UmairLatifGaming',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000056', '10000000-0000-4000-8000-000000000014', 'facebook',  'umairlatifgaming',     'https://www.facebook.com/umairlatifgaming',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000057', '10000000-0000-4000-8000-000000000015', 'tiktok',    'rabia.yousaf.cooking', 'https://www.tiktok.com/@rabia.yousaf.cooking',   0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000058', '10000000-0000-4000-8000-000000000015', 'instagram', 'rabia.yousaf',         'https://www.instagram.com/rabia.yousaf',         0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000059', '10000000-0000-4000-8000-000000000015', 'youtube',   'RabiaYousafKitchen',   'https://www.youtube.com/@RabiaYousafKitchen',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000060', '10000000-0000-4000-8000-000000000015', 'facebook',  'rabiayousafkitchen',   'https://www.facebook.com/rabiayousafkitchen',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000061', '10000000-0000-4000-8000-000000000016', 'tiktok',    'adnan.maqsood.agri',   'https://www.tiktok.com/@adnan.maqsood.agri',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000062', '10000000-0000-4000-8000-000000000016', 'instagram', 'adnan.maqsood',        'https://www.instagram.com/adnan.maqsood',        0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000063', '10000000-0000-4000-8000-000000000016', 'youtube',   'AdnanMaqsoodAgri',     'https://www.youtube.com/@AdnanMaqsoodAgri',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000064', '10000000-0000-4000-8000-000000000016', 'facebook',  'adnanmaqsoodagri',     'https://www.facebook.com/adnanmaqsoodagri',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000065', '10000000-0000-4000-8000-000000000017', 'tiktok',    'mahnoor.zahid.books',  'https://www.tiktok.com/@mahnoor.zahid.books',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000066', '10000000-0000-4000-8000-000000000017', 'instagram', 'mahnoor.zahid',        'https://www.instagram.com/mahnoor.zahid',        0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000067', '10000000-0000-4000-8000-000000000017', 'youtube',   'MahnoorZahidBooks',    'https://www.youtube.com/@MahnoorZahidBooks',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000068', '10000000-0000-4000-8000-000000000017', 'facebook',  'mahnoorzahidbooks',    'https://www.facebook.com/mahnoorzahidbooks',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000069', '10000000-0000-4000-8000-000000000018', 'tiktok',    'shahzaib.rauf.bikes',  'https://www.tiktok.com/@shahzaib.rauf.bikes',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000070', '10000000-0000-4000-8000-000000000018', 'instagram', 'shahzaib.rauf',        'https://www.instagram.com/shahzaib.rauf',        0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000071', '10000000-0000-4000-8000-000000000018', 'youtube',   'ShahzaibRaufBikes',    'https://www.youtube.com/@ShahzaibRaufBikes',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000072', '10000000-0000-4000-8000-000000000018', 'facebook',  'shahzaibraufbikes',    'https://www.facebook.com/shahzaibraufbikes',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000073', '10000000-0000-4000-8000-000000000019', 'tiktok',    'areeba.naz.fashion',   'https://www.tiktok.com/@areeba.naz.fashion',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000074', '10000000-0000-4000-8000-000000000019', 'instagram', 'areeba.naz',           'https://www.instagram.com/areeba.naz',           0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000075', '10000000-0000-4000-8000-000000000019', 'youtube',   'AreebaNazFashion',     'https://www.youtube.com/@AreebaNazFashion',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000076', '10000000-0000-4000-8000-000000000019', 'facebook',  'areebanazfashion',     'https://www.facebook.com/areebanazfashion',      0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000077', '10000000-0000-4000-8000-000000000020', 'tiktok',    'hassan.imran.comedy',  'https://www.tiktok.com/@hassan.imran.comedy',    0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000078', '10000000-0000-4000-8000-000000000020', 'instagram', 'hassan.imran',         'https://www.instagram.com/hassan.imran',         0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000079', '10000000-0000-4000-8000-000000000020', 'youtube',   'HassanImranComedy',    'https://www.youtube.com/@HassanImranComedy',     0, 0, 0, false),
    ('e1000000-0000-4000-8000-000000000080', '10000000-0000-4000-8000-000000000020', 'facebook',  'hassanimrancomedy',    'https://www.facebook.com/hassanimrancomedy',     0, 0, 0, false)
on conflict (id) do nothing;

insert into brands (id, name, logo_url, website, category, description, monthly_budget)
values
    ('20000000-0000-4000-8000-000000000001', 'Khaadi', 'https://images.chumchum.pk/brands/logos/khaadi.png', 'https://www.khaadi.com', 'FASHION', 'Fashion retail brand running monthly seasonal creator activations.', 3800000),
    ('20000000-0000-4000-8000-000000000002', 'Gul Ahmed', 'https://images.chumchum.pk/brands/logos/gulahmed.png', 'https://www.gulahmedshop.com', 'FASHION', 'Textile and fashion brand investing in broad creator funnels.', 3500000),
    ('20000000-0000-4000-8000-000000000003', 'Servis', 'https://images.chumchum.pk/brands/logos/servis.png', 'https://www.servis.com', 'FASHION', 'Footwear campaigns focused on youth categories and launches.', 2100000),
    ('20000000-0000-4000-8000-000000000004', 'Daraz Pakistan', 'https://images.chumchum.pk/brands/logos/daraz.png', 'https://www.daraz.pk', 'GENERAL', 'Marketplace campaigns around sale cycles and category pushes.', 7000000),
    ('20000000-0000-4000-8000-000000000005', 'PakWheels', 'https://images.chumchum.pk/brands/logos/pakwheels.png', 'https://www.pakwheels.com', 'AUTOMOTIVE', 'Automotive platform using creator-led trust and lead generation.', 2500000),
    ('20000000-0000-4000-8000-000000000006', 'Jubilee Life', 'https://images.chumchum.pk/brands/logos/jubilee-life.png', 'https://jubileelife.com', 'BUSINESS_FINANCE', 'Insurance provider running trust-building educational campaigns.', 2400000),
    ('20000000-0000-4000-8000-000000000007', 'Nestle Pakistan', 'https://images.chumchum.pk/brands/logos/nestle-pk.png', 'https://www.nestle.pk', 'FOOD', 'FMCG campaigns for family and wellness product lines.', 5200000),
    ('20000000-0000-4000-8000-000000000008', 'Tapal Tea', 'https://images.chumchum.pk/brands/logos/tapal.png', 'https://www.tapal.com', 'FOOD', 'Seasonal tea culture and recipe-led creator collaborations.', 1800000),
    ('20000000-0000-4000-8000-000000000009', 'Bank Alfalah', 'https://images.chumchum.pk/brands/logos/bank-alfalah.png', 'https://www.bankalfalah.com', 'BUSINESS_FINANCE', 'Retail bank promoting digital products via finance creators.', 2900000),
    ('20000000-0000-4000-8000-000000000010', 'Careem Food', 'https://images.chumchum.pk/brands/logos/careem-food.png', 'https://www.careem.com', 'FOOD', 'Localized city-level campaign execution with conversion goals.', 4100000),
    ('20000000-0000-4000-8000-000000000011', 'Cheezious', 'https://images.chumchum.pk/brands/logos/cheezious.png', 'https://www.cheezious.com', 'FOOD', 'QSR brand focused on youth-heavy video and reel campaigns.', 2300000),
    ('20000000-0000-4000-8000-000000000012', 'Jazz', 'https://images.chumchum.pk/brands/logos/jazz.png', 'https://www.jazz.com.pk', 'TECH', 'Telecom brand scaling creator-led bundles and data offers.', 5600000)
on conflict (id) do nothing;

insert into notification_preferences (
    user_id, new_orders, messages, reviews, marketing,
    weekly_digest, push_notifications, email_notifications, sms_notifications
)
values
    ('10000000-0000-4000-8000-000000000001', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000002', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000003', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000004', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000005', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000006', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000007', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000008', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000009', true, true, false, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000010', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000011', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000012', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000013', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000014', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000015', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000016', true, true, false, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000017', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000018', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000019', true, true, true, false, true, true, true, false),
    ('10000000-0000-4000-8000-000000000020', true, true, true, false, true, true, true, false),
    ('20000000-0000-4000-8000-000000000001', false, true, true, true, true, true, true, false),
    ('20000000-0000-4000-8000-000000000002', false, true, true, true, true, true, true, false),
    ('20000000-0000-4000-8000-000000000003', false, true, true, true, true, true, true, false),
    ('20000000-0000-4000-8000-000000000004', false, true, true, true, true, true, true, false),
    ('20000000-0000-4000-8000-000000000005', false, true, true, true, true, true, true, false),
    ('20000000-0000-4000-8000-000000000006', false, true, true, true, true, true, true, false),
    ('20000000-0000-4000-8000-000000000007', false, true, true, true, true, true, true, false),
    ('20000000-0000-4000-8000-000000000008', false, true, true, true, true, true, true, false),
    ('20000000-0000-4000-8000-000000000009', false, true, true, true, true, true, true, false),
    ('20000000-0000-4000-8000-000000000010', false, true, true, true, true, true, true, false),
    ('20000000-0000-4000-8000-000000000011', false, true, true, true, true, true, true, false),
    ('20000000-0000-4000-8000-000000000012', false, true, true, true, true, true, true, false)
on conflict (user_id) do nothing;


-- Spotlight campaign: recent Imtiaz Mega Store opening in E-11 Islamabad.
-- Inserted at the end so default created_at ordering naturally surfaces it on top.
insert into users (id, username, email, password_hash, role, name, city, phone, creator_program_status, is_active)
values
    ('31000000-0000-4000-8000-000000000001', 'imtiaz.e11', 'digital@imtiaz.com.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'BRAND', 'Imtiaz Marketing Islamabad', 'Islamabad', '+92-51-111-468-429', 'NONE', true),
    ('31000000-0000-4000-8000-000000000002', 'sara.islamabad', 'sara.islamabad@zingzing.pk', '$2y$10$gPlvx3weE7hKTU1nf5cN5eT2KGyte9ShYXmf/0qxc0DO/TwPKvM1u', 'CREATOR', 'Sara Islamabad', 'Islamabad', '+92-321-7001001', 'IN_PATH', true)
on conflict (id) do nothing;

insert into creators (
    id, username, bio, availability_status, response_time,
    min_price, max_price, followers, avg_views, engagement_rate,
    is_verified, is_trending, is_fast_responder, rating, total_reviews, completed_deals,
    collaboration_preferences, minimum_budget, languages, categories
)
values
    ('31000000-0000-4000-8000-000000000002', 'sara.islamabad', 'Islamabad-focused lifestyle and retail creator covering openings, local deals, and family shopping content.', 'AVAILABLE', 'within_6_hours', 35000, 165000, 412000, 99000, 6.80, true, true, true, 4.80, 76, 183, '["paid", "barter", "hybrid"]'::jsonb, 25000, '["Urdu", "English"]'::jsonb, '["LIFESTYLE", "BUSINESS_FINANCE"]'::jsonb)
on conflict (id) do nothing;

insert into social_accounts (id, creator_id, platform, username, profile_url, followers, avg_views, engagement_rate, is_verified)
values
    ('f1000000-0000-4000-8000-000000000001', '31000000-0000-4000-8000-000000000002', 'tiktok',    'sara.islamabad.city', 'https://www.tiktok.com/@sara.islamabad.city',    0, 0, 0, false),
    ('f1000000-0000-4000-8000-000000000002', '31000000-0000-4000-8000-000000000002', 'instagram', 'sara.islamabad',      'https://www.instagram.com/sara.islamabad',      0, 0, 0, false),
    ('f1000000-0000-4000-8000-000000000003', '31000000-0000-4000-8000-000000000002', 'youtube',   'SaraIslamabadCity',   'https://www.youtube.com/@SaraIslamabadCity',    0, 0, 0, false),
    ('f1000000-0000-4000-8000-000000000004', '31000000-0000-4000-8000-000000000002', 'facebook',  'saraislamabadcity',   'https://www.facebook.com/saraislamabadcity',    0, 0, 0, false)
on conflict (id) do nothing;

insert into brands (id, name, logo_url, website, category, description, monthly_budget)
values
    ('31000000-0000-4000-8000-000000000001', 'Imtiaz', 'https://images.chumchum.pk/brands/logos/imtiaz.png', 'https://imtiaz.com.pk', 'FOOD', 'National retail chain running city-specific campaigns for store launches and high-frequency grocery categories.', 6800000)
on conflict (id) do nothing;

insert into packages (
    id, creator_id, name, title, description, platform, category, pricing_type, deal_type,
    price, currency, deliverables, delivery_days, revisions, status, visibility,
    short_description, full_description, is_active, is_featured, is_popular, orders_completed
)
values
    ('32000000-0000-4000-8000-000000000001', '31000000-0000-4000-8000-000000000002', 'Imtiaz Launch Feature', 'Imtiaz Mega E-11 Opening Feature Reel + Stories', 'High-impact opening-day coverage package tailored for retail footfall and first-week offer conversion.', 'INSTAGRAM', 'BUSINESS_FINANCE', 'PAID', 'PAID', 145000, 'PKR', '["1 opening-day reel", "6 story frames", "store map + offer highlights", "48h comment support"]'::jsonb, 3, 2, 'ACTIVE', 'public', 'Front-page featured launch campaign for Imtiaz E-11', 'Designed for Islamabad launch visibility with deal-led storytelling, parking/access notes, and family basket highlights.', true, true, true, 214)
on conflict (id) do nothing;

insert into package_analytics (
    package_id, views, clicks, inquiries, conversion_rate, completion_rate,
    repeat_brands, engagement_performance, updated_at
)
values
    ('32000000-0000-4000-8000-000000000001', 186400, 31280, 972, 3.11, 96.80, 14, 95.70, '2026-05-28 21:30:00+05')
on conflict (package_id) do nothing;

insert into orders (
    id, package_id, creator_id, brand_id, order_number, deal_type, amount, message,
    status, progress, delivery_date, deadline_date, created_at, updated_at
)
values
    ('33000000-0000-4000-8000-000000000001', '32000000-0000-4000-8000-000000000001', '31000000-0000-4000-8000-000000000002', '31000000-0000-4000-8000-000000000001', 'ORD-PK-IMT-E11-001', 'PAID', 145000, 'Prioritize opening-day queue, fresh produce section, and family value deals for E-11 launch.', 'COMPLETED', 100, '2026-05-27', '2026-05-27', '2026-05-24 10:30:00+05', '2026-05-27 20:50:00+05')
on conflict (id) do nothing;

insert into reviews (id, package_id, reviewer_id, order_id, creator_id, brand_id, star, rating, description, comment, created_at, updated_at)
values
    ('34000000-0000-4000-8000-000000000001', '32000000-0000-4000-8000-000000000001', '31000000-0000-4000-8000-000000000001', '33000000-0000-4000-8000-000000000001', '31000000-0000-4000-8000-000000000002', '31000000-0000-4000-8000-000000000001', 5, 5, 'Excellent launch-day storytelling with clear location and offer communication.', 'Footfall intent and store-discovery responses were significantly above benchmark for Islamabad campaigns.', '2026-05-28 12:10:00+05', '2026-05-28 12:10:00+05')
on conflict (id) do nothing;

insert into saved_creators (brand_id, creator_id, saved_at)
values
    ('31000000-0000-4000-8000-000000000001', '31000000-0000-4000-8000-000000000002', '2026-05-23 17:00:00+05')
on conflict (brand_id, creator_id) do nothing;

insert into notification_preferences (
    user_id, new_orders, messages, reviews, marketing,
    weekly_digest, push_notifications, email_notifications, sms_notifications
)
values
    ('31000000-0000-4000-8000-000000000001', false, true, true, true, true, true, true, false),
    ('31000000-0000-4000-8000-000000000002', true, true, true, false, true, true, true, false)
on conflict (user_id) do nothing;

update creators
set badge_level = 'VERIFIED'
where is_verified = true;
