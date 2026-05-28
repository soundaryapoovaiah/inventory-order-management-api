## API Testing Screenshots

The APIs were tested using Postman, and database results were verified directly in PostgreSQL using SQL queries.

### Product Creation API

```http
POST /api/products
```

![Product Create API](docs/screenshots/product-create.png)

### Paginated Product API

```http
GET /api/products/paged?page=0&size=2&sortBy=productId&sortDirection=asc
```

![Product Pagination API](docs/screenshots/product-pagination.png)

### Customer Creation API

```http
POST /api/customers
```

![Customer Create API](docs/screenshots/customer-create.png)

### Order Placement API

```http
POST /api/orders
```

![Order Create API](docs/screenshots/order-create.png)

### Customer Order History API

```http
GET /api/orders/customer/{customerId}
```

![Order History API](docs/screenshots/order-history.png)

### Error Handling Response

```http
POST /api/orders
```

![Error Response](docs/screenshots/error-response.png)

### PostgreSQL Join Verification

![SQL Join Result](docs/screenshots/sql-join-result.png)
