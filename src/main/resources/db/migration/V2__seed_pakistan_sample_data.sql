set search_path to core;

-- Demo users (password hash is the same for all sample users)
insert into users (id, username, email, password_hash, role, image, city, phone, is_active, created_at, updated_at) values
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1001', 'ali.raza.khi', 'ali.raza.khi@example.com', crypt('password', gen_salt('bf', 10)), 'CREATOR', 'https://images.example.com/users/ali-raza.jpg', 'Karachi', '+92-300-1112233', true, now(), now()),
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1002', 'sara.hussain.lhr', 'sara.hussain.lhr@example.com', crypt('password', gen_salt('bf', 10)), 'CREATOR', 'https://images.example.com/users/sara-hussain.jpg', 'Lahore', '+92-321-4455667', true, now(), now()),
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1003', 'usman.khan.isb', 'usman.khan.isb@example.com', crypt('password', gen_salt('bf', 10)), 'CREATOR', 'https://images.example.com/users/usman-khan.jpg', 'Islamabad', '+92-333-7788990', true, now(), now()),
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1004', 'hbl.marketing.pk', 'campaigns@hbl.com', crypt('password', gen_salt('bf', 10)), 'BRAND', 'https://images.example.com/brands/hbl.jpg', 'Karachi', '+92-21-111111425', true, now(), now()),
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1005', 'foodpanda.pk.team', 'partner.marketing@foodpanda.pk', crypt('password', gen_salt('bf', 10)), 'BRAND', 'https://images.example.com/brands/foodpanda.jpg', 'Karachi', '+92-21-111222333', true, now(), now()),
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1006', 'jazz.brand.team', 'brand.collabs@jazz.com.pk', crypt('password', gen_salt('bf', 10)), 'BRAND', 'https://images.example.com/brands/jazz.jpg', 'Islamabad', '+92-51-111300300', true, now(), now()),
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1007', 'admin.pk', 'admin@chumchum.pk', crypt('password', gen_salt('bf', 10)), 'ADMIN', 'https://images.example.com/users/admin.jpg', 'Lahore', '+92-300-0000000', true, now(), now());

insert into creators (id, bio, category, tiktok_url, instagram_url, youtube_url, followers, avg_views, engagement_rate, rating, total_reviews) values
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1001', 'Food and street culture creator focused on Karachi''s local restaurants and hidden gems.', 'Food', 'https://www.tiktok.com/@ali.eats.khi', 'https://www.instagram.com/ali.eats.khi', 'https://www.youtube.com/@ali-eats-khi', 185000, 72000, 6.40, 4.70, 18),
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1002', 'Lifestyle and fashion creator from Lahore sharing reels in Urdu and Punjabi.', 'Lifestyle', 'https://www.tiktok.com/@sara.styles.lhr', 'https://www.instagram.com/sara.styles.lhr', 'https://www.youtube.com/@sara-styles-lhr', 240000, 98000, 7.10, 4.82, 26),
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1003', 'Tech and productivity creator covering apps, gadgets, and telecom reviews.', 'Technology', 'https://www.tiktok.com/@usman.tech.isb', 'https://www.instagram.com/usman.tech.isb', 'https://www.youtube.com/@usman-tech-isb', 128000, 51000, 5.80, 4.55, 12);

insert into brands (id, company_name, website, industry, description) values
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1004', 'Habib Bank Limited', 'https://www.hbl.com', 'Banking', 'Pakistan''s leading bank running digital-first youth campaigns.'),
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1005', 'Foodpanda Pakistan', 'https://www.foodpanda.pk', 'Food Delivery', 'Food delivery platform collaborating with city-based creators.'),
('8c46d542-2f4d-4ab2-9bd8-39a3ceaf1006', 'Jazz Pakistan', 'https://jazz.com.pk', 'Telecommunications', 'Telecom brand promoting data bundles and youth products.');

