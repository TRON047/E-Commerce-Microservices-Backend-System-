# E-Commerce Microservices

---

## 1. Elevator Pitch

This is a **backend system for an online shopping platform** built using a microservices architecture. It handles the core shopping flow — managing product inventory, processing customer orders, and sending email notifications — with each responsibility split into its own independently deployable service.

---

## 2. Architecture & Tech Stack

### Why these technologies?

| Technology | Why we chose it |
|---|---|
| **Spring Boot** | Industry standard for building Java REST APIs fast with minimal boilerplate |
| **Spring Cloud / Eureka** | Services need to find each other dynamically — Eureka acts as a phone directory |
| **API Gateway** | One entry point for all traffic instead of clients knowing every service's port |
| **Kafka** | Order and Notification services shouldn't be tightly coupled — Kafka lets them communicate asynchronously |
| **PostgreSQL** | Products have fixed, structured fields (name, price, stock) — relational DB fits perfectly |
| **MongoDB** | Orders are flexible documents with nested items — NoSQL document model is a natural fit |
| **Docker** | Runs all 5 services + 3 databases on any machine with one command |

### How services talk to each other

```
Client → API Gateway → Product Service → PostgreSQL
                     → Order Service  → MongoDB
                                      → Kafka → Notification Service → Email
```

- **Synchronous** (HTTP): API Gateway → Order Service → Product Service (to check stock)
- **Asynchronous** (Kafka): Order Service publishes an event → Notification Service consumes it independently

---

## 3. Core Features

- **Product CRUD** — Full create, read, update, delete for products with validation. Each product has a unique SKU, price, and stock quantity stored in PostgreSQL.

- **Atomic stock reservation** — When an order is placed, the system runs a single SQL UPDATE with a `WHERE stock >= quantity` condition. This prevents two customers from simultaneously buying the last item in stock.

- **Order placement flow** — Order Service calls Product Service via HTTP to fetch price and reserve stock, then saves the order to MongoDB, then fires a Kafka event — all in one request.

- **Kafka event pipeline** — When an order is confirmed, Order Service publishes an `OrderPlacedEvent` to a Kafka topic. Notification Service listens on that topic and sends a confirmation email. If Notification Service is down, Kafka queues the events and replays them when it restarts.

- **Fake email with real hook** — Notification Service logs a formatted email to console by default. Flipping one config flag (`notification.mail.enabled=true`) switches it to real SMTP with zero code changes.

- **Service discovery** — Every service registers with Eureka on startup. The API Gateway uses `lb://service-name` URIs so it discovers service addresses dynamically instead of hardcoding ports.

- **Circuit breaker fallbacks** — If Product or Order Service is down, the Gateway returns a clean JSON error message instead of crashing or hanging.

---

## 4. Your Direct Contributions

Claim full ownership of these — you made the design decisions:

- **Product Service end-to-end** — the `Product` JPA entity, repository with the atomic `decreaseStock()` query, service layer with SKU duplicate validation, and REST controller with proper HTTP status codes

- **`OrderPlacedEvent` record** — designed the Kafka message contract that connects Order and Notification services. You decided what data both sides need

- **Order placement flow in `OrderService.java`** — the multi-step logic: fetch product details via WebClient → reserve stock → save to MongoDB → publish Kafka event, with proper rollback thinking if any step fails

- **`KafkaConsumerConfig.java`** — manually configured the consumer with `ErrorHandlingDeserializer` to prevent a bad message from crashing the consumer thread (this is a real production concern most tutorials miss)

- **`docker-compose.yml`** — wired up all 5 services with healthchecks, environment variable injection, and correct startup ordering (Postgres healthcheck before Product Service starts)

- **RFC 7807 error handling** — implemented `GlobalExceptionHandler` in both Product and Order services that returns structured `ProblemDetail` responses instead of raw stack traces

---

## 5. Challenges & Solutions

### Challenge 1 — Overselling under concurrent orders

**The problem:** During testing, we simulated two users simultaneously ordering the last unit of a product. Both requests read `stockQuantity = 1`, both passed the stock check, and both succeeded — leaving stock at -1.

**The solution:** Removed the application-level stock check entirely and replaced it with a single atomic SQL UPDATE:
```sql
UPDATE products
SET stock_quantity = stock_quantity - :quantity
WHERE id = :id AND stock_quantity >= :quantity
```
The database executes this as one atomic operation. If two transactions try simultaneously, only one succeeds — the other updates 0 rows, which we detect and return a "Insufficient stock" response. No distributed lock needed.

---

### Challenge 2 — Notification Service crashing on a malformed Kafka message

**The problem:** During testing, a manually published Kafka message had a typo in the JSON structure. The `JsonDeserializer` threw an exception, the consumer thread crashed, and the service stopped processing all future messages — including valid ones.

**The solution:** Wrapped the `JsonDeserializer` in Spring Kafka's `ErrorHandlingDeserializer`. Now when a poison-pill message arrives, the error is logged and the message is skipped, but the consumer thread keeps running and processes all subsequent valid messages normally.

