# Fluxx

**Fluxx** is a self-hosted, event-notified feature flag management system built on Java 21 and Spring Boot 4. Engineering teams use Fluxx to create, toggle, and target feature flags across projects — changes propagate to all connected SDK clients in real time via Server-Sent Events (SSE), with a measured end-to-end latency of **~182 ms** on a single-region local setup and horizontal scalability to an unbounded number of SDK clients across multiple backend instances.

Fluxx is an **event-notified system**: flag state is the source of truth in MySQL, and every mutation (create, update, delete) emits a `FlagEvent` that flows through a Redis Pub/Sub channel to all subscribers, which fan-out the change to connected SSE clients. It is not purely event-sourced — the relational database holds authoritative state — but the event log provides a full immutable audit trail of every flag change.

---

## What It Does

- **Project management** — create isolated projects, each issued a unique HMAC-SHA256 API key derived from the project ID and a server-side secret
- **Feature flags** — create, toggle, and delete flags per project with three types: `KILL_SWITCH`, `ROLLOUT`, `TARGETED`
- **Activation strategies** — attach typed strategies (`DEFAULT`, `ROLLOUT`, `USER_ID`) with arbitrary key-value parameters to control targeting logic
- **Real-time propagation** — flag mutations publish events to Redis; every connected SSE client across all backend nodes receives the update within ~182 ms end-to-end (locally measured: 118 ms DB write + Redis publish, 30 ms Redis pub/sub transit, 34 ms SSE send)
- **Audit log** — every flag lifecycle event (`FLAG_CREATED`, `FLAG_ENABLED`, `FLAG_ARCHIVED`, `STRATEGY_ADDED`, `STRATEGY_UPDATED`, `STRATEGY_REMOVED`) is persisted with a full JSON snapshot of state after the change
- **API key authentication** — every project is issued a unique HmacSHA256-signed API key on creation; SDK clients present this key as a Bearer token and Spring Security validates it on every request, rejecting unknown keys with `401 Unauthorized`
- **Horizontal scaling** — backend instances are stateless with respect to flag data; each node subscribes independently to Redis Pub/Sub, so any number of instances behind a load balancer correctly fan-out events to their own SSE connection pools

---

## Architecture

```
  ┌──────────────────────┐
  │  Dashboard / Admin   │
  │  REST Client         │
  └──────────┬───────────┘
             │ HTTP + Bearer <api-key>
             ▼
  ┌──────────────────────────────────────────────────────────┐
  │                     Fluxx Backend                        │
  │                  (Spring Boot 4 / Java 21)               │
  │                                                          │
  │  Spring Security Filter Chain                            │
  │  └─ ApiKeyAuthFilter (HmacSHA256 validation → 401)       │
  │                                                          │
  │  ProjectController   FeatureFlagController               │
  │       │                      │    StrategyController     │
  │  ProjectService         FeatureFlagService  │            │
  │  HmacSHA256 keygen           │    StrategyService        │
  │                              └────────┬─────┘            │
  │                                       │                  │
  │                              FlagEventService            │
  │                              ├─ persist FlagEvent (JPA)  │
  │                              └─ Redis PUBLISH ──────────►├──┐
  │                                                          │  │
  │  RedisMessageListenerContainer                           │  │
  │  └─ FlagEventSubscriber ◄─────────────────── Redis ◄────┘  │
  │       └─ SseEmitterService.broadcast()                   │  │
  └──────────────────────────────────────────────────────────┘  │
             │  SSE (text/event-stream)           Redis 7        │
             │                                  Pub/Sub ◄────────┘
    ┌────────┴──────────────────────────┐    Channel: flag-events
    ▼                ▼                  ▼
  SDK Client 1   SDK Client 2  ...  SDK Client N
  (SSE open)     (SSE open)         (SSE open)


  ┌──────────────────────────────────────┐
  │           MySQL 8.4                  │
  │  tables: projects                    │
  │          feature_flags               │
  │          activation_strategy         │
  │          strategy_params             │
  │          flag_event (audit log)      │
  └──────────────────────────────────────┘
```

### Event flow timing (single-region, measured locally)

```
t=0 ms    HTTP request hits FeatureFlagService / StrategyService
t=118 ms  DB write committed + Redis PUBLISH sent        (+118 ms)
t=148 ms  FlagEventSubscriber receives from Redis        ( +30 ms)
t=182 ms  SseEmitterService broadcasts to all clients    ( +34 ms)
```

