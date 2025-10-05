-- Insert test users
-- Password for all users: password123 (BCrypt encoded)
INSERT INTO users (id, email, password, first_name, last_name, phone, role, is_enabled, created_at, updated_at) 
VALUES 
(1, 'user@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Test', 'User', '1234567890', 'USER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'admin@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', 'User', '0987654321', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'kumarsaimand@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Kumar', 'Saimand', '1112223333', 'USER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert test categories
INSERT INTO categories (id, name, slug, description, image_url, is_active, created_at, updated_at)
VALUES
(1, 'Men', 'men', 'Men''s Fashion', 'https://images.unsplash.com/photo-1490114538077-0a7f8cb49891', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Women', 'women', 'Women''s Fashion', 'https://images.unsplash.com/photo-1483985988355-763728e1935b', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Kids', 'kids', 'Kids Fashion', 'https://images.unsplash.com/photo-1503944583220-79d8926ad5e2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert test products
INSERT INTO products (id, name, slug, description, price, discount_price, category_id, image_url, stock_quantity, is_active, is_featured, created_at, updated_at)
VALUES
(1, 'Classic White T-Shirt', 'classic-white-tshirt', 'Premium cotton white t-shirt', 29.99, 24.99, 1, 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab', 100, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Blue Denim Jeans', 'blue-denim-jeans', 'Comfortable slim fit jeans', 59.99, 49.99, 1, 'https://images.unsplash.com/photo-1542272604-787c3835535d', 50, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Summer Dress', 'summer-dress', 'Floral print summer dress', 79.99, 69.99, 2, 'https://images.unsplash.com/photo-1515372039744-b8f02a3ae446', 30, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Kids Hoodie', 'kids-hoodie', 'Warm and cozy hoodie for kids', 39.99, 34.99, 3, 'https://images.unsplash.com/photo-1519238263530-99bdd11df2ea', 75, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
