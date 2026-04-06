# Finance Dashboard — Technical Decisions

## Overview

This document describes the key technical decisions made while building the Finance Dashboard backend assessment, including the choice of framework, database, authentication, architecture, and trade-offs considered at each step.

---

## 1. Framework — Spring Boot 3.4 + Java 21

**Choice:** Spring Boot 3.4 with Java 21.

**Why Spring Boot:**
Spring Boot is the industry-standard for building production-ready REST APIs in Java. It eliminates boilerplate configuration through convention-over-configuration, ships with an embedded Tomcat server (no separate deployment), and deeply integrates with the entire ecosystem needed here — JPA, Security, Validation, and Testing — all through a single dependency model (starters).

**Why Java 21:**
Java 21 is the current LTS (Long-Term Support) release. It brings virtual threads (Project Loom), modern language features (records, sealed classes, pattern matching), and is the natural pairing for Spring Boot 3.x which drops support for Java 17 on newer versions.

**Trade-off considered:**
A lightweight alternative like Quarkus or Micronaut would offer faster startup and a smaller memory footprint. However, Spring Boot was chosen for its maturity, richer documentation, and wider ecosystem — which is more appropriate for a backend assessment where the focus is on correctness and architecture rather than raw performance.

---

## 2. Database — MySQL with Hibernate ORM

**Choice:** MySQL 8 as the relational database, accessed via Spring Data JPA (Hibernate ORM).

**Why MySQL:**
- Industry-standard relational database, well-supported on every cloud platform.
- Financial data is inherently relational — users own records, records belong to users, aggregates are computed across rows. MySQL handles all of this natively.
- JPQL aggregation functions (`SUM`, `GROUP BY`, `YEAR()`, `MONTH()`) map cleanly onto MySQL's SQL dialect.

**Why Hibernate/JPA:**
JPA lets entities be defined as plain Java classes with annotations (`@Entity`, `@ManyToOne`), and Hibernate manages the SQL translation. This avoids writing raw DDL and makes schema evolution natural.

**DDL Strategy — `ddl-auto=update`:**
Set to `update` so Hibernate automatically adds new columns (like `user_id` added in this project) on restart without losing existing data. For a production system this would be replaced with **Flyway or Liquibase** migrations for full auditability.

**Trade-off considered:**
A NoSQL store like MongoDB would allow schemaless documents and could be easier to iterate on. However, since financial data requires strong consistency, referential integrity (foreign keys between users and records), and aggregation queries (totals, monthly trends), a relational database is the correct choice here.

---

## 3. Authentication — Stateless JWT (HS256)

**Choice:** JSON Web Tokens (JWT) using the HMAC-SHA256 (HS256) algorithm, implemented via the JJWT library.

**How it works:**
1. User registers (`POST /api/auth/register`) or logs in (`POST /api/auth/login`).
2. Server validates credentials and issues a signed JWT containing the user's email as the `subject` claim.
3. Client attaches the token on every subsequent request in the `Authorization: Bearer <token>` header.
4. A custom `JwtAuthFilter` (extending `OncePerRequestFilter`) intercepts each request, extracts and validates the token, and populates the Spring `SecurityContext` with the user's identity and role.

**Why JWT over sessions:**
- **Stateless:** No server-side session store needed — the token itself is self-contained and verifiable.
- **Scalable:** Any instance of the service can validate any token, making horizontal scaling trivial.
- **Standard:** JWT is the de-facto standard for REST API auth in microservices and SPA frontends.

**Security measures implemented:**
- Password stored as a BCrypt hash (via `BCryptPasswordEncoder`) — never plaintext.
- JWT secret injected from environment variable (`JWT_SECRET`) so it is never hardcoded in production.
- Token expiry set to 24 hours (configurable via `JWT_EXPIRATION_MS`).
- `UserDetailsServiceImpl` blocks login for inactive users at the `loadUserByUsername` level.

**Trade-off considered:**
JWT tokens cannot be revoked mid-lifetime without a server-side denylist (which reintroduces state). A short expiry (24h) is chosen as a pragmatic balance. In production, a Redis-backed token blacklist would be added for immediate revocation on logout or account deactivation.

---

## 4. Authorization — Method-Level RBAC with `@PreAuthorize`

**Choice:** Spring Security's method-level security using `@PreAuthorize` annotations, with three roles: `VIEWER`, `ANALYST`, `ADMIN`.

**Role permissions:**

| Action | VIEWER | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| View own records & summary | ✅ | — | — |
| View all records / summary | — | ✅ | ✅ |
| Create records (with userId) | — | — | ✅ |
| Update / Delete records | — | — | ✅ |
| List & manage users | — | — | ✅ |
| Toggle user active/inactive | — | — | ✅ |

**How data scoping works:**
Rather than relying solely on URL-level security (`antMatchers`), the scoping is embedded in the **service layer**:
- `VIEWER` → `resolveUserId()` always returns their own ID, forcing all queries to be filtered to `WHERE user_id = <self>`.
- `ANALYST` → passes an optional `userId` filter; if omitted, sees all records.
- `ADMIN` → same as Analyst but also has write access.