In cross-region deployments the Redis pub/sub transit (~30 ms) and SSE write (~34 ms) legs will increase with network RTT. Use Linux `tc netem` or Toxiproxy to simulate this locally before deploying.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Web | Spring MVC (`spring-boot-starter-webmvc`), `SseEmitter` |
| ORM | Spring Data JPA + Hibernate 6 (`@Entity`, `@GeneratedValue(UUID)`, `@OneToMany`, `@CreationTimestamp`, `@UpdateTimestamp`, `@JdbcTypeCode(JSON)`) |
| Database | MySQL 8.4 (`mysql-connector-j`), schema managed via `ddl-auto=update` |
| Cache / Pub-Sub | Redis 7 (`spring-boot-starter-data-redis-reactive`), `RedisTemplate`, `RedisMessageListenerContainer` |
| Serialization | Jackson (`jackson-databind`, `jackson-datatype-jsr310`) |
| Security | Spring Security (`spring-boot-starter-security`) — active; `ApiKeyAuthFilter` validates `Bearer <api-key>` on all endpoints; HmacSHA256 key generation and verification via `javax.crypto.Mac` |
| Boilerplate | Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`) |
| Build | Gradle 8, `.env` file loaded automatically at `bootRun` |
| Observability | Spring Boot Actuator, SLF4J structured logging with per-operation timing |
| Container | Docker Compose (Redis 7-alpine, MySQL 8.4) |

---

## Data Model

```
projects
  id (UUID PK)
  name
  api_key (HmacSHA256 hex, unique)
  created_at

feature_flags
  id (UUID PK)
  project_id (FK → projects)
  name
  flag_type  KILL_SWITCH | ROLLOUT | TARGETED
  enabled
  updated_at

activation_strategy
  id (UUID PK)
  flag_id (FK → feature_flags)
  strategy_type  DEFAULT | ROLLOUT | USER_ID

strategy_params
  id (UUID PK)
  strategy_id (FK → activation_strategy)
  key
  value

flag_event  (append-only audit log)
  id (UUID PK)
  flag_id
  event_type  FLAG_CREATED | FLAG_ENABLED | FLAG_DISABLED |
              FLAG_ARCHIVED | STRATEGY_ADDED | STRATEGY_UPDATED | STRATEGY_REMOVED
  after_state (JSON snapshot of entity post-change)
  created_at
```

---

## API Reference

All endpoints require `Authorization: Bearer <api-key>`. The API key is returned when a project is created. Requests with a missing or invalid key receive `401 Unauthorized`.

### Projects
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/projects` | Create project; response includes the HmacSHA256 API key |
| `GET` | `/api/projects` | List all projects |
| `GET` | `/api/projects/{id}` | Get project by ID |
| `PUT` | `/api/projects/{id}?name=` | Rename project |
| `DELETE` | `/api/projects/{id}` | Delete project |

### Feature Flags
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/feature-flags` | Create flag |
| `GET` | `/api/feature-flags` | List all flags |
| `GET` | `/api/feature-flags/project/{projectId}` | Flags for a project |
| `GET` | `/api/feature-flags/{id}` | Get flag by ID |
| `PUT` | `/api/feature-flags/{id}` | Toggle enabled state |
| `DELETE` | `/api/feature-flags/{id}` | Archive and delete flag |

### Strategies
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/strategies` | Add strategy to a flag |
| `GET` | `/api/strategies` | List all strategies |
| `GET` | `/api/strategies/{id}` | Get strategy by ID |
| `GET` | `/api/strategies/flag/{flagId}` | Strategy for a flag |
| `GET` | `/api/strategies/type/{strategyType}` | Strategies by type |
| `PUT` | `/api/strategies/{id}` | Update strategy |
| `DELETE` | `/api/strategies/{id}` | Remove strategy |

### SSE Subscription (SDK)
| Method | Path | Header | Description |
|---|---|---|---|
| `GET` | `/api/pubsub/subscribe` | `Authorization: Bearer <api-key>` | Open SSE stream; Spring Security validates the key and maps it to the project; receives `flag-event` messages on every flag/strategy change |

**SSE event payload:**
```json
{
  "flagId": "1237972d-28d2-48fe-bcbc-e6b62b0b3967",
  "eventType": "FLAG_CREATED",
  "Message": "New update"
}
```

---

## Setup

### Prerequisites
- Java 21+
- Docker and Docker Compose
- Gradle (or use the included `./gradlew` wrapper)

### 1. Clone and configure

```bash
git clone <repo-url>
cd fluxx
```

Create a `.env` file in the project root:

```env
API_KEY_SECRET=your-secret-key-here
```

