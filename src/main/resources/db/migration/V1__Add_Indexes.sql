-- =====================================================
-- PASETO API - Database Indexes for Query Optimization
-- Version: V2__Add_Indexes
-- Description: Add indexes to improve query performance
-- =====================================================

-- =====================================================
-- USERS TABLE INDEXES
-- =====================================================

-- Index for username lookup (login)
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Index for email lookup (registration)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Composite index for active users with created_at
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at DESC);

-- =====================================================
-- REFRESH_TOKENS TABLE INDEXES
-- =====================================================

-- Index for user_id lookup (get user tokens)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Index for token_id lookup (token validation)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_id ON refresh_tokens(token_id);

-- Composite index for active tokens (revoke all user tokens)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked ON refresh_tokens(user_id, revoked);

-- Composite index for token validation (active + not expired)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active ON refresh_tokens(revoked, expired, expires_at);

-- Index for cleanup expired tokens
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Index for device tracking
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_device_ip ON refresh_tokens(device_info, ip_address);

-- =====================================================
-- PRODUCTS TABLE INDEXES
-- =====================================================

-- Index for product name search (search by name)
CREATE INDEX IF NOT EXISTS idx_products_name ON products USING gin(to_tsvector('english', name));

-- Index for active products
CREATE INDEX IF NOT EXISTS idx_products_active ON products(active);

-- Index for SKU lookup
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);

-- Composite index for active products with created_at (listing)
CREATE INDEX IF NOT EXISTS idx_products_active_created ON products(active, created_at DESC);

-- Index for price range queries
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);

-- Index for stock queries
CREATE INDEX IF NOT EXISTS idx_products_stock ON products(stock);

-- Composite index for active products in stock
CREATE INDEX IF NOT EXISTS idx_products_active_stock ON products(active, stock);

-- =====================================================
-- BANNERS TABLE INDEXES
-- =====================================================

-- Index for active banners
CREATE INDEX IF NOT EXISTS idx_banners_active ON banners(active);

-- Index for display order
CREATE INDEX IF NOT EXISTS idx_banners_display_order ON banners(display_order);

-- Composite index for active banners ordered by display order (listing)
CREATE INDEX IF NOT EXISTS idx_banners_active_order ON banners(active, display_order ASC);

-- =====================================================
-- PERFORMANCE ANALYSIS
-- =====================================================

-- Analyze tables to update statistics
ANALYZE users;
ANALYZE refresh_tokens;
ANALYZE products;
ANALYZE banners;

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON INDEX idx_users_username IS 'Index for fast username lookup during login';
COMMENT ON INDEX idx_refresh_tokens_active IS 'Composite index for active token validation';
COMMENT ON INDEX idx_products_name IS 'Full-text search index for product names';
COMMENT ON INDEX idx_products_active_created IS 'Index for active products listing ordered by creation date';
COMMENT ON INDEX idx_banners_active_order IS 'Index for active banners ordered by display priority';