This design means even if a Viewer crafts a request with `?userId=5`, the service silently ignores it and returns only their own data — the scoping is not bypassable by the client.

**Trade-off considered:**
URL-level security (`antMatchers` per role) is simpler to understand but becomes unmanageable as business logic grows (e.g., "same endpoint, different data based on role"). Method-level `@PreAuthorize` + service-layer scoping keeps all access logic in one place and is explicit, auditable, and testable.

---

## 5. Project Architecture — Layered MVC

**Choice:** Classic 4-layer Spring architecture: Controller → Service → Repository → Database.

```
┌──────────────────────────────────┐
│         REST Controllers          │  ← HTTP in/out, input validation
├──────────────────────────────────┤
│           Service Layer           │  ← Business logic, RBAC scoping
├──────────────────────────────────┤
│         Repository Layer          │  ← Data access, JPQL queries
├──────────────────────────────────┤
│        MySQL / Hibernate          │  ← Persistence
└──────────────────────────────────┘
```

**Key design choices within layers:**

**Controllers** are thin — they only handle HTTP concerns (status codes, request parsing, passing `Authentication`). All logic lives in services.

**Services** hold all business rules:
- `AuthService` — registration, login, JWT issuance.
- `RecordService` — CRUD with ADMIN-only write guards, Viewer data scoping.
- `DashboardService` — aggregation queries (summary, categories, monthly trends), scoped per role.
- `UserService` — user management, active/inactive toggle.

**Repositories** use Spring Data JPA with custom JPQL queries where needed. The key decision was to avoid `JpaSpecificationExecutor` and the JPA Criteria API (which is verbose and hard to read) in favour of **nullable JPQL query parameters**:

```sql
AND (:userId IS NULL OR r.user.id = :userId)
AND (:type   IS NULL OR r.type   = :type)
```

This achieves fully optional dynamic filtering in a single, readable query without any runtime predicate building.

**DTOs** (Data Transfer Objects) are used for every API boundary — the JPA entities are never serialized directly. This decouples the API contract from the database schema.

---

## 6. Data Modeling — Dual User Keys on FinancialRecord

**Choice:** `FinancialRecord` carries two user foreign keys:
- `created_by_id` — who *entered* the record into the system (audit trail).
- `user_id` — who the record *belongs to* (the data owner).

**Why two keys:**
This separation is important for an Admin-operated system. An Admin creates records and assigns them to a specific user (`userId`). If only one field existed, you could not distinguish "this was created by Admin" from "this belongs to the Viewer user".

**Soft deletes:**
Both `User` and `FinancialRecord` use a `deletedAt` timestamp for soft deletion rather than physical `DELETE`. This preserves data history, allows recovery, and prevents orphaned foreign keys. All queries always filter `WHERE deletedAt IS NULL`.

---

## 7. Error Handling — `@RestControllerAdvice`

**Choice:** A centralized `GlobalExceptionHandler` using `@RestControllerAdvice` that maps domain exceptions to consistent JSON error responses.

Every error response follows the same structure:
```json
{
  "timestamp": "2026-04-06T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Record not found with id: 99",
  "path": "/api/records/99"
}
```

**Exceptions handled:**
| Exception | HTTP Status |
|---|---|
| `MethodArgumentNotValidException` | 400 (with per-field errors) |
| `IllegalArgumentException` | 400 |
| `AuthenticationException` | 401 |
| `AccessDeniedException` | 403 |
| `ResourceNotFoundException` | 404 |
| `ConflictException` | 409 |
| `Exception` (catch-all) | 500 |

This means the client always receives a structured, predictable error regardless of what fails.

---

## 8. Testing — Spring Boot Integration Tests

**Choice:** `@SpringBootTest` with `MockMvc` for full end-to-end integration testing against a real (H2 in-memory or MySQL) database.

Tests cover:
- Auth flows (register, login, duplicate email, wrong password).
- Record CRUD with role enforcement (Viewer blocked from create, Analyst blocked from delete).
- Dashboard access control (Viewer blocked from categories endpoint).
- User management (Admin can list users, Viewer cannot).
- Unauthenticated access returns 401.

**Trade-off considered:**
Unit tests with mocked dependencies are faster and more isolated. Integration tests were prioritised here because the access-control logic spans the security filter, `@PreAuthorize` in the service, and service-layer data scoping simultaneously — mocking any one of those layers would miss cross-layer bugs.

---

## Summary of Key Trade-offs

| Decision | Chosen | Alternative | Reason |
|---|---|---|---|
| Framework | Spring Boot | Quarkus / Micronaut | Maturity, ecosystem |
| Database | MySQL | MongoDB | Relational data, joins, aggregates |
| Auth | Stateless JWT | Session cookies | Scalability, SPA-friendly |
| Authorization | Method-level RBAC | URL-level antMatchers | Fine-grained, data-scoping friendly |
| Filtering | JPQL nullable params | JPA Specification/Criteria API | Simpler, more readable |
| Token Revocation | Expiry only (24h) | Redis denylist | Pragmatic for assessment scope |
| Schema migration | `ddl-auto=update` | Flyway / Liquibase | Rapid development; prod would use migrations |
| Soft delete | `deletedAt` timestamp | Physical DELETE | Data preservation, audit trail |
