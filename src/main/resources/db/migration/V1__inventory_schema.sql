CREATE TABLE products (
                          product_id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(100) NOT NULL,
                          description TEXT,
                          category VARCHAR(50) NOT NULL,
                          price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
                          stock_quantity INT NOT NULL CHECK (stock_quantity >= 0),
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customers (
                           customer_id BIGSERIAL PRIMARY KEY,
                           name VARCHAR(100) NOT NULL,
                           email VARCHAR(150) UNIQUE NOT NULL,
                           phone VARCHAR(20),
                           address TEXT,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
                        order_id BIGSERIAL PRIMARY KEY,
                        customer_id BIGINT NOT NULL,
                        order_status VARCHAR(30) NOT NULL DEFAULT 'PLACED',
                        total_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_orders_customer
                            FOREIGN KEY (customer_id)
                                REFERENCES customers(customer_id)
);

CREATE TABLE order_items (
                             order_item_id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INT NOT NULL CHECK (quantity > 0),
                             unit_price NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),

                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id)
                                     REFERENCES orders(order_id),

                             CONSTRAINT fk_order_items_product
                                 FOREIGN KEY (product_id)
                                     REFERENCES products(product_id)
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);