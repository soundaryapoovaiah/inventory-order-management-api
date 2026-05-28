# Inventory and Order Management API

A backend REST API built with Java, Spring Boot, PostgreSQL, Spring Data JPA, Flyway, and Docker.

The project manages products, customers, inventory stock, and customer orders with transactional stock updates. It demonstrates real-world backend development concepts such as REST API design, PostgreSQL schema modeling, database migrations, transaction management, pagination, sorting, error handling, and SQL verification.

---

## Tech Stack

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway Migration
- Docker Compose
- Hibernate Validator
- Lombok
- Postman
- Maven

---

## Project Overview

This project simulates a small inventory and order management backend system.

It supports:

- Product management
- Customer management
- Multi-item order placement
- Transactional inventory updates
- Stock validation
- Low-stock reporting
- Pagination and sorting
- PostgreSQL schema design with indexes and foreign keys
- Clean API error handling
- SQL-based verification of database relationships

---

## Architecture

```text
Client / Postman
      |
      v
Spring Boot REST Controllers
      |
      v
Service Layer
      |
      v
Spring Data JPA Repository Layer
      |
      v
PostgreSQL Database running in Docker
```

---

## Database Tables

The database contains the following tables:

```text
products
customers
orders
order_items
flyway_schema_history
```

---

## Entity Relationships

```text
One customer can have many orders.
One order can have many order items.
One order item belongs to one product.
```

Relationship flow:

```text
customers.customer_id
        ↓
orders.customer_id
        ↓
order_items.order_id
        ↓
products.product_id
```

---

## Features

### Product APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/products` | Create product |
| GET | `/api/products` | Get all products |
| GET | `/api/products/{productId}` | Get product by ID |
| PUT | `/api/products/{productId}` | Update product |
| DELETE | `/api/products/{productId}` | Delete product |
| GET | `/api/products/category/{category}` | Get products by category |
| GET | `/api/products/search?name=mouse` | Search products by name |
| GET | `/api/products/low-stock?threshold=10` | Get low-stock products |
| GET | `/api/products/paged?page=0&size=5&sortBy=productId&sortDirection=asc` | Get paginated products |

### Customer APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/customers` | Create customer |
| GET | `/api/customers` | Get all customers |
| GET | `/api/customers/{customerId}` | Get customer by ID |
| GET | `/api/customers/email?email=john@example.com` | Get customer by email |
| PUT | `/api/customers/{customerId}` | Update customer |
| DELETE | `/api/customers/{customerId}` | Delete customer |

### Order APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/orders` | Place order |
| GET | `/api/orders/{orderId}` | Get order by ID |
| GET | `/api/orders/customer/{customerId}` | Get all orders for a customer |

---

## Core Business Logic

The order placement API performs the following operations inside a transaction:

```text
1. Validate customer exists
2. Validate each product exists
3. Check available stock
4. Reduce product stock
5. Calculate line item totals
6. Calculate total order amount
7. Save order and order items
8. Roll back if any step fails
```

This prevents invalid orders and protects inventory consistency.

---

## Example Product Request

```json
{
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse with USB receiver",
  "category": "Electronics",
  "price": 25.99,
  "stockQuantity": 50
}
```

---

## Example Customer Request

```json
{
  "name": "John Smith",
  "email": "john@example.com",
  "phone": "5131112222",
  "address": "Cincinnati, OH"
}
```

---

## Example Order Request

```json
{
  "customerId": 1,
  "items": [
    {
      "productId": 2,
      "quantity": 2
    },
    {
      "productId": 3,
      "quantity": 1
    }
  ]
}
```

---

## Example Error Response

```json
{
  "status": 400,
  "message": "Product not found with id: 1",
  "path": "/api/orders",
  "timestamp": "2026-05-28T15:05:30"
}
```

---

## API Testing Screenshots

The APIs were tested using Postman, and database results were verified directly in PostgreSQL using SQL queries.

### Product Creation API

```http
POST /api/products
```

![Product Create API](docs/screenshots/product-create.png)

### Get Products API

```http
GET /api/products
```

![Get Products API](docs/screenshots/product-get-all.png)

### Get Product by ID API

```http
GET /api/products/{productId}
```

![Get Product By ID API](docs/screenshots/product-get-by-id.png)

### Product Search by Category API

```http
GET /api/products/category/{category}
```

![Product Category Search API](docs/screenshots/product-category-search.png)

### Product Search by Name API

```http
GET /api/products/search?name=mouse
```

![Product Name Search API](docs/screenshots/product-name-search.png)

### Low Stock Products API

```http
GET /api/products/low-stock?threshold=10
```

![Low Stock API](docs/screenshots/product-low-stock.png)

### Product Update API

```http
PUT /api/products/{productId}
```

![Product Update API](docs/screenshots/product-update.png)

### Customer Creation API

```http
POST /api/customers
```

![Customer Create API](docs/screenshots/customer-create.png)

### Customer Update API

```http
PUT /api/customers/{customerId}
```

![Customer Update API](docs/screenshots/customer-update.png)

### Order Placement API

```http
POST /api/orders
```

This API validates the customer, checks product availability, reduces stock, calculates total amount, and saves order items inside a transaction.

![Order Create API](docs/screenshots/order-create.png)

### Customer Order History API

```http
GET /api/orders/customer/{customerId}
```

