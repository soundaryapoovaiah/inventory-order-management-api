# Inventory and Order Management API

A backend REST API built with Java, Spring Boot, PostgreSQL, Spring Data JPA, Flyway, and Docker.
The project manages products, customers, inventory stock, and customer orders with transactional stock updates.

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

## Project Overview

This project simulates a small inventory and order management backend system.

It supports:

- Product management
- Customer management
- Multi-item order placement
- Transactional inventory updates
- Stock validation
- Low-stock reporting
- Pagination
- PostgreSQL schema design with indexes and foreign keys
- Clean API error handling

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
Spring Data JPA Repositories Layer
      |
      v
PostgreSQL Database running in Docker
```