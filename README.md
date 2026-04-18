# E-Commerce Microservices

A production-grade microservices system built with Java 17, Spring Boot 3, Spring Cloud, Apache Kafka, PostgreSQL, and MongoDB.

## Architecture

```
Client
  │
  ▼
API Gateway (:8080)          ◄──── Eureka Discovery (:8761)
  │                                      ▲
  ├── /api/products/** ──► Product Service (:8081) ──► PostgreSQL
  │
  └── /api/orders/**  ──► Order Service (:8082)   ──► MongoDB
                              │
                              │ publishes "order-placed"
                              ▼
                           Kafka
                              │
                              │ consumes "order-placed"
                              ▼
                     Notification Service (:8083)
                              │
                              ▼
                         Email (logged / real SMTP)
```

### Service Responsibilities

| Service              | Port  | DB         | Role                                                    |
|----------------------|-------|------------|---------------------------------------------------------|
| Discovery Server     | 8761  | —          | Eureka registry — all services register here            |
| API Gateway          | 8080  | —          | Single entry point; routes, circuit-breaks, CORS        |
| Product Service      | 8081  | PostgreSQL | Inventory CRUD; atomic stock reservation                |
| Order Service        | 8082  | MongoDB    | Place/cancel orders; publish Kafka events               |
| Notification Service | 8083  | —          | Consume Kafka events; send confirmation emails          |

---

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose

---

## Quick Start

```bash
# 1. Clone and build all JARs
git clone <repo-url>
cd ecommerce-microservices
mvn clean package -DskipTests

# 2. Start everything (infrastructure + all 5 services)
docker compose up --build

# 3. Check Eureka dashboard — all services should appear within ~30s
open http://localhost:8761
```

---

## API Reference

All requests go through the **API Gateway at http://localhost:8080**.

### Products

```bash
# Create a product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Pro",
    "description": "High-performance laptop",
    "price": 1299.99,
    "stockQuantity": 50,
    "sku": "LAP-PRO-001"
  }'

# List all products
curl http://localhost:8080/api/products

# Get product by ID
curl http://localhost:8080/api/products/1

# Update product
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{ "name": "Laptop Pro v2", "price": 1199.99, "stockQuantity": 45, "sku": "LAP-PRO-001" }'

# Delete product
curl -X DELETE http://localhost:8080/api/products/1
```

### Orders

```bash
# Place an order (triggers stock reservation + Kafka event + fake email)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerEmail": "alice@example.com",
    "customerName": "Alice Smith",
    "items": [
      { "productId": 1, "quantity": 2 },
      { "productId": 2, "quantity": 1 }
    ]
  }'

# Get order by ID
curl http://localhost:8080/api/orders/<order-id>

# Get all orders by customer email
curl "http://localhost:8080/api/orders?email=alice@example.com"

# Cancel an order
curl -X PATCH http://localhost:8080/api/orders/<order-id>/cancel
```

---

## Key Design Decisions

### Atomic stock reservation
`ProductRepository.decreaseStock()` uses a single SQL UPDATE:
```sql
UPDATE products
SET stock_quantity = stock_quantity - :quantity
WHERE id = :id AND stock_quantity >= :quantity
```
The `WHERE stock_quantity >= :quantity` guard means two concurrent orders
can never both succeed when only one unit remains. No distributed lock needed.

### Why Kafka over direct HTTP for notifications?
- **Decoupling**: Order Service doesn't know Notification Service exists.
- **Durability**: If Notification Service restarts, events queue up and replay.
- **Scalability**: Add SMS, analytics, or warehouse consumers with zero changes to Order Service.

### Why MongoDB for orders?
Orders are write-heavy, schema-flexible, and self-contained. A single document
holds the order plus all its items — no JOINs, fast reads.

### Circuit breakers in the Gateway
If a service is down, the Gateway returns a graceful JSON error from the
`/fallback/*` endpoints instead of a raw 503. Add Resilience4j for retry
and rate-limiting on top.

---

## Sending Real Emails

In `notification-service/src/main/resources/application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your@gmail.com
    password: your-app-password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

notification:
  mail:
    enabled: true   # ← flip this
    from: orders@yourdomain.com
```

---

## Running Tests

```bash
# Unit tests only (no Docker needed)
mvn test

# Integration tests (uses embedded Kafka broker)
mvn verify

# Single service
cd notification-service && mvn test
```

---

## What to Add Next

| Feature                | How                                              |
|------------------------|--------------------------------------------------|
| Security               | Spring Security + JWT at the Gateway             |
| Distributed tracing    | Micrometer + Zipkin (add `spring-boot-starter-actuator` tracing) |
| Config server          | Spring Cloud Config for centralised YAML         |
| API versioning         | Path prefix `/api/v1/` in Gateway routes         |
| Outbox pattern         | Persist events to DB before Kafka; poll & publish (guarantees at-least-once delivery) |
| Schema Registry        | Confluent Schema Registry + Avro instead of JSON |
| Rate limiting          | Gateway `RequestRateLimiter` filter + Redis       |