![Order History API](docs/screenshots/order-history.png)

### Error Handling Response

The application returns clean JSON error responses for invalid requests.

```http
POST /api/orders
```

![Error Response](docs/screenshots/error-response.png)

### Paginated Product API

```http
GET /api/products/paged?page=0&size=2&sortBy=productId&sortDirection=asc
```

![Product Pagination API](docs/screenshots/product-pagination.png)

### PostgreSQL Join Verification

The order, customer, product, and order item relationships were verified using a SQL join query.

![SQL Join Result](docs/screenshots/sql-join-result.png)

---

## Full API Testing Documentation

Detailed API testing screenshots and PostgreSQL verification steps are available in the project documentation.

[View API Testing Documentation](docs/PostgreSQL_API_Testing_Documentation.docx)

---

## PostgreSQL Schema Highlights

The project uses:

- Primary keys
- Foreign keys
- Unique constraints
- Check constraints
- Indexes
- Join queries
- Transactional updates

Indexes created:

```sql
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
```

---

## Important SQL Join Query

```sql
SELECT 
    o.order_id,
    c.name AS customer_name,
    p.name AS product_name,
    oi.quantity,
    oi.unit_price,
    oi.quantity * oi.unit_price AS line_total,
    o.total_amount,
    o.order_status,
    o.created_at
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
JOIN order_items oi ON o.order_id = oi.order_id
JOIN products p ON oi.product_id = p.product_id
ORDER BY o.created_at DESC;
```

---

## Running the Project Locally

### 1. Clone the repository

```bash
git clone https://github.com/soundaryapoovaiah/inventory-order-management-api.git
cd inventory-order-management-api
```

### 2. Start PostgreSQL using Docker

```bash
docker compose up -d
```

### 3. Verify PostgreSQL container

```bash
docker ps
```

### 4. Run Spring Boot application

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```bash
.\mvnw spring-boot:run
```

### 5. Application URL

```text
http://localhost:8080
```

---

## PostgreSQL Connection Details

```text
Database: inventory_db
Username: inventory_user
Password: inventory_pass
Port: 5432
```

---

## Flyway Migration

Database tables are created using Flyway migration.

Migration file:

```text
src/main/resources/db/migration/V1__inventory_schema.sql
```

---

## Project Structure

```text
src/main/java/microservices/postgresql
├── controller
├── dto
├── entity
├── exception
├── repository
├── service
└── PostgreSqlApplication.java

src/main/resources
├── db/migration/V1__inventory_schema.sql
└── application.properties

docs
├── screenshots
└── PostgreSQL_API_Testing_Documentation.docx
```

---

## Key Learning Outcomes

This project demonstrates:

- REST API development using Spring Boot
- PostgreSQL database design
- Transaction management using `@Transactional`
- Spring Data JPA repository methods
- Docker-based local database setup
- Flyway database migration
- Pagination and sorting
- Global exception handling
- Real-world backend service layering
- SQL join verification across normalized relational tables

---
### Concurrency Test Result

## Concurrency Handling

Order placement uses transaction-level row locking to prevent overselling during concurrent purchases.

The `ProductRepository` uses pessimistic locking:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.productId = :productId")
Optional<Product> findByIdForUpdate(@Param("productId") Long productId);
```

During order placement, each product row is locked before stock validation and inventory deduction. This prevents two concurrent transactions from reading the same stock quantity and overselling the product.

The order service also sorts product IDs before acquiring locks to reduce deadlock risk when an order contains multiple products.

### Concurrency Test Result

To validate concurrency-safe stock updates, product ID `2` was updated to stock quantity `1`. Two order requests were submitted at nearly the same time for the same product.

Result:

```text
Request 1: HTTP_STATUS:201
Request 2: HTTP_STATUS:400
Final stock quantity: 0
```

This confirms that pessimistic row-level locking prevents two concurrent transactions from overselling the same product.

### Concurrency Test Evidence

PowerShell concurrency test showing one successful request and one failed request:

![Concurrency Terminal Result](docs/screenshots/concurrency-terminal.png)

Final product stock after concurrent requests:

![Concurrency Final Stock](docs/screenshots/concurrency-final-stock.png)

Customer order history showing only the successful order:

![Concurrency Order History](docs/screenshots/concurrency-order-history.png)


## Advanced PostgreSQL Features

This project includes a second Flyway migration to demonstrate PostgreSQL-specific database features beyond basic CRUD operations.

Migration file:

```text
src/main/resources/db/migration/V2__postgresql_advanced_features.sql
```

The V2 migration adds:

- PostgreSQL trigger
- PL/pgSQL function
- Database view
- Automatic `updated_at` timestamp handling
- Customer order summary reporting
- Low-stock product reporting through a database function

### Flyway Migration History

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Expected result:

```text
1 | inventory schema               | true
2 | postgresql advanced features   | true
```

### Customer Order Summary View

The `customer_order_summary` view summarizes total orders, total spending, and last order date per customer.

```sql
SELECT * FROM customer_order_summary;
```

### Low-Stock PostgreSQL Function

The `get_low_stock_products()` function returns products below a given stock threshold.

```sql
SELECT * FROM get_low_stock_products(50);
```

### V2 Migration Verification

![PostgreSQL V2 Migration Verification](docs/screenshots/postgresql-v2-migration.png)
