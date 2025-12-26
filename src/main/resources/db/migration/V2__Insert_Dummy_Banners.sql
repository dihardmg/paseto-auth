-- =====================================================
-- PASETO API - Dummy Data: Banners
-- Version: V3__Insert_Dummy_Banners
-- Description: Insert 20 dummy banner records
-- =====================================================

INSERT INTO banners (title, description, image_url, link_url, display_order, active, created_at, updated_at) VALUES
('Summer Sale 2024', 'Get up to 50% off on all summer items', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200', 'https://example.com/summer-sale', 1, true, NOW(), NOW()),
('New Arrivals', 'Check out our latest collection', 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1200', 'https://example.com/new-arrivals', 2, true, NOW(), NOW()),
('Flash Sale', 'Limited time offer - 24 hours only!', 'https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=1200', 'https://example.com/flash-sale', 3, true, NOW(), NOW()),
('Free Shipping', 'Free shipping on orders over $50', 'https://images.unsplash.com/photo-1556740758-90de374c12ad?w=1200', 'https://example.com/free-shipping', 4, true, NOW(), NOW()),
('Member Exclusive', 'Exclusive deals for members only', 'https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=1200', 'https://example.com/member-exclusive', 5, true, NOW(), NOW()),
('Weekend Special', 'Special discounts every weekend', 'https://images.unsplash.com/photo-1512909006721-3d6018887383?w=1200', 'https://example.com/weekend-special', 6, true, NOW(), NOW()),
('Clearance Sale', 'Last chance to grab these deals', 'https://images.unsplash.com/photo-1601924994987-69e26d50dc26?w=1200', 'https://example.com/clearance', 7, true, NOW(), NOW()),
('Tech Deals', 'Best deals on electronics', 'https://images.unsplash.com/photo-1498049394581-545c03869a1f?w=1200', 'https://example.com/tech-deals', 8, true, NOW(), NOW()),
('Fashion Week', 'New fashion collection available', 'https://images.unsplash.com/photo-1445205170230-053b83016050?w=1200', 'https://example.com/fashion-week', 9, true, NOW(), NOW()),
('Home & Living', 'Transform your home', 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=1200', 'https://example.com/home-living', 10, true, NOW(), NOW()),
('Sports Gear', 'Get ready for the game', 'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=1200', 'https://example.com/sports-gear', 11, true, NOW(), NOW()),
('Beauty Products', 'Discover your beauty routine', 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=1200', 'https://example.com/beauty', 12, true, NOW(), NOW()),
('Kids Corner', 'Fun stuff for kids', 'https://images.unsplash.com/photo-1503454537195-1dcabb73ffb9?w=1200', 'https://example.com/kids-corner', 13, true, NOW(), NOW()),
('Book Lovers', 'Explore our book collection', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=1200', 'https://example.com/books', 14, true, NOW(), NOW()),
('Fitness First', 'Achieve your fitness goals', 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=1200', 'https://example.com/fitness', 15, true, NOW(), NOW()),
('Garden Paradise', 'Everything for your garden', 'https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?w=1200', 'https://example.com/garden', 16, true, NOW(), NOW()),
('Pet Supplies', 'Your pets will love these', 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=1200', 'https://example.com/pet-supplies', 17, true, NOW(), NOW()),
('Auto Accessories', 'Upgrade your ride', 'https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?w=1200', 'https://example.com/auto', 18, true, NOW(), NOW()),
('Food & Beverages', 'Delicious treats await', 'https://images.unsplash.com/photo-1484723091739-30a097e8f929?w=1200', 'https://example.com/food', 19, true, NOW(), NOW()),
('Gift Ideas', 'Perfect gifts for everyone', 'https://images.unsplash.com/photo-1549465220-1a8b9238cd48?w=1200', 'https://example.com/gifts', 20, true, NOW(), NOW())
ON CONFLICT DO NOTHING;
