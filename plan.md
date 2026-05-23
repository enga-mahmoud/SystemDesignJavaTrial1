# E-Commerce Platform — Production-Grade System Design
## Java Spring Boot Microservices + Angular

---

## Table of Contents

1. [Problem Statement & Goals](#1-problem-statement--goals)
2. [Non-Functional Requirements](#2-non-functional-requirements)
3. [High-Level Architecture](#3-high-level-architecture)
4. [Service Inventory](#4-service-inventory)
5. [Infrastructure Components](#5-infrastructure-components)
6. [Per-Service Deep Dives](#6-per-service-deep-dives)
   - 6.1 API Gateway
   - 6.2 Service Registry
   - 6.3 Config Server
   - 6.4 User Service
   - 6.5 Product Service
   - 6.6 Inventory Service
   - 6.7 Order Service (CQRS + Event Sourcing + Saga)
   - 6.8 Payment Service
   - 6.9 Notification Service
   - 6.10 Search Service
   - 6.11 Angular Frontend
7. [Data Models](#7-data-models)
8. [Kafka Topics & Event Contracts](#8-kafka-topics--event-contracts)
9. [Redis Key Patterns](#9-redis-key-patterns)
10. [Sequence Diagrams](#10-sequence-diagrams)
11. [Design Pattern Reference](#11-design-pattern-reference)
12. [Technology Decision Rationale](#12-technology-decision-rationale)
13. [System Design Interview Talking Points](#13-system-design-interview-talking-points)
14. [Complete File/Directory Structure](#14-complete-filedirectory-structure)
15. [Implementation Milestones](#15-implementation-milestones)
16. [Verification & Testing Plan](#16-verification--testing-plan)

---

## 1. Problem Statement & Goals

Build a scalable, fault-tolerant e-commerce platform that supports:
- Product catalog management and full-text search
- User registration, authentication, and authorization
- Shopping cart and order placement
- Inventory management with concurrency safety
- Payment processing with guaranteed-once semantics
- Real-time order status notifications

The platform is intentionally designed to demonstrate **every major distributed systems pattern** used in production. It is a learning vehicle: every architectural decision has a reason, and every reason teaches a concept.

---

## 2. Non-Functional Requirements

| Requirement        | Target                                              |
|--------------------|-----------------------------------------------------|
| Availability       | 99.9% per service (independent failure domains)     |
| Read latency       | p99 < 200ms for product catalog and search          |
| Write latency      | p99 < 500ms for order placement (end-to-end saga)   |
| Throughput         | 10,000 RPS peak on product search endpoint          |
| Consistency        | Strong: order state, payment; Eventual: search index|
| Fault isolation    | Single service failure MUST NOT cascade             |
| Observability      | Distributed tracing, metrics, structured logging     |
| Security           | JWT RS256, refresh token rotation, rate limiting, gateway identity header isolation |
| Scalability        | Every service must be horizontally scalable         |

---

## 3. High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                     │
│                                                                               │
│         ┌─────────────────────────────────────────────────────┐              │
│         │         Angular 17+ SPA  (Port 4200)                │              │
│         │   NgRx Store │ Angular Material │ HTTP Interceptors  │              │
│         └───────────────────────┬─────────────────────────────┘              │
└───────────────────────────────────────────────────────────────────────────────┘
                                  │ HTTPS
┌─────────────────────────────────▼─────────────────────────────────────────────┐
│                           EDGE / GATEWAY LAYER                                 │
│                                                                                │
│     ┌──────────────────────────────────────────────────────────┐              │
│     │                 API Gateway  (Port 8080)                  │              │
│     │   Spring Cloud Gateway (Reactive / Netty)                 │              │
│     │   ├── JwtAuthFilter      (RS256 token validation)         │              │
│     │   ├── RateLimitFilter    (Redis sliding-window, 100/min)  │              │
│     │   ├── CorrelationIdFilter(inject X-Correlation-Id header) │              │
│     │   └── CircuitBreaker     (Resilience4j per route)         │              │
│     └───────────────────────────┬────────────────────────────── ┘              │
└───────────────────────────────────────────────────────────────────────────────┘
                                  │
         ┌────────────────────────┼──────────────────────────────────┐
         │                        │                                   │
┌────────▼─────────┐  ┌──────────▼──────────┐  ┌────────────────────▼──────┐
│  user-service    │  │  product-service     │  │  order-service             │
│  Port 8081       │  │  Port 8082           │  │  Port 8084                 │
│  PostgreSQL      │  │  PostgreSQL + Redis  │  │  PostgreSQL (event store)  │
└──────────────────┘  └──────────────────────┘  └────────────────────────────┘
         │                        │                                   │
┌────────▼─────────┐  ┌──────────▼──────────┐  ┌────────────────────▼──────┐
│  search-service  │  │  inventory-service   │  │  payment-service           │
│  Port 8087       │  │  Port 8083           │  │  Port 8085                 │
│  Elasticsearch   │  │  PostgreSQL          │  │  PostgreSQL                │
└──────────────────┘  └──────────────────────┘  └────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         EVENT BUS  (Apache Kafka :9092)                      │
│  Topics: user.*, product.*, inventory.*, order.*, payment.*                  │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                         ┌──────────▼───────────┐
                         │  notification-service │
                         │  Port 8086            │
                         │  Kafka consumers only │
                         └───────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                        PLATFORM SERVICES                                      │
│                                                                               │
│  service-registry (Eureka)  :8761   │   config-server  :8888                 │
│  Zipkin (tracing)           :9411   │   Prometheus     :9090                 │
│  Grafana (dashboards)       :3000   │   Redis          :6379                 │
│  PostgreSQL (per service)   :5432+  │   Elasticsearch  :9200                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Service Inventory

| Service              | Port | Database         | Produces Topics                                                | Consumes Topics                                                          |
|----------------------|------|------------------|----------------------------------------------------------------|--------------------------------------------------------------------------|
| api-gateway          | 8080 | Redis (sessions) | —                                                              | —                                                                        |
| service-registry     | 8761 | —                | —                                                              | —                                                                        |
| config-server        | 8888 | —                | —                                                              | —                                                                        |
| user-service         | 8081 | user_db          | user.registered, user.password-changed                         | —                                                                        |
| product-service      | 8082 | product_db       | product.created, product.updated, product.deleted              | —                                                                        |
| inventory-service    | 8083 | inventory_db     | inventory.reserved, inventory.released, inventory.failed       | order.placed                                                             |
| order-service        | 8084 | order_db         | order.placed, order.confirmed, order.cancelled, order.shipped  | inventory.reserved, inventory.released, inventory.failed, payment.completed, payment.failed |
| payment-service      | 8085 | payment_db       | payment.completed, payment.failed                              | order.placed                                                             |
| notification-service | 8086 | —                | —                                                              | user.registered, order.confirmed, order.cancelled, payment.failed        |
| search-service       | 8087 | Elasticsearch    | —                                                              | product.created, product.updated, product.deleted                        |

---

## 5. Infrastructure Components

### Apache Kafka
- **Role:** Durable, ordered, replayable event bus. Decouples producers from consumers.
- **Why not REST?** REST creates temporal coupling (both services must be up simultaneously). Kafka stores messages; consumers process when ready.
- **Why not RabbitMQ?** Kafka retains messages indefinitely, enabling replay for new consumers (e.g., building a new read model from history). RabbitMQ deletes after delivery.
- Topics use 3 partitions and replication factor 1 (dev); in production use RF=3.

### PostgreSQL (one database per service)
- **Pattern: Database-per-Service** — each service owns its schema exclusively.
- No cross-service JOINs allowed. Data is shared through events, not direct DB access.
- Enables independent schema evolution, independent scaling (read replicas per service), and independent data retention policies.

### Redis
- **Rate limiting:** Fixed-window counter per IP using an atomic Lua script (see below).
- **Cache:** Product and category data (cache-aside pattern with TTL).
- **Refresh tokens:** Stored as `refresh:{userId}` with 7-day TTL.
- **Token blacklist:** Invalidated JWT IDs stored as `blacklist:{jti}` with TTL equal to remaining token lifetime.
- **Pub/Sub:** Used for real-time cart synchronization (optional extension).

#### Rate-Limiting Lua Script

The script lives inline in `RateLimitFilter.java` and is sent to Redis via `ReactiveStringRedisTemplate.execute(RedisScript, ...)`:

```lua
local count = redis.call('INCR', KEYS[1])   -- ① atomically increment the IP counter
if count == 1 then                            -- ② key was just created (first request in window)
  redis.call('EXPIRE', KEYS[1], ARGV[1])     -- ③ arm the TTL — 60 s window starts now
end
return count                                  -- ④ Java side compares this to maxRequests (100)
```

**Line-by-line explanation**

| Line | Command | What it does |
|------|---------|--------------|
| ① | `INCR KEYS[1]` | Atomically adds 1 to `rate:{clientIp}`. If the key doesn't exist Redis creates it at 0 first, so the first call always returns 1. |
| ② | `if count == 1` | Detects that this is the first request in the window — the key was freshly created by the INCR above. |
| ③ | `EXPIRE KEYS[1] ARGV[1]` | Sets a 60-second TTL. Called only once per window (when count == 1) so later requests don't accidentally reset the timer. |
| ④ | `return count` | The Java filter compares this value to `maxRequests` (100). If `count > 100` → HTTP 429. |

**Why Lua? The atomicity problem**

Without Lua, the naïve two-command version has a race condition:

```
Request A:  INCR rate:1.2.3.4  → returns 1
                                            Request B:  INCR rate:1.2.3.4 → returns 2
                                            Request B:  count != 1, skip EXPIRE
Request A:  EXPIRE rate:1.2.3.4 60
```

If Request B crashes or the connection drops between its INCR and the EXPIRE check, the key exists with no TTL — it lives forever and permanently blocks that IP. Redis executes an entire Lua script as a **single atomic operation**: no other client command can interleave between `INCR` and `EXPIRE`. This eliminates the race entirely.

**Why INCR and not GET → check → SET?**

`INCR` is natively atomic in Redis. A `GET`-then-`SET` pattern would require an optimistic lock (`WATCH / MULTI / EXEC`) to be safe under concurrency — far more complex and slower. `INCR` gives atomicity for free.

**Fixed window vs sliding window**

This implementation is a **fixed window** counter. The window resets when the Redis key expires (every 60 s). A known edge case: a client can burst up to 200 requests in a short span by sending 100 at the very end of one window and 100 at the very start of the next.

A true **sliding window** uses a sorted set to track per-request timestamps:

```lua
-- sliding window (more accurate, ~4× more expensive)
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - window)  -- evict old entries
local count = redis.call('ZADD', KEYS[1], now, now)       -- record this request
redis.call('EXPIRE', KEYS[1], window)
return redis.call('ZCARD', KEYS[1])                        -- how many in window?
```

The fixed-window approach is a deliberate trade-off: it is O(1) per request (one INCR, one conditional EXPIRE) vs O(log n) for the sorted-set approach, and accurate enough for protecting against abuse at 100 req/min scale.

### Elasticsearch
- **Role:** Full-text search and faceted browsing for the product catalog.
- **Why not PostgreSQL full-text?** PostgreSQL `tsvector` works for small datasets but lacks native facet aggregations, relevance scoring (TF/IDF), edge-n-gram autocomplete, and horizontal sharding.
- Products are indexed asynchronously via Kafka consumer in search-service.
- Index: `products-index` with custom analyzer for autocomplete.

### Zipkin
- **Role:** Distributed tracing. Every inter-service call propagates `X-B3-TraceId` headers.
- Allows you to see the full path of an order placement saga across 4+ services.

### Prometheus + Grafana
- Spring Boot Actuator exposes `/actuator/prometheus` on each service.
- Prometheus scrapes via internal Kubernetes service names — never through the public API Gateway. The gateway only exposes `/actuator/health` (exact match) for liveness checks; the full `/actuator/` tree is blocked from external traffic.
- Grafana dashboards show: RPS, error rate, p99 latency, JVM heap, Kafka consumer lag.

---

## 6. Per-Service Deep Dives

### 6.1 API Gateway

**Responsibilities:** Single entry point for all client traffic. Handles cross-cutting concerns so individual services don't need to.

**Routes:**
```
/api/auth/**      → user-service:8081     (no auth required)
/api/products/**  → product-service:8082  (GET public, POST/PUT/DELETE require ADMIN role)
/api/inventory/** → inventory-service:8083 (ADMIN only)
/api/orders/**    → order-service:8084    (authenticated users)
/api/payments/**  → payment-service:8085  (ADMIN only, internal)
/api/search/**    → search-service:8087   (GET public, POST/PUT/DELETE require auth)
/actuator/health  → internal only         (exact path match — full /actuator/ tree is NOT exposed)
```

**Filters (ordered):**
1. `CorrelationIdFilter` — generates UUID correlation ID, attaches as `X-Correlation-Id` request and response header. All downstream logs include this ID.
2. `JwtAuthFilter` — validates RS256 JWT from `Authorization: Bearer` header. **Identity header isolation:** `X-User-Id` and `X-User-Role` are stripped from every incoming request before any processing — client-supplied values are never trusted. On successful JWT validation, trusted values derived from the token claims are injected using `HttpHeaders.set()` (replace, not append). On public paths the same stripping applies; no identity headers reach downstream services from unauthenticated requests. Public paths: `/api/auth/**` (all methods), `GET /api/products/**`, `GET /api/search/**`, `/actuator/health` (exact match only — the full `/actuator/` tree is blocked).
3. `RateLimitFilter` — Redis fixed-window counter via atomic Lua script. Key: `rate:{clientIp}`. Limit: 100 requests per 60-second window. Returns HTTP 429 with `Retry-After` header if exceeded. Fails open on Redis errors (lets the request through rather than blocking all traffic). Client IP is read from `X-Forwarded-For` first (set by the ingress/load balancer), falling back to the TCP remote address. See the Redis section for a full explanation of the Lua script.

**Circuit Breaker:** Each route has a Resilience4j circuit breaker. After 5 consecutive failures, circuit opens for 10 seconds. Fallback returns a JSON error body with a friendly message.

**Key Learning:** The gateway pattern centralizes cross-cutting concerns. Without it, every service would need to implement rate limiting and JWT validation independently — a maintenance nightmare and a security risk.

---

### 6.2 Service Registry (Eureka)

**Responsibilities:** Service discovery. Every Spring Boot service registers itself on startup with its host, port, and health endpoint. The API Gateway uses Eureka to resolve `lb://user-service` to actual IPs.

**Why needed?** In Docker/Kubernetes, container IPs are dynamic. Services cannot hardcode each other's addresses. Eureka provides a dynamic registry.

**Configuration:** `@EnableEurekaServer` on the main class. All other services use `@EnableEurekaClient` (implicit in Spring Cloud).

---

### 6.3 Config Server

**Responsibilities:** Centralized configuration. All service `application.yml` files are served from Config Server. Services fetch their config at startup using `spring.config.import=configserver:http://config-server:8888`.

**Sensitive config stored here:** JWT RSA private key (for user-service), database passwords (loaded from env vars in production), Kafka bootstrap servers, Redis host.

**Why centralize config?** In a 10-service microservices system, maintaining 10 separate config files is error-prone. Config Server provides one source of truth with environment-specific overrides.

---

### 6.4 User Service

**Responsibilities:** User lifecycle and authentication. Issues JWTs, manages refresh tokens.

**API Endpoints:**
```
POST /api/auth/register    → Register user, hash password (BCrypt cost=12), publish user.registered
POST /api/auth/login       → Verify BCrypt hash, issue JWT (15min) + refresh token (7d in Redis)
POST /api/auth/refresh     → Validate refresh token from Redis, rotate (delete old, store new), issue new JWT
POST /api/auth/logout      → Delete refresh token from Redis, add JWT jti to blacklist
GET  /api/users/{id}       → Get user profile (requires auth, own profile or ADMIN)
```

**Default Admin Seeding:**

`register` always assigns `Role.USER`. To bootstrap the first admin, `DataInitializer` runs at startup:
```
On startup → existsByEmail("admin@ecommerce.com")
  If false → INSERT user {email=admin@ecommerce.com, passwordHash=BCrypt("Admin@1234"), role=ADMIN}
  If true  → no-op (idempotent)
```
Default credentials: `admin@ecommerce.com` / `Admin@1234` — change before production.

**JWT Design:**
- Algorithm: RS256 (RSA SHA-256 asymmetric)
- Private key: stored in Config Server, only user-service uses it to sign
- Public key: distributed to all services via Config Server, used to verify signatures
- Claims: `sub` (userId), `email`, `role`, `jti` (UUID for blacklisting), `iat`, `exp`
- Expiry: 15 minutes (short to limit blast radius of token theft)

**Refresh Token Design:**
- Opaque UUID stored in Redis as `refresh:{userId}` with 7-day TTL
- On each use: delete old key, store new key (rotation) — prevents replay attacks
- Stored alongside `refresh:{userId}:meta` with device/user-agent for audit

**Security Pattern: Stateless + Blacklist Hybrid**
- JWT is stateless (no server lookup per request, scales to any number of instances)
- Blacklist in Redis handles premature revocation (logout, password change)
- The blacklist only grows during the 15-minute JWT lifetime — very small

**Database Schema:**
```sql
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,       -- BCrypt hash
    role          VARCHAR(50)  NOT NULL DEFAULT 'USER',  -- USER, ADMIN, VENDOR
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_email ON users(email);
```

---

### 6.5 Product Service

**Responsibilities:** Product catalog CRUD. Implements cache-aside with Redis and guaranteed Kafka publishing via the Outbox Pattern.

**API Endpoints:**
```
GET    /api/products/{id}         → Cache-aside: check Redis first, fallback to DB  [public]
GET    /api/products?category=&page= → Paginated list (DB query, not cached)        [public]
POST   /api/products              → Create product + outbox record (same transaction) [ADMIN]
PUT    /api/products/{id}         → Update product + outbox record + invalidate Redis key [ADMIN]
DELETE /api/products/{id}         → Soft-delete (status=DELETED) + outbox + invalidate cache [ADMIN]
POST   /api/products/reindex      → Queue outbox events for all ACTIVE products → re-sync ES [ADMIN]
```

**Role-Based Access Control:**

Product write operations are restricted to the `ADMIN` role. The API Gateway validates the JWT, strips any client-supplied identity headers, and injects trusted `X-User-Id` and `X-User-Role` headers derived solely from the token claims. The product-service enforces authorization via Spring Security method-level security:

```
HeaderAuthFilter (OncePerRequestFilter)
  Reads X-User-Id and X-User-Role from the gateway-injected headers
  (client-supplied values are stripped at the gateway before this point)
  Creates a UsernamePasswordAuthenticationToken with ROLE_<role> as a GrantedAuthority
  Registers it in the SecurityContextHolder for the duration of the request

@PreAuthorize("hasRole('ADMIN')")  ← on POST, PUT, DELETE in ProductController
  Spring Security AOP checks the SecurityContext before the method executes
  Throws AccessDeniedException → returns HTTP 403 {"error":"Access denied: ADMIN role required"}
```

> **Trust model:** `HeaderAuthFilter` trusts `X-User-Id`/`X-User-Role` only because the gateway guarantees they originate from a validated JWT. In production, a Kubernetes `NetworkPolicy` should restrict direct pod-to-pod access so downstream services are unreachable without going through the gateway.

GET endpoints have no role requirement and are publicly accessible without a token.

**Cache-Aside Pattern:**
```
1. Request arrives for GET /api/products/123
2. Check Redis key "product:123"
3. Cache HIT  → deserialize JSON → return (fast path, ~1ms)
4. Cache MISS → query PostgreSQL → serialize → write to Redis with TTL=600s → return
5. On PUT/DELETE: invalidate "product:123" in Redis immediately
```

**Outbox Pattern (Guaranteed Delivery):**

Problem: If you write to DB and then publish to Kafka, a crash between the two steps leaves the DB updated but Kafka without the event. This is the dual-write problem.

Solution: Write to DB and to an `outbox` table in the **same transaction**. A separate scheduler reads unpublished outbox rows and publishes them to Kafka, marking them as published.

```
POST /api/products
  BEGIN TRANSACTION
    INSERT INTO products (...)
    INSERT INTO product_outbox (aggregate_id=productId, event_type='product.created', payload=JSON)
  COMMIT TRANSACTION
  ← return 201 (Kafka not yet published)

@Scheduled(fixedDelay=1000)
  SELECT * FROM product_outbox WHERE published = false ORDER BY created_at LIMIT 100
  FOR EACH row:
    kafkaTemplate.send(row.event_type, row.payload)
    UPDATE product_outbox SET published=true WHERE id=row.id
```

**Why this guarantees delivery:** If the scheduler crashes after publishing but before marking as published, it will re-publish on restart. Consumers must be idempotent (use product ID as idempotency key).

**Database Schema:**
```sql
CREATE TABLE categories (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name      VARCHAR(255) NOT NULL,
    parent_id UUID REFERENCES categories(id)
);

CREATE TABLE products (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(500) NOT NULL,
    description TEXT,
    price       NUMERIC(12,2) NOT NULL,
    category_id UUID REFERENCES categories(id),
    vendor_id   UUID NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, DELETED, OUT_OF_STOCK
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE product_outbox (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,          -- product ID
    event_type   VARCHAR(100) NOT NULL,  -- "product.created", etc.
    payload      JSONB NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published    BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_outbox_unpublished ON product_outbox(published, created_at) WHERE published = false;
```

---

### 6.6 Inventory Service

**Responsibilities:** Track stock levels per SKU. Reserve and release stock during the order saga. Prevent overselling under concurrent load using optimistic locking.

**API Endpoints:**
```
GET  /api/inventory/{skuId}          → Current available stock
POST /api/inventory/{skuId}/reserve  → Reserve N units for an order
POST /api/inventory/{skuId}/release  → Release previously reserved units
POST /api/inventory             → Create inventory record (admin)
PUT  /api/inventory/{skuId}/restock  → Add stock (admin)
```

**Optimistic Locking:**
```java
@Entity
public class Inventory {
    @Version
    private Long version;  // JPA increments this on each update
    
    private int quantity;
    private int reservedQuantity;
}
```

When two concurrent requests try to reserve the same SKU:
- Thread A reads version=5, Thread B reads version=5
- Thread A updates: `UPDATE inventory SET reserved=10, version=6 WHERE id=X AND version=5` → succeeds
- Thread B updates: `UPDATE inventory SET reserved=10, version=6 WHERE id=X AND version=5` → fails (version now 6)
- Thread B gets `OptimisticLockException`, service catches and retries up to 3 times
- This avoids pessimistic locking (which serializes all requests and kills throughput)

**Kafka Integration:**
- Consumes `order.placed` → attempts to reserve stock for each order item
- On success: publishes `inventory.reserved`
- On failure (insufficient stock): publishes `inventory.failed`
- The order saga listens to these responses to advance its state machine

**Database Schema:**
```sql
CREATE TABLE inventory (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id             UUID UNIQUE NOT NULL,
    product_id         UUID NOT NULL,
    quantity           INT NOT NULL DEFAULT 0,
    reserved_quantity  INT NOT NULL DEFAULT 0,
    version            BIGINT NOT NULL DEFAULT 0,  -- optimistic lock
    CONSTRAINT chk_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_reserved CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_available CHECK (quantity >= reserved_quantity)
);

CREATE TABLE inventory_transaction (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id     UUID NOT NULL REFERENCES inventory(sku_id),
    type       VARCHAR(50) NOT NULL,  -- RESERVE, RELEASE, RESTOCK, SELL
    quantity   INT NOT NULL,
    order_id   UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

### 6.7 Order Service — CQRS + Event Sourcing + Saga

This is the most complex service. It demonstrates three advanced patterns simultaneously.

#### CQRS (Command Query Responsibility Segregation)

**Core idea:** The model optimized for writing (consistency, validation) is different from the model optimized for reading (speed, shape of data).

```
Write side (Command):
  POST /api/orders → PlaceOrderCommand → PlaceOrderCommandHandler
    → validates business rules
    → appends OrderPlacedEvent to event store
    → triggers OrderSaga
    
Read side (Query):
  GET /api/orders/{id} → GetOrderQueryHandler
    → reads from orders_read_model table (pre-projected, denormalized)
    → returns immediately without replaying events
```

**Why separate?** Reads vastly outnumber writes (users check order status many times). The read model is a flat, denormalized table optimized for fast SELECT. The write model is a normalized event log optimized for consistency.

#### Event Sourcing

**Core idea:** The current state of an order is not stored directly. Instead, every state change is stored as an immutable event. The current state is derived by replaying all events.

```
Order 123 event log:
  1. OrderPlacedEvent     {orderId, userId, items, total}
  2. InventoryReservedEvent {orderId, reservations}
  3. PaymentCompletedEvent  {orderId, paymentId, amount}
  4. OrderConfirmedEvent    {orderId}

Current state = replay all 4 events in sequence
```

**Benefits:**
- Full audit trail (who ordered what, when, what happened to it)
- Time-travel debugging (replay events up to a point to see past state)
- New projections can be built by replaying the full event history
- Easy to implement compensating transactions (just append a CancelledEvent)

**Event Store Schema:**
```sql
CREATE TABLE order_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB NOT NULL,
    sequence_no  INT NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (order_id, sequence_no)  -- prevents duplicate events
);

CREATE TABLE orders_read_model (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    status      VARCHAR(50) NOT NULL,
    total       NUMERIC(12,2) NOT NULL,
    items_json  JSONB NOT NULL,           -- denormalized for fast reads
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
```

#### Saga Pattern (Orchestration-based)

**Core idea:** A distributed transaction spanning multiple services. The order saga orchestrates the sequence: reserve inventory → charge payment → confirm order. If any step fails, compensating transactions roll back previous steps.

**Why Saga over 2PC (Two-Phase Commit)?**
- 2PC requires a distributed coordinator that locks resources across services. If the coordinator crashes, resources stay locked indefinitely.
- Saga releases resources immediately via compensating transactions. No global locks. Better availability.
- Trade-off: eventual consistency. There is a window where inventory is reserved but the order is not yet confirmed.

**Saga State Machine:**

```
                     ┌─────────┐
                     │ PENDING │  ← initial state after order.placed
                     └────┬────┘
                          │ inventory.reserved
                          ▼
               ┌──────────────────────┐
               │ INVENTORY_RESERVED   │
               └──────────┬───────────┘
                          │ (saga triggers payment)
                          ▼
               ┌──────────────────────┐
               │ PAYMENT_PROCESSING   │
               └──────────┬───────────┘
                          │ payment.completed
                          ▼
                    ┌──────────┐
                    │CONFIRMED │  ← happy path end
                    └──────────┘

Failure paths:
  PENDING             + inventory.failed  → CANCELLED
  INVENTORY_RESERVED  + payment.failed   → (compensate: release inventory) → CANCELLED
```

**Saga Persistence:**
```sql
CREATE TABLE order_saga (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID UNIQUE NOT NULL,
    state       VARCHAR(50) NOT NULL,
    saga_data   JSONB NOT NULL,   -- stores reservation IDs needed for compensation
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Key point:** The saga state is persisted. If the order-service crashes mid-saga, on restart it reads the saga table and resumes from the last known state.

---

### 6.8 Payment Service

**Responsibilities:** Process payments idempotently. Publish results to Kafka for the order saga to consume.

**API Endpoints:**
```
POST /api/payments/charge    → Charge (idempotent via idempotency_key)
GET  /api/payments/{orderId} → Get payment status
```

**Idempotency Pattern:**
```
Request arrives with header: Idempotency-Key: {orderId}
  SELECT id FROM payments WHERE idempotency_key = ?
  IF found → return existing result (do NOT re-charge)
  IF not found:
    BEGIN TRANSACTION
      INSERT INTO payments (idempotency_key, ...)
      INSERT INTO payment_outbox (...)
    COMMIT
    → return result
```

**Why idempotency?** The order saga may retry a payment request if it doesn't receive a response (network timeout, service restart). Without idempotency, the customer gets charged twice.

**Simulated Payment Gateway:**
- 95% success rate (random) for testing failure + compensation paths
- In production, this calls Stripe/PayPal SDK

**Database Schema:**
```sql
CREATE TABLE payments (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    status           VARCHAR(50) NOT NULL,    -- PENDING, COMPLETED, FAILED, REFUNDED
    idempotency_key  VARCHAR(255) UNIQUE NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payment_outbox (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type   VARCHAR(100) NOT NULL,  -- payment.completed, payment.failed
    payload      JSONB NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published    BOOLEAN NOT NULL DEFAULT FALSE
);
```

---

### 6.9 Notification Service

**Responsibilities:** Consume domain events and send user notifications. Completely decoupled — the rest of the system does not know or care that notifications exist.

**Design:** Stateless Kafka consumer. No database. No outbound HTTP calls during order flow (notifications are fire-and-forget).

**Consumers:**
- `user.registered` → welcome email
- `order.confirmed` → order confirmation email
- `order.cancelled` → cancellation email with reason
- `payment.failed` → payment failure email

**Templates:** Thymeleaf HTML email templates with order details, links to track orders.

**Dev mode:** Instead of sending real emails (requires SMTP), logs the email body to stdout.

**Key Learning:** The notification service can be added or removed without touching any other service. This is the power of event-driven architecture — new consumers subscribe to existing topics.

---

### 6.10 Search Service

**Responsibilities:** Full-text product search with facets, sorting, and autocomplete, backed by Elasticsearch.

**API Endpoints:**
```
GET /api/search?q=laptop&category=Electronics&minPrice=500&maxPrice=2000&page=0&size=20
GET /api/search/autocomplete?q=lap
```

**Elasticsearch Index Design:**
```json
{
  "mappings": {
    "properties": {
      "id":          { "type": "keyword" },
      "name":        { "type": "text", "analyzer": "standard",
                       "fields": { "autocomplete": { "type": "text", "analyzer": "edge_ngram_analyzer" } } },
      "description": { "type": "text" },
      "category":    { "type": "keyword" },
      "price":       { "type": "double" },
      "vendor_id":   { "type": "keyword" },
      "status":      { "type": "keyword" }
    }
  },
  "settings": {
    "analysis": {
      "analyzer": {
        "edge_ngram_analyzer": {
          "tokenizer": "edge_ngram_tokenizer"
        }
      },
      "tokenizer": {
        "edge_ngram_tokenizer": {
          "type": "edge_ngram",
          "min_gram": 2,
          "max_gram": 10
        }
      }
    }
  }
}
```

**Query Design (faceted search):**
```json
{
  "query": {
    "bool": {
      "must": [
        { "multi_match": { "query": "laptop", "fields": ["name^3", "description"] } }
      ],
      "filter": [
        { "term": { "category": "Electronics" } },
        { "range": { "price": { "gte": 500, "lte": 2000 } } },
        { "term": { "status": "ACTIVE" } }
      ]
    }
  },
  "aggs": {
    "categories": { "terms": { "field": "category" } },
    "price_ranges": { "range": { "field": "price", "ranges": [...] } }
  }
}
```

**Kafka Consumer:** Listens to `product.created`, `product.updated`, `product.deleted` topics. Synchronizes Elasticsearch index with the product catalog. Eventual consistency — lag is typically < 1 second.

**CQRS at Platform Level:** Search service is the read replica for the product catalog. Product service owns the write model (PostgreSQL). Search service owns the read model (Elasticsearch). They are synchronized asynchronously via Kafka.

---

### 6.11 Angular Frontend

**Architecture:** Feature-module-based SPA with lazy loading. NgRx for global state management.

**Module Structure:**
- `CoreModule` (singleton): HTTP interceptors, auth guards, global services. Imported once in AppModule.
- `SharedModule`: Reusable dumb components (search bar, pagination, loading spinner). Imported in feature modules.
- Feature Modules (lazy-loaded):
  - `AuthModule` — login, register
  - `CatalogModule` — product list, product detail
  - `CartModule` — cart view, checkout button
  - `OrdersModule` — order history, order detail
  - `AdminModule` — product management (create, edit, delete); guarded by `AdminGuard`

**Route Guards:**
- `AuthGuard` — blocks unauthenticated users (checks token presence), redirects to `/login`
- `AdminGuard` — blocks non-admin users (checks `user.role === 'ADMIN'`), redirects to `/products`; applied on top of `AuthGuard` for the `/admin` route

**Lazy Routes:**
```
/              → redirectTo /products
/login         → AuthModule
/register      → AuthModule
/products      → CatalogModule            (public)
/products/:id  → CatalogModule            (public)
/cart          → CartModule               (AuthGuard)
/orders        → OrdersModule             (AuthGuard)
/admin                    → AdminModule   (AuthGuard + AdminGuard)
/admin/products/new       → AdminModule   (AuthGuard + AdminGuard)
/admin/products/:id/edit  → AdminModule   (AuthGuard + AdminGuard)
```

**NgRx Store Slices:**
```typescript
AppState {
  auth: {
    user: User | null,         // includes role: 'USER' | 'ADMIN' | 'VENDOR'
    accessToken: string | null,
    loading: boolean,
    error: string | null
  },
  products: {
    products: Product[],
    selectedProduct: Product | null,
    loading: boolean,          // true during list/detail fetch
    submitting: boolean,       // true during create / update / delete
    error: string | null,
    totalElements: number,
    totalPages: number,
    currentPage: number
  },
  cart: {
    items: CartItem[],
    total: number
  }
}
```

**Key Selectors (auth):**
- `selectIsLoggedIn` — `!!accessToken && !!user`
- `selectIsAdmin` — `user?.role === 'ADMIN'` — used by navbar and AdminGuard
- `selectCurrentUser` — full User object with role

**HTTP Interceptors:**
1. `AuthInterceptor` — adds `Authorization: Bearer {token}` header to every request (admin writes included)
2. `ErrorInterceptor` — catches 401 (triggers token refresh), 429 (shows rate limit toast), 5xx (shows error notification)

**Cart Persistence:** Cart state is persisted to `localStorage` via a NgRx meta-reducer. Cart survives page refresh.

**Product Search:** Debounced 300ms input in the search bar component. Each keystroke after debounce dispatches `loadProducts` which triggers an NgRx Effect calling the search API. Results update the store; the product-list component renders from the store.

**Admin Product Management:**
- Navbar shows an **Admin** link (amber colour) only when `selectIsAdmin` is true.
- `/admin` — table of all products with Edit and Delete buttons per row.
- `/admin/products/new` — `ProductFormComponent` in create mode (dispatches `createProduct`).
- `/admin/products/:id/edit` — `ProductFormComponent` in edit mode (dispatches `loadProduct` to pre-fill, then `updateProduct` on save).
- On create/update success the effect navigates back to `/admin` via `Router.navigate`.
- Delete uses `window.confirm` before dispatching `deleteProduct`; the reducer removes the row from state immediately on `deleteProductSuccess`.
- All write actions set `submitting: true` while in-flight — the Save/Delete buttons are disabled during that window.

---

## 7. Data Models

### Java Entities (representative)

```java
// User Service
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    private Role role;           // USER, ADMIN, VENDOR
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// Product Service
@Entity @Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private UUID categoryId;
    private UUID vendorId;
    @Enumerated(EnumType.STRING)
    private ProductStatus status;  // ACTIVE, DELETED, OUT_OF_STOCK
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// Inventory Service
@Entity @Table(name = "inventory")
public class Inventory {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true)
    private UUID skuId;
    private UUID productId;
    private int quantity;
    private int reservedQuantity;
    @Version                       // Optimistic lock field
    private Long version;
}

// Order Service — Event Store
@Entity @Table(name = "order_events")
public class OrderEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID orderId;
    private String eventType;
    @Column(columnDefinition = "jsonb")
    private String payload;
    private int sequenceNo;
    private LocalDateTime occurredAt;
}

// Order Service — Read Model
@Entity @Table(name = "orders_read_model")
public class OrderReadModel {
    @Id
    private UUID id;
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private BigDecimal total;
    @Column(columnDefinition = "jsonb")
    private String itemsJson;     // Denormalized for fast reads
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 8. Kafka Topics & Event Contracts

### Topic: `user.registered`
```json
{
  "eventId": "uuid",
  "userId": "uuid",
  "email": "string",
  "occurredAt": "ISO-8601"
}
```

### Topic: `product.created` / `product.updated`
```json
{
  "eventId": "uuid",
  "productId": "uuid",
  "name": "string",
  "description": "string",
  "price": "number",
  "categoryId": "uuid",
  "vendorId": "uuid",
  "status": "ACTIVE",
  "occurredAt": "ISO-8601"
}
```

### Topic: `order.placed`
```json
{
  "eventId": "uuid",
  "orderId": "uuid",
  "userId": "uuid",
  "items": [
    { "skuId": "uuid", "productId": "uuid", "quantity": 2, "unitPrice": 49.99 }
  ],
  "total": "number",
  "occurredAt": "ISO-8601"
}
```

### Topic: `inventory.reserved`
```json
{
  "eventId": "uuid",
  "orderId": "uuid",
  "reservations": [
    { "skuId": "uuid", "quantity": 2 }
  ],
  "occurredAt": "ISO-8601"
}
```

### Topic: `inventory.failed`
```json
{
  "eventId": "uuid",
  "orderId": "uuid",
  "skuId": "uuid",
  "reason": "INSUFFICIENT_STOCK",
  "occurredAt": "ISO-8601"
}
```

### Topic: `payment.completed`
```json
{
  "eventId": "uuid",
  "paymentId": "uuid",
  "orderId": "uuid",
  "amount": "number",
  "occurredAt": "ISO-8601"
}
```

### Topic: `payment.failed`
```json
{
  "eventId": "uuid",
  "orderId": "uuid",
  "reason": "string",
  "occurredAt": "ISO-8601"
}
```

---

## 9. Redis Key Patterns

| Key Pattern              | Type   | Value                       | TTL    | Purpose                                      |
|--------------------------|--------|-----------------------------|--------|----------------------------------------------|
| `product:{id}`           | String | JSON serialized Product     | 600s   | Cache-aside for product reads                |
| `category:tree`          | String | JSON category tree          | 3600s  | Rarely-changing hierarchical data            |
| `refresh:{userId}`       | String | Opaque refresh token UUID   | 604800s| Refresh token store (7 days)                 |
| `blacklist:{jti}`        | String | "1"                         | Remaining JWT TTL | Revoked JWT IDs        |
| `rate:{clientIp}`        | String | Request count (integer)     | 60s    | Rate limiting fixed-window counter (Lua INCR + EXPIRE) |
| `cart:{userId}`          | Hash   | Map of skuId → CartItem     | 86400s | Server-side cart (optional, for cross-device)|

---

## 10. Sequence Diagrams

### 10.1 User Login Flow

```
Client          API Gateway         user-service          Redis
  │                  │                    │                  │
  │──POST /login────►│                    │                  │
  │                  │──forward request──►│                  │
  │                  │                    │──BCrypt.verify()─┤
  │                  │                    │◄─match───────────┤
  │                  │                    │──SETEX refresh:userId token 604800──►│
  │                  │◄──{accessToken,refreshToken}──────────│
  │◄─200 {tokens}───│                    │                  │
```

### 10.2 Product Search Flow

```
Client          API Gateway       search-service       Elasticsearch
  │                  │                  │                    │
  │──GET /search?q──►│                  │                    │
  │                  │──validate JWT────┤                    │
  │                  │──route to search►│                    │
  │                  │                  │──bool query───────►│
  │                  │                  │◄──hits + aggs──────│
  │◄─200 {results}──│◄─paginated resp──│                    │
```

### 10.3 Order Placement Saga — Happy Path

```
Client  API-GW  order-svc   Kafka      inventory-svc  payment-svc  notification-svc
  │       │        │           │               │             │              │
  │─POST──►│        │           │               │             │              │
  │       │─route──►│           │               │             │              │
  │       │        │─INSERT order_events[OrderPlaced]         │              │
  │       │        │─INSERT order_saga[PENDING]               │              │
  │       │        │─────────publish order.placed────────────►│             │
  │◄202───│◄─202───│           │               │             │              │
  │       │        │           │◄─order.placed─┤             │              │
  │       │        │           │  (consumed)   │             │              │
  │       │        │           │               │─reserve()   │              │
  │       │        │           │               │─publish inv.reserved──────►│
  │       │        │◄──────────inventory.reserved─────────────┤             │
  │       │        │─saga: INVENTORY_RESERVED                │              │
  │       │        │─────────publish order.placed for payment──────────────►│
  │       │        │           │               │─────────────│─charge()     │
  │       │        │           │               │             │─publish pay.completed
  │       │        │◄──────────────────────────payment.completed────────────┤
  │       │        │─INSERT order_events[OrderConfirmed]      │              │
  │       │        │─UPDATE read_model[CONFIRMED]             │              │
  │       │        │─publish order.confirmed──────────────────────────────►│
  │       │        │           │               │             │─send email───┤
```

### 10.4 Order Saga — Payment Failure + Compensation

```
  ...(same until inventory.reserved)...
  
  payment-service: charge fails (simulated)
  payment-service: publish payment.failed
  
  order-service: consume payment.failed
  order-service: read saga_data (contains reservation IDs)
  order-service: publish inventory.release-request (compensation)
  
  inventory-service: consume release-request
  inventory-service: release reserved units
  inventory-service: publish inventory.released
  
  order-service: consume inventory.released
  order-service: INSERT order_events[OrderCancelledEvent]
  order-service: UPDATE read_model[CANCELLED]
  order-service: publish order.cancelled
  
  notification-service: consume order.cancelled → send cancellation email
```

### 10.5 Cache-Aside Pattern (Product Read)

```
Client    API-GW    product-svc    Redis    PostgreSQL
  │          │           │           │           │
  │─GET /p/1►│           │           │           │
  │          │───────────►│           │           │
  │          │           │─GET product:1─────────►│
  │          │           │◄──MISS────────────────-│
  │          │           │─────────────────────SELECT id=1─►│
  │          │           │◄────────────────────────result───│
  │          │           │─SETEX product:1 600 JSON──────────►│
  │          │◄──result──│           │           │
  │◄─result──│           │           │           │
  
  (second request within 600s:)
  │─GET /p/1►│           │           │           │
  │          │───────────►│           │           │
  │          │           │─GET product:1─────────►│
  │          │           │◄──HIT JSON────────────-│
  │          │◄──result──│           │           │
  │◄─result──│           │           │           │
```

---

## 11. Design Pattern Reference

| Pattern                    | Service(s)           | Problem Solved                                                    |
|----------------------------|----------------------|-------------------------------------------------------------------|
| API Gateway                | api-gateway          | Clients should not know about individual microservices            |
| Service Registry           | all                  | Services cannot hardcode each other's dynamic IPs                 |
| Centralized Config         | config-server        | Configuration scattered across 10 services                        |
| JWT + Refresh Token Rotation | user-service       | Stateless auth that can be revoked without shared session store   |
| Cache-Aside                | product-service      | Avoid repeated expensive DB reads for popular products            |
| Outbox Pattern             | product, payment     | Dual-write problem: DB and Kafka must both succeed atomically     |
| Database-per-Service       | all                  | Independent scaling, schema evolution, and ownership              |
| CQRS                       | order-service        | Read and write workloads have different shapes and scale needs    |
| Event Sourcing             | order-service        | Full audit trail, time-travel debugging, new projections from history |
| Saga (Orchestration)       | order-service        | Distributed transaction without 2PC blocking                      |
| Optimistic Locking         | inventory-service    | High-throughput concurrent updates without DB-level locks          |
| Idempotency Key            | payment-service      | Prevent duplicate charges on retry                                |
| Circuit Breaker            | api-gateway          | Stop cascading failures when a downstream service is slow/down    |
| Rate Limiting              | api-gateway          | Protect services from abuse and accidental thundering herd        |
| Correlation ID             | api-gateway          | Trace a request across all services in logs and Zipkin            |
| Compensating Transaction   | order saga           | Rollback distributed changes when a saga step fails               |

---

## 12. Technology Decision Rationale

### Why Kafka over REST for Saga Choreography?
REST creates temporal coupling: both services must be simultaneously available. A network partition or service restart causes the REST call to fail. Kafka stores the message in a durable log. If inventory-service is restarting when order-service publishes `order.placed`, the message waits. When inventory-service comes back, it processes the message. **Durability is the key advantage.**

### Why Redis for Rate Limiting?
Rate limiting requires atomic increment-and-check operations. In PostgreSQL this requires a row lock per IP, which under high traffic causes contention. Redis `INCR` is atomic and executes in microseconds. The Lua script combines `INCR` and `EXPIRE` into a single atomic operation — without Lua, a crash between the two commands could leave a key with no TTL, permanently blocking an IP. PostgreSQL connection overhead alone (1-5ms) would dominate a 100 RPS rate-limit check.

### Why Elasticsearch over PostgreSQL for Search?
PostgreSQL `tsvector` full-text search works for < 100k rows. At 10M products:
- Inverted indexes in Elasticsearch are 10-100x faster than B-tree scans on text
- Faceted aggregations (count by category, price histogram) require `GROUP BY` in PostgreSQL — table scans at millions of rows
- Edge-n-gram tokenizer for autocomplete is not available in PostgreSQL
- Elasticsearch is designed to scale horizontally via sharding; PostgreSQL full-text search is single-node

### Why Event Sourcing in Order Service?
E-commerce orders require a full audit trail for legal, dispute resolution, and debugging. With event sourcing, you can always replay events to find exactly what happened at each step. Without it, a bug that corrupts order status would be unrecoverable — you'd have no history.

### Why RS256 JWT over HS256?
- HS256 uses a shared symmetric key. Any service that can verify tokens can also create them. A compromised microservice could forge tokens.
- RS256 uses asymmetric keys. Only user-service has the private key and can sign tokens. All other services verify with the public key. A compromised downstream service cannot forge tokens.

### Why Outbox over Sending to Kafka Directly After Commit?
```java
// Dangerous: if crash occurs here, DB has the product but Kafka has no event
productRepository.save(product);
// ← CRASH HERE = inconsistent state forever
kafkaTemplate.send("product.created", event);
```
The outbox table and the product write happen in the same DB transaction. Either both succeed or neither does. The async publisher handles the eventual delivery to Kafka.

---

## 13. System Design Interview Talking Points

**Q: Why not use a single monolithic database?**
A: Shared DB creates tight coupling. Schema changes in one service can break others. Locking contention between services (e.g., order writes blocking product reads). Independent scaling becomes impossible. The constraint "no shared database" forces services to communicate through well-defined contracts (APIs and events).

**Q: Why not use 2PC (Two-Phase Commit) instead of Saga?**
A: 2PC requires a distributed coordinator (like XA transaction manager). If the coordinator crashes mid-transaction, all participants hold locks indefinitely. In a system with 10 services, 2PC becomes a single point of failure. Saga uses compensating transactions — no global locks, better availability, explicit failure handling.

**Q: How do you handle the case where a Saga compensation also fails?**
A: Implement retry with exponential backoff and jitter for compensating transactions. After N retries, move to a dead-letter queue. An alert triggers a human operator to investigate. This is an accepted trade-off — saga compensation failure is extremely rare but must be handled.

**Q: How does the search index stay consistent with the product DB?**
A: It doesn't — eventually. The Outbox → Kafka → search-service pipeline has typical latency < 1 second. For an e-commerce catalog this is acceptable. If a user creates a product and searches immediately, they might not see it. This is a documented behavior, not a bug. Strong consistency for search would require synchronous ES writes during product create — too slow and creating coupling.

**Q: How do you prevent the same Kafka message from being processed twice?**
A: Consumers are designed to be idempotent. For inventory reservation: use the `orderId` as a unique key; if a reservation already exists for that orderId, return the existing result. For Elasticsearch indexing: upsert operations by `productId` are naturally idempotent. For payment: idempotency key (orderId) prevents duplicate charges.

**Q: How do you scale the order service?**
A: Multiple order-service instances can run simultaneously. The Kafka consumer group ensures each `order.placed` message is delivered to exactly one instance. The saga state is in PostgreSQL (shared across instances). A new instance that picks up a saga message reads the saga state from DB and continues from where the crashed instance left off.

**Q: What is your observability strategy?**
A: Three pillars — Logs (structured JSON with correlationId, serviceId, userId), Metrics (Prometheus scraped from `/actuator/prometheus` — RPS, error rate, Kafka consumer lag, JVM heap), Traces (Zipkin distributed tracing via Micrometer, shows full saga path across services). Alert on: error rate > 1%, Kafka consumer lag > 1000, p99 latency > 1s.

---

## 14. Complete File/Directory Structure

```
SystemDesignJavaTrial1/
├── plan.md                                    ← THIS FILE
├── docs.txt                                   ← Line-by-line code documentation
├── docker-compose.yml                         ← All 10 services + infrastructure
├── docker-compose.infra.yml                   ← Infrastructure only (Kafka, DBs, Redis, ES)
│
├── infrastructure/
│   ├── init-scripts/
│   │   ├── 01-user-db.sql
│   │   ├── 02-product-db.sql
│   │   ├── 03-inventory-db.sql
│   │   ├── 04-order-db.sql
│   │   └── 05-payment-db.sql
│   └── elasticsearch/
│       └── products-index-mapping.json
│
├── api-gateway/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/gateway/
│       │   ├── GatewayApplication.java
│       │   ├── config/
│       │   │   ├── GatewayConfig.java
│       │   │   └── SecurityConfig.java
│       │   ├── filter/
│       │   │   ├── JwtAuthFilter.java
│       │   │   ├── RateLimitFilter.java
│       │   │   └── CorrelationIdFilter.java
│       │   ├── util/JwtUtil.java
│       │   └── fallback/FallbackController.java
│       └── resources/application.yml
│
├── service-registry/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/registry/RegistryApplication.java
│       └── resources/application.yml
│
├── config-server/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/config/ConfigServerApplication.java
│       └── resources/
│           ├── application.yml
│           └── configs/
│               ├── user-service.yml
│               ├── product-service.yml
│               ├── inventory-service.yml
│               ├── order-service.yml
│               ├── payment-service.yml
│               ├── notification-service.yml
│               └── search-service.yml
│
├── user-service/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/user/
│       │   ├── UserServiceApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── JwtConfig.java
│       │   │   └── DataInitializer.java         ← seeds default admin on startup (user-service)
│       │   ├── controller/AuthController.java
│       │   ├── service/
│       │   │   ├── AuthService.java
│       │   │   └── TokenService.java
│       │   ├── repository/UserRepository.java
│       │   ├── entity/User.java
│       │   ├── enums/Role.java
│       │   ├── dto/
│       │   │   ├── RegisterRequest.java
│       │   │   ├── LoginRequest.java
│       │   │   ├── RefreshRequest.java
│       │   │   └── TokenResponse.java
│       │   └── kafka/UserEventPublisher.java
│       └── resources/application.yml
│
├── product-service/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/product/
│       │   ├── ProductServiceApplication.java
│       │   ├── config/
│       │   │   ├── RedisConfig.java
│       │   │   ├── SecurityConfig.java          ← Spring Security + @EnableMethodSecurity
│       │   │   └── DataInitializer.java         ← seeds 12 sample products via createProduct() on first startup
│       │   ├── security/
│       │   │   └── HeaderAuthFilter.java        ← reads X-User-Id/X-User-Role → SecurityContext
│       │   ├── controller/ProductController.java  ← includes POST /reindex (ADMIN) to re-sync Elasticsearch
│       │   ├── service/ProductService.java        ← includes reindexAll() for ES recovery
│       │   ├── repository/
│       │   │   ├── ProductRepository.java
│       │   │   ├── CategoryRepository.java
│       │   │   └── OutboxRepository.java
│       │   ├── entity/
│       │   │   ├── Product.java
│       │   │   ├── Category.java
│       │   │   └── ProductOutbox.java
│       │   ├── enums/ProductStatus.java
│       │   ├── dto/
│       │   │   ├── ProductRequest.java
│       │   │   └── ProductResponse.java
│       │   ├── kafka/OutboxPublisher.java
│       │   └── cache/ProductCacheService.java
│       └── resources/application.yml
│
├── inventory-service/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/inventory/
│       │   ├── InventoryServiceApplication.java
│       │   ├── controller/InventoryController.java
│       │   ├── service/InventoryService.java
│       │   ├── repository/
│       │   │   ├── InventoryRepository.java
│       │   │   └── InventoryTransactionRepository.java
│       │   ├── entity/
│       │   │   ├── Inventory.java
│       │   │   └── InventoryTransaction.java
│       │   ├── dto/
│       │   │   ├── ReservationRequest.java
│       │   │   └── ReservationResponse.java
│       │   └── kafka/
│       │       ├── OrderEventConsumer.java
│       │       └── InventoryEventPublisher.java
│       └── resources/application.yml
│
├── order-service/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/order/
│       │   ├── OrderServiceApplication.java
│       │   ├── command/
│       │   │   ├── PlaceOrderCommand.java
│       │   │   └── PlaceOrderCommandHandler.java
│       │   ├── query/
│       │   │   ├── GetOrderQuery.java
│       │   │   └── GetOrderQueryHandler.java
│       │   ├── saga/
│       │   │   ├── OrderSaga.java
│       │   │   └── OrderSagaState.java
│       │   ├── event/
│       │   │   ├── OrderEvent.java
│       │   │   ├── OrderPlacedEvent.java
│       │   │   ├── OrderConfirmedEvent.java
│       │   │   └── OrderCancelledEvent.java
│       │   ├── projection/OrderProjector.java
│       │   ├── repository/
│       │   │   ├── OrderEventRepository.java
│       │   │   ├── OrderReadModelRepository.java
│       │   │   └── OrderSagaRepository.java
│       │   ├── entity/
│       │   │   ├── OrderEventEntity.java
│       │   │   ├── OrderReadModel.java
│       │   │   └── OrderSagaEntity.java
│       │   ├── enums/
│       │   │   ├── OrderStatus.java
│       │   │   └── SagaState.java
│       │   ├── controller/OrderController.java
│       │   ├── dto/
│       │   │   ├── PlaceOrderRequest.java
│       │   │   └── OrderResponse.java
│       │   └── kafka/
│       │       ├── OrderEventPublisher.java
│       │       └── SagaEventConsumer.java
│       └── resources/application.yml
│
├── payment-service/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/payment/
│       │   ├── PaymentServiceApplication.java
│       │   ├── controller/PaymentController.java
│       │   ├── service/PaymentService.java
│       │   ├── repository/
│       │   │   ├── PaymentRepository.java
│       │   │   └── PaymentOutboxRepository.java
│       │   ├── entity/
│       │   │   ├── Payment.java
│       │   │   └── PaymentOutbox.java
│       │   ├── enums/PaymentStatus.java
│       │   ├── dto/
│       │   │   ├── ChargeRequest.java
│       │   │   └── PaymentResponse.java
│       │   └── kafka/
│       │       ├── OrderEventConsumer.java
│       │       └── PaymentEventPublisher.java
│       └── resources/application.yml
│
├── notification-service/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/notification/
│       │   ├── NotificationServiceApplication.java
│       │   ├── consumer/
│       │   │   ├── UserEventConsumer.java
│       │   │   └── OrderEventConsumer.java
│       │   ├── service/EmailService.java
│       │   └── dto/EmailNotification.java
│       └── resources/
│           ├── application.yml
│           └── templates/
│               ├── welcome-email.html
│               ├── order-confirmed-email.html
│               └── order-cancelled-email.html
│
├── search-service/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/search/
│       │   ├── SearchServiceApplication.java
│       │   ├── controller/SearchController.java
│       │   ├── service/SearchService.java
│       │   ├── consumer/ProductEventConsumer.java
│       │   ├── config/ElasticsearchConfig.java
│       │   └── dto/
│       │       ├── SearchRequest.java
│       │       └── SearchResponse.java
│       └── resources/application.yml
│
└── frontend/
    ├── package.json
    ├── angular.json
    ├── tsconfig.json
    └── src/
        ├── main.ts
        ├── index.html
        ├── styles.scss
        └── app/
            ├── app.module.ts
            ├── app-routing.module.ts
            ├── app.component.ts
            ├── core/
            │   ├── core.module.ts
            │   ├── interceptors/
            │   │   ├── auth.interceptor.ts
            │   │   └── error.interceptor.ts
            │   ├── guards/
            │   │   ├── auth.guard.ts
            │   │   └── admin.guard.ts              ← role guard: ADMIN only
            │   └── services/
            │       ├── auth.service.ts
            │       └── token-storage.service.ts
            ├── store/
            │   ├── index.ts
            │   ├── auth/
            │   │   ├── auth.actions.ts
            │   │   ├── auth.effects.ts
            │   │   ├── auth.reducer.ts
            │   │   └── auth.selectors.ts
            │   ├── products/
            │   │   ├── products.actions.ts
            │   │   ├── products.effects.ts
            │   │   ├── products.reducer.ts
            │   │   └── products.selectors.ts
            │   └── cart/
            │       ├── cart.actions.ts
            │       ├── cart.effects.ts
            │       ├── cart.reducer.ts
            │       └── cart.selectors.ts
            ├── features/
            │   ├── auth/
            │   │   ├── auth.module.ts
            │   │   ├── login/
            │   │   │   ├── login.component.ts
            │   │   │   └── login.component.html
            │   │   └── register/
            │   │       ├── register.component.ts
            │   │       └── register.component.html
            │   ├── catalog/
            │   │   ├── catalog.module.ts
            │   │   ├── product-list/
            │   │   │   ├── product-list.component.ts
            │   │   │   └── product-list.component.html
            │   │   └── product-detail/
            │   │       ├── product-detail.component.ts
            │   │       └── product-detail.component.html
            │   ├── cart/
            │   │   ├── cart.module.ts
            │   │   └── cart/
            │   │       ├── cart.component.ts
            │   │       └── cart.component.html
            │   ├── orders/
            │   │   ├── orders.module.ts
            │   │   └── order-history/
            │   │       ├── order-history.component.ts
            │   │       └── order-history.component.html
            │   └── admin/                          ← admin-only module (lazy-loaded)
            │       ├── admin.module.ts             ← routes: /admin, /admin/products/new, /admin/products/:id/edit
            │       ├── admin-product-list/
            │       │   ├── admin-product-list.component.ts
            │       │   └── admin-product-list.component.html
            │       └── product-form/
            │           ├── product-form.component.ts    ← shared create/edit form
            │           └── product-form.component.html
            └── shared/
                ├── shared.module.ts
                ├── models/
                │   ├── product.model.ts
                │   ├── order.model.ts
                │   ├── cart.model.ts
                │   └── user.model.ts
                └── components/
                    ├── search-bar/
                    │   ├── search-bar.component.ts
                    │   └── search-bar.component.html
                    └── pagination/
                        ├── pagination.component.ts
                        └── pagination.component.html
```

---

## 15. Implementation Milestones

Work through these in order. Each milestone is runnable and demonstrable before moving to the next.

### Milestone 1: Infrastructure
- `docker-compose.infra.yml`: Zookeeper, Kafka, PostgreSQL (×5 databases), Redis, Elasticsearch, Zipkin
- Run `docker-compose -f docker-compose.infra.yml up -d` and verify all containers healthy

### Milestone 2: Platform Services
- service-registry (Eureka) — verify dashboard at http://localhost:8761
- config-server — verify config served at http://localhost:8888/user-service/default

### Milestone 3: Auth Layer
- user-service — register, login, refresh, logout with JWT RS256
- api-gateway — routing + JwtAuthFilter
- Test: register → login → call a protected route → token expires → refresh → call again

### Milestone 4: Product Catalog
- product-service — CRUD + Redis cache-aside + Outbox Pattern
- Test: create product → verify in Redis → update → verify cache invalidated → verify outbox row created

### Milestone 5: Search
- search-service — Elasticsearch index + Kafka consumer for product events
- Start consuming from the outbox-published events from Milestone 4
- Test (SQL path): create product → GET /api/products?q=product-name → appears immediately
- Test (ES path): create product → wait 2s for pipeline → GET /api/search?q=product-name

### Milestone 6: Inventory
- inventory-service — stock management + Kafka consumer for order.placed
- Test: create inventory record → reserve → verify optimistic lock works under concurrent requests

### Milestone 7: Order Core (CQRS + Event Sourcing, no Saga yet)
- order-service — POST /api/orders writes event store + read model
- Test: place order → GET order by ID → verify read model matches events

### Milestone 8: Payment
- payment-service — idempotent charge + Kafka consumer + outbox publisher
- Test: POST /api/payments/charge with same idempotency key twice → verify single charge

### Milestone 9: Order Saga
- Add Saga orchestration to order-service
- Test happy path: order.placed → inventory.reserved → payment.completed → order.confirmed
- Test failure path: force payment failure → verify inventory.released → order.cancelled
- Trace full saga in Zipkin

### Milestone 10: Notifications
- notification-service — Kafka consumers sending email logs
- Verify notification logs appear for each saga outcome

### Milestone 11: Angular Frontend
- Implement in order: auth → product catalog → search → cart → order placement → order history
- Test complete purchase flow end-to-end in browser

### Milestone 12: Resilience & Observability
- Resilience4j circuit breakers on api-gateway routes
- Prometheus scraping from all services
- Grafana dashboards: RPS, error rate, Kafka consumer lag, JVM heap
- Test: shut down inventory-service → verify circuit breaker opens → start it again → verify circuit closes

---

## 16. Verification & Testing Plan

### Integration Test Checklist

1. **Auth flow:**
   ```
   POST /api/auth/register {"email":"test@test.com","password":"Pass123!"}
   POST /api/auth/login    → save access + refresh tokens
   GET  /api/products/1    with Authorization: Bearer {access_token}
   POST /api/auth/refresh  with {refreshToken}
   POST /api/auth/logout
   GET  /api/products/1    with old token → 401
   ```

2. **Product + Search pipeline (requires ADMIN token):**
   ```
   # Login as ADMIN user first and save the access token
   POST /api/auth/login {"email":"admin@ecommerce.com","password":"Admin@1234"}
   → save access_token

   # Optional: re-sync Elasticsearch if /api/search endpoint needs it
   POST /api/products/reindex
        Authorization: Bearer {admin_access_token}
   → 200 "Queued reindex for 12 products"
   → wait 2 seconds for OutboxPublisher → Kafka → search-service pipeline
   # NOTE: GET /api/products?q= (SQL search) works without this step

   POST /api/products {"name":"Gaming Laptop","price":1299.99}
        Authorization: Bearer {admin_access_token}
   → 201 Created

   POST /api/products {...}   (no token or USER token)
   → 403 {"error":"Access denied: ADMIN role required"}

   GET /api/products?q=gaming+laptop → returns product immediately (SQL ILIKE, no token needed)
   → wait 1-2 seconds for Kafka → Elasticsearch indexing (optional, for /api/search path)
   GET /api/products/{id} → first call: DB (slow), second call: Redis (fast)

   PUT /api/products/{id} {"price":1199.99}
       Authorization: Bearer {admin_access_token}
   → 200 OK, cache invalidated

   GET /api/products/{id} → DB again (cache miss), then cached again
   ```

3. **Happy-path saga:**
   ```
   POST /api/inventory {skuId, productId, quantity:100}
   POST /api/orders {items:[{skuId, quantity:2, unitPrice:1199.99}]}
   GET  /api/orders/{orderId} → status: CONFIRMED
   Open Zipkin → search trace for orderId → verify 4+ service hops
   ```

4. **Payment failure compensation:**
   ```
   Configure payment-service to fail 100% (test flag)
   POST /api/orders {items:[...]}
   GET  /api/orders/{orderId} → status: CANCELLED
   GET  /api/inventory/{skuId} → reserved_quantity should be back to 0
   ```

5. **Rate limiting:**
   ```
   Send 110 requests to GET /api/search in 60 seconds from same IP
   → first 100: 200 OK
   → requests 101-110: 429 Too Many Requests with Retry-After header
   ```

6. **Circuit breaker:**
   ```
   Stop inventory-service container
   Send 6 requests through order flow
   → first 5: 503 Service Unavailable
   → request 6: circuit opens, instant 503 with fallback response
   Start inventory-service
   → wait 10 seconds
   → next request: circuit half-opens, one probe request goes through
   → success: circuit closes
   ```

7. **Frontend E2E:**
   - `ng serve` at localhost:4200
   - Register new user → login
   - Browse catalog → search for product
   - Add to cart (persists on page refresh)
   - Place order → confirm success notification
   - View order history → status shows CONFIRMED

8. **Admin UI E2E:**
   - Login as `admin@ecommerce.com` / `Admin@1234`
   - Verify "Admin" nav link appears in navbar (gold/amber color)
   - Navigate to `/admin` → product table loads (up to 50 products)
   - Click "Add Product" → fill form → click "Create Product"
     - Button shows "Saving…" while POST is in flight
     - Redirects to `/admin` on success; new product visible at top
   - Click "Edit" on a product → form pre-filled → change price → "Update Product"
     - Redirects to `/admin` on success; updated price visible in table
   - Click "Delete" → confirm dialog → product removed from table immediately
   - Log out → log in as regular USER → verify no "Admin" link in navbar
   - Manually navigate to `http://localhost:4200/admin` → redirected to `/products` (AdminGuard)
   - `curl -X POST /api/products` with USER token → 403 Access denied (backend enforcement)
