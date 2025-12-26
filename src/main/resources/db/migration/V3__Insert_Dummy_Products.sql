-- =====================================================
-- PASETO API - Dummy Data: Products
-- Version: V4__Insert_Dummy_Products
-- Description: Insert 20 dummy product records
-- =====================================================

INSERT INTO products (name, description, price, stock, image_url, sku, active, created_at, updated_at) VALUES
('Wireless Bluetooth Headphones', 'Premium noise-cancelling headphones with 30-hour battery life', 149.99, 150, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500', 'AUD-BT-001', true, NOW(), NOW()),
('Smart Watch Pro', 'Fitness tracker with heart rate monitor and GPS', 299.99, 75, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500', 'WATCH-SM-002', true, NOW(), NOW()),
('Portable Power Bank 20000mAh', 'Fast charging power bank with dual USB ports', 39.99, 200, 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=500', 'PWR-20000-003', true, NOW(), NOW()),
('Mechanical Gaming Keyboard', 'RGB backlit mechanical keyboard with blue switches', 89.99, 50, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500', 'KB-MECH-004', true, NOW(), NOW()),
('Wireless Gaming Mouse', '16000 DPI optical sensor with programmable buttons', 59.99, 120, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=500', 'MOUSE-GM-005', true, NOW(), NOW()),
('USB-C Hub 7-in-1', 'Multiport adapter with HDMI, USB 3.0, SD card reader', 34.99, 180, 'https://images.unsplash.com/photo-1625723044792-44de16ccb4e9?w=500', 'HUB-USB7-006', true, NOW(), NOW()),
('Laptop Stand Aluminum', 'Ergonomic adjustable laptop stand for better posture', 49.99, 90, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=500', 'STAND-LAP-007', true, NOW(), NOW()),
('Webcam 1080p HD', 'Full HD webcam with auto focus and noise reduction', 69.99, 65, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500', 'CAM-1080-008', true, NOW(), NOW()),
('Desk Lamp LED', 'Adjustable brightness desk lamp with USB charging port', 29.99, 140, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=500', 'LAMP-LED-009', true, NOW(), NOW()),
('Wireless Charger Pad', '10W fast wireless charging for all Qi-enabled devices', 24.99, 220, 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=500', 'CHG-WL-010', true, NOW(), NOW()),
('Bluetooth Speaker Waterproof', 'Portable waterproof speaker with 12-hour playtime', 45.99, 85, 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=500', 'SPK-BT-011', true, NOW(), NOW()),
('Laptop Sleeve 15.6 inch', 'Padded neoprene sleeve with external pocket', 19.99, 300, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=500', 'SLV-LAP-012', true, NOW(), NOW()),
('Monitor Light Bar', 'Screen bar with adjustable color temperature', 39.99, 95, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=500', 'LIGHT-MON-013', true, NOW(), NOW()),
('USB-C Charging Cable 6ft', 'Braided nylon fast charging cable', 12.99, 400, 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=500', 'CBL-USB6-014', true, NOW(), NOW()),
('Wireless Earbuds True Wireless', 'Touch control earbuds with charging case', 79.99, 110, 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=500', 'EARBUD-TW-015', true, NOW(), NOW()),
('Graphics Tablet Drawing', '8192 levels of pressure sensitivity', 129.99, 40, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=500', 'TAB-DRAW-016', true, NOW(), NOW()),
('External SSD 1TB', 'Ultra-fast portable SSD with USB 3.2 Gen 2', 119.99, 70, 'https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=500', 'SSD-1TB-017', true, NOW(), NOW()),
('Network Cable Cat6 25ft', 'High-speed internet cable with gold-plated connectors', 14.99, 250, 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=500', 'NET-CAT6-018', true, NOW(), NOW()),
('Ring Light 10 inch', 'LED ring light for photography and video calls', 34.99, 130, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=500', 'LIGHT-RNG-019', true, NOW(), NOW()),
('Webcam Cover Slide', 'Privacy cover for laptop and tablet cameras', 8.99, 500, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500', 'COV-CAM-020', true, NOW(), NOW())
ON CONFLICT DO NOTHING;