`API_KEY_SECRET` is the server-side secret fed into `HmacSHA256` when signing project API keys. Every backend instance must use the same value — if they differ, API keys generated on one instance will fail validation on another.

### 2. Start infrastructure

```bash
docker-compose up -d
```

This starts:
- **MySQL 8.4** on `localhost:3307` (database: `fluxx`, user: `user1`, password: `user1`)
- **Redis 7** on `localhost:6380` with AOF persistence enabled

Hibernate will auto-create all tables on first boot via `ddl-auto=update`.

### 3. Run the application

```bash
./gradlew bootRun
```

The server starts on `http://localhost:8080`. On startup, Fluxx pings Redis and logs subscription status for the `flag-events` channel.

### 4. Verify

```bash
# Create a project — response includes id and api_key
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "my-app"}'

# Response: { "id": "<projectId>", "api_key": "<hmac-sha256-hex>", ... }
# Save api_key as API_KEY for subsequent requests

# Open an SSE stream (in a separate terminal) — authenticated with the project API key
curl -N http://localhost:8080/api/pubsub/subscribe \
  -H "Authorization: Bearer <api_key>"

# Create a flag — requires the same API key; SSE stream receives a flag-event within ~182 ms
curl -X POST http://localhost:8080/api/feature-flags \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <api_key>" \
  -d '{"projectId":"<projectId>","name":"dark-mode","flag_type":"KILL_SWITCH","enabled":false}'
```

A request without a valid `Authorization` header returns:
```
HTTP/1.1 401 Unauthorized
```

### 5. Horizontal scaling

Start multiple instances on different ports. All instances share the same MySQL and Redis:

```bash
SERVER_PORT=8081 ./gradlew bootRun
SERVER_PORT=8082 ./gradlew bootRun
```

Place a load balancer (nginx, AWS ALB) in front. Each instance independently subscribes to Redis `flag-events` and maintains its own SSE connection pool — flag changes published by any instance are received by all instances and forwarded to all connected clients.

---

## Pros

- **Low-latency real-time propagation** — ~182 ms end-to-end locally; Redis Pub/Sub adds minimal overhead over direct HTTP polling and eliminates the need for clients to poll
- **Horizontally scalable** — stateless HTTP tier scales to N instances behind a load balancer; Redis fan-out ensures every connected SDK client across every instance receives every event
- **Durable audit log** — every flag change is persisted as an immutable `FlagEvent` with a JSON snapshot of state, enabling full history replay and debugging
- **Simple, stateless security model** — HmacSHA256 API keys are deterministically derived from `projectId + secret`; Spring Security validates every request without a database lookup, making auth overhead negligible and scaling linearly with instance count
- **Hibernate schema management** — `ddl-auto=update` keeps the schema in sync during development with zero migration boilerplate; switch to Flyway/Liquibase for production
- **Lean event bus** — Redis Pub/Sub is the right tool here: SSE clients that miss events reconnect and fetch current state via REST, so the ephemeral nature of Redis Pub/Sub is not a liability
- **Typed strategy system** — `KILL_SWITCH`, `ROLLOUT`, and `TARGETED` flags with parameterized strategies (`DEFAULT`, `ROLLOUT`, `USER_ID`) give fine-grained targeting without bespoke logic in each SDK

## Cons

- **In-memory SSE connection pool** — `SseEmitterService` stores emitters in a `ConcurrentHashMap` per instance; there is no cross-instance registry, so sticky sessions or a shared registry (e.g., Redis) are needed if you need to push to a specific client from a specific instance
- **Redis Pub/Sub is fire-and-forget** — messages in transit during a Redis restart or network partition are lost; a downstream service that must not miss events would need Kafka or Redis Streams instead
- **No built-in SDK** — clients must implement SSE reconnect logic and state hydration on connect themselves
- **Single Redis node** — the current setup uses a single Redis instance; high availability requires Redis Sentinel or Redis Cluster, which changes the pub/sub topology
- **MySQL horizontal scaling requires sharding or read replicas** — `ddl-auto=update` and UUID PKs are compatible with read replicas, but write scaling requires a proxy layer (e.g., ProxySQL, Vitess) or explicit sharding by `project_id`
- **API key rotation requires secret rotation** — because keys are derived deterministically from `projectId + API_KEY_SECRET`, rotating a compromised key means rotating the server secret and invalidating all existing keys simultaneously; per-project key revocation is not supported without storing keys in the database
- **No SDK-side caching** — if the SSE connection drops, the SDK has no local snapshot of flag state until it reconnects and re-fetches via REST; a local cache with stale-read fallback would improve resilience
