DROP TRIGGER IF EXISTS trg_update_product_updated_at ON products;
DROP FUNCTION IF EXISTS update_product_updated_at();

DROP VIEW IF EXISTS customer_order_summary;

DROP FUNCTION IF EXISTS get_low_stock_products(INT);

-- V2 Migration: Advanced PostgreSQL Features
-- Adds trigger, view, and database function to demonstrate PostgreSQL-specific knowledge


-- 1. Automatically update updated_at column whenever a product row is updated
CREATE OR REPLACE FUNCTION update_product_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_update_product_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_product_updated_at();


-- 2. View for customer order summary
CREATE OR REPLACE VIEW customer_order_summary AS
SELECT
    c.customer_id,
    c.name AS customer_name,
    c.email,
    COUNT(o.order_id) AS total_orders,
    COALESCE(SUM(o.total_amount), 0) AS total_spent,
    MAX(o.created_at) AS last_order_date
FROM customers c
         LEFT JOIN orders o ON c.customer_id = o.customer_id
GROUP BY c.customer_id, c.name, c.email;


-- 3. PostgreSQL function for low-stock reporting
CREATE OR REPLACE FUNCTION get_low_stock_products(threshold_quantity INT)
RETURNS TABLE (
    product_id BIGINT,
    product_name VARCHAR,
    category VARCHAR,
    stock_quantity INT
)
AS $$
BEGIN
RETURN QUERY
SELECT
    p.product_id,
    p.name,
    p.category,
    p.stock_quantity
FROM products p
WHERE p.stock_quantity < threshold_quantity
ORDER BY p.stock_quantity ASC;
END;
$$ LANGUAGE plpgsql;