insert into packages (id, creator_id, name, title, description, platform, category, type, pricing_type, barter_details, price, currency, deliverables, delivery_days, duration_days, revisions, cover_image, media_urls, tags, is_active, is_featured, created_at, updated_at) values
('a8fb7a90-4514-4df3-9d26-02f1e4d84001', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1001', 'Karachi Food Reel', 'Instagram reel for restaurant launch', 'One high-quality reel shot on-site with taste test and menu highlights in Urdu.', 'INSTAGRAM', 'Food', 'ONE_TIME', 'PAID', null, 35000.00, 'PKR', '1 Reel + 3 Story frames + location tag', 4, null, 2, 'https://images.example.com/packages/ali-food-reel.jpg', array['https://images.example.com/packages/ali-food-reel-1.jpg'], array['karachi', 'food', 'reel'], true, true, now(), now()),
('a8fb7a90-4514-4df3-9d26-02f1e4d84002', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1001', 'Ramzan Iftar Feature', 'TikTok feature for iftar deals', 'Short-form TikTok with authentic iftar review and call-to-action.', 'TIKTOK', 'Food', 'ONE_TIME', 'PAID', null, 28000.00, 'PKR', '1 TikTok video + pinned comment CTA', 3, null, 1, 'https://images.example.com/packages/ali-iftar.jpg', array['https://images.example.com/packages/ali-iftar-1.jpg'], array['ramzan', 'iftar', 'tiktok'], true, false, now(), now()),
('a8fb7a90-4514-4df3-9d26-02f1e4d84003', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1002', 'Lahore Fashion Story Pack', 'Instagram styling campaign package', 'Fashion-focused campaign with unboxing and styling tips for local audience.', 'INSTAGRAM', 'Lifestyle', 'ONE_TIME', 'PAID', null, 42000.00, 'PKR', '1 Reel + 5 Stories + product mention in caption', 5, null, 2, 'https://images.example.com/packages/sara-fashion.jpg', array['https://images.example.com/packages/sara-fashion-1.jpg'], array['lahore', 'fashion', 'lifestyle'], true, true, now(), now()),
('a8fb7a90-4514-4df3-9d26-02f1e4d84004', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1002', 'Eid Lookbook Collab', 'Eid festive lookbook collaboration', 'Seasonal Eid campaign with multiple outfit transitions and brand tag.', 'TIKTOK', 'Lifestyle', 'ONE_TIME', 'PAID', null, 50000.00, 'PKR', '1 TikTok + 1 Reel cross-post + 2 Stories', 6, null, 2, 'https://images.example.com/packages/sara-eid.jpg', array['https://images.example.com/packages/sara-eid-1.jpg'], array['eid', 'lookbook', 'fashion'], true, false, now(), now()),
('a8fb7a90-4514-4df3-9d26-02f1e4d84005', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1003', 'Tech Review Basic', 'YouTube short review for mobile app or gadget', 'Quick review focused on utility, performance, and value for Pakistani audience.', 'YOUTUBE', 'Technology', 'ONE_TIME', 'PAID', null, 30000.00, 'PKR', '1 YouTube Short + 1 Instagram Story', 4, null, 1, 'https://images.example.com/packages/usman-tech-basic.jpg', array['https://images.example.com/packages/usman-tech-basic-1.jpg'], array['tech', 'review', 'islamabad'], true, false, now(), now()),
('a8fb7a90-4514-4df3-9d26-02f1e4d84006', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1003', 'Monthly Telecom Promo', 'Subscription package for telecom promos', 'Four short videos per month for data bundle, app usage, and network tips.', 'TIKTOK', 'Technology', 'SUBSCRIPTION', 'PAID', null, 120000.00, 'PKR', '4 TikToks/month + 4 stories/month + performance report', 7, 30, 2, 'https://images.example.com/packages/usman-telecom-sub.jpg', array['https://images.example.com/packages/usman-telecom-sub-1.jpg'], array['telecom', 'subscription', 'tech'], true, true, now(), now());

insert into package_tiers (id, package_id, name, price, deliverables, delivery_days, revisions, created_at) values
('bc3d71d8-d6e0-4af7-9a76-5b0099d71001', 'a8fb7a90-4514-4df3-9d26-02f1e4d84003', 'BASIC', 30000.00, '1 Reel + 2 Stories', 4, 1, now()),
('bc3d71d8-d6e0-4af7-9a76-5b0099d71002', 'a8fb7a90-4514-4df3-9d26-02f1e4d84003', 'STANDARD', 42000.00, '1 Reel + 5 Stories + swipe-up link', 5, 2, now()),
('bc3d71d8-d6e0-4af7-9a76-5b0099d71003', 'a8fb7a90-4514-4df3-9d26-02f1e4d84003', 'PREMIUM', 60000.00, '2 Reels + 6 Stories + pinned highlight', 7, 3, now()),
('bc3d71d8-d6e0-4af7-9a76-5b0099d71004', 'a8fb7a90-4514-4df3-9d26-02f1e4d84006', 'BASIC', 120000.00, '4 videos/month + 4 stories', 7, 2, now()),
('bc3d71d8-d6e0-4af7-9a76-5b0099d71005', 'a8fb7a90-4514-4df3-9d26-02f1e4d84006', 'PREMIUM', 175000.00, '8 videos/month + stories + monthly analytics report', 10, 3, now());

insert into conversations (id, creator_id, brand_id, read_by_creator, read_by_brand, last_message, created_at, updated_at) values
('76b6fca0-9a34-4356-bd34-1f51c47ac001', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1001', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1005', true, false, 'Can we schedule the reel shoot this Thursday in DHA?', now(), now()),
('76b6fca0-9a34-4356-bd34-1f51c47ac002', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1003', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1006', false, true, 'Please share your brand guidelines for the next bundle campaign.', now(), now());

insert into messages (id, conversation_id, sender_id, description, created_at, updated_at) values
('fcdf9843-38c5-4b12-a591-3798e0ac5001', '76b6fca0-9a34-4356-bd34-1f51c47ac001', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1005', 'We want a reel focused on value meals under PKR 999.', now(), now()),
('fcdf9843-38c5-4b12-a591-3798e0ac5002', '76b6fca0-9a34-4356-bd34-1f51c47ac001', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1001', 'Perfect, I can deliver concept draft by tomorrow evening.', now(), now()),
('fcdf9843-38c5-4b12-a591-3798e0ac5003', '76b6fca0-9a34-4356-bd34-1f51c47ac002', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1003', 'Please share your brand guidelines for the next bundle campaign.', now(), now());

insert into orders (id, package_id, creator_id, brand_id, image, title, price, completed, payment_intent, created_at, updated_at) values
('1b3a83c7-6d88-4e9f-84ae-27af3f0f6001', 'a8fb7a90-4514-4df3-9d26-02f1e4d84001', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1001', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1005', 'https://images.example.com/orders/order-1.jpg', 'Foodpanda Karachi Ramadan promo reel', 35000.00, true, 'pi_foodpanda_khi_0001', now() - interval '12 days', now() - interval '8 days'),
('1b3a83c7-6d88-4e9f-84ae-27af3f0f6002', 'a8fb7a90-4514-4df3-9d26-02f1e4d84006', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1003', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1006', 'https://images.example.com/orders/order-2.jpg', 'Jazz monthly data-bundle promo', 120000.00, false, 'pi_jazz_isb_0002', now() - interval '3 days', now() - interval '1 day');

insert into reviews (id, package_id, reviewer_id, star, description, created_at, updated_at) values
('e5acd17e-0df4-4d42-819d-6d4f0d947001', 'a8fb7a90-4514-4df3-9d26-02f1e4d84001', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1005', 5, 'Great energy, authentic delivery, and excellent engagement from Karachi audience.', now() - interval '7 days', now() - interval '7 days'),
('e5acd17e-0df4-4d42-819d-6d4f0d947002', 'a8fb7a90-4514-4df3-9d26-02f1e4d84003', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1004', 5, 'The campaign matched our youth banking segment perfectly.', now() - interval '15 days', now() - interval '15 days'),
('e5acd17e-0df4-4d42-819d-6d4f0d947003', 'a8fb7a90-4514-4df3-9d26-02f1e4d84006', '8c46d542-2f4d-4ab2-9bd8-39a3ceaf1006', 4, 'Strong technical storytelling and clear telecom messaging.', now() - interval '2 days', now() - interval '2 days');

