# Finance Dashboard Backend

A production-quality REST API built with **Spring Boot 3.4**, **Java 21**, and **MySQL**, featuring JWT authentication and Role-Based Access Control (RBAC).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.4.4 |
| Language | Java 21 |
| Database | MySQL 8+ (H2 for tests) |
| ORM | Spring Data JPA / Hibernate |
| Auth | Spring Security 6 + JWT (JJWT 0.12) |
| Validation | Jakarta Bean Validation |
| Boilerplate | Lombok |

---

## Setup & Running

### Prerequisites
- Java 21+
- Maven (or use `mvnw`)
- MySQL 8+ running on `localhost:3306`

### 1. Create the database
```sql
CREATE DATABASE zorvyn;
```

### 2. Configure credentials (optional)

Defaults in `application.properties`:
```
spring.datasource.username=root
spring.datasource.password=231309
```

Override via environment variables for any environment:
```powershell
$env:MYSQL_USER="root"; $env:MYSQL_PASS="yourpass"; $env:JWT_SECRET="yourSecret"
```

### 3. Run
```powershell
cd "d:\Projects\zorvyn assesment\demo"
.\mvnw spring-boot:run
```

Server starts at `http://localhost:8080`.

### 4. Run Tests (no MySQL needed — uses H2)
```powershell
.\mvnw test
```

---

## Role Permissions

| Endpoint | VIEWER | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| `POST /api/auth/register` | ✅ | ✅ | ✅ |
| `POST /api/auth/login` | ✅ | ✅ | ✅ |
| `GET /api/records` | ✅ | ✅ | ✅ |
| `GET /api/records/{id}` | ✅ | ✅ | ✅ |
| `POST /api/records` | ❌ | ✅ | ✅ |
| `PATCH /api/records/{id}` | ❌ | ✅ | ✅ |
| `DELETE /api/records/{id}` | ❌ | ❌ | ✅ |
| `GET /api/dashboard/summary` | ✅ | ✅ | ✅ |
| `GET /api/dashboard/recent` | ✅ | ✅ | ✅ |
| `GET /api/dashboard/categories` | ❌ | ✅ | ✅ |
| `GET /api/dashboard/trends` | ❌ | ✅ | ✅ |
| `GET /api/users` | ❌ | ❌ | ✅ |
| `GET /api/users/{id}` | ❌ | ❌ | ✅ |
| `PATCH /api/users/{id}` | ❌ | ❌ | ✅ |
| `DELETE /api/users/{id}` | ❌ | ❌ | ✅ |

---

## API Reference

All protected endpoints require:
```
Authorization: Bearer <jwt_token>
```

### Auth

#### `POST /api/auth/register`
Register a new user. Default role = **VIEWER**.

**Request**
```json
{ "name": "Alice", "email": "alice@example.com", "password": "secret123" }
```
**Response** `201 Created`
```json
{ "token": "eyJ...", "type": "Bearer", "email": "alice@example.com", "role": "VIEWER" }
```

---

#### `POST /api/auth/login`
**Request**
```json
{ "email": "alice@example.com", "password": "secret123" }
```
**Response** `200 OK` — same shape as register.

---

### Financial Records

#### `POST /api/records` *(ANALYST, ADMIN)*
```json
{
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2024-03-01",
  "notes": "March salary"
}
```
**Response** `201 Created`
```json
{
  "id": 1,
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2024-03-01",
  "notes": "March salary",
  "createdBy": "alice@example.com",
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2024-03-01T10:00:00"
}
```

---

#### `GET /api/records` *(All roles)*
Supports pagination and filtering via query params:

| Param | Type | Description |
|---|---|---|
| `type` | `INCOME` / `EXPENSE` | Filter by type |
| `category` | string | Partial case-insensitive match |
| `from` | `YYYY-MM-DD` | Date range start |
| `to` | `YYYY-MM-DD` | Date range end |
| `page` | int (default 0) | Page number |
| `size` | int (default 20) | Page size |
| `sort` | `field,direction` (default `date,desc`) | Sort |

**Example:** `GET /api/records?type=INCOME&from=2024-01-01&page=0&size=10`

**Response** `200 OK` — Spring `Page<RecordDto>`:
```json
{
  "content": [ { ... } ],
  "totalElements": 42,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

---

#### `GET /api/records/{id}` *(All roles)*
**Response** `200 OK` — single `RecordDto`

---

#### `PATCH /api/records/{id}` *(ANALYST, ADMIN)*
All fields optional. Only provided fields are updated.
```json
{ "amount": 6000.00, "notes": "Updated" }
```

---

#### `DELETE /api/records/{id}` *(ADMIN only)*
Soft-delete. **Response** `204 No Content`

---

### Dashboard

#### `GET /api/dashboard/summary` *(All roles)*
```json
{
  "totalIncome": 15000.00,
  "totalExpense": 8000.00,
  "netBalance": 7000.00,
  "totalRecords": 25
}
```

---

#### `GET /api/dashboard/categories` *(ANALYST, ADMIN)*
```json
[
  { "category": "Food", "type": "EXPENSE", "total": 1200.00 },
  { "category": "Salary", "type": "INCOME", "total": 15000.00 }
]
```

---

#### `GET /api/dashboard/trends` *(ANALYST, ADMIN)*
Returns monthly totals for the last 12 months.
```json
[
  { "year": 2024, "month": 1, "type": "INCOME", "total": 5000.00 },
  { "year": 2024, "month": 1, "type": "EXPENSE", "total": 2000.00 }
]
```

---

#### `GET /api/dashboard/recent` *(All roles)*
Returns the 10 most recent records (sorted by date desc, then createdAt desc).

---

### User Management *(ADMIN only)*

#### `GET /api/users`
List all active (non-deleted) users.

#### `GET /api/users/{id}`
Get user by ID.

#### `PATCH /api/users/{id}`
Update role and/or active status:
```json
{ "role": "ANALYST", "active": true }
```

#### `DELETE /api/users/{id}`
Soft-delete user. **Response** `204 No Content`

---

## Error Response Format

All errors use a consistent JSON shape:
```json
{
  "timestamp": "2024-03-01T10:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Request has invalid fields",
  "path": "/api/records",
  "fieldErrors": {
    "amount": "Amount is required",
    "category": "Category is required"
  }
}
```

| Status | Cause |
|---|---|
| 400 | Validation failure or bad input |
| 401 | Missing/invalid/expired JWT |
| 403 | Role insufficient for the operation |
| 404 | Resource not found |
| 409 | Conflict (e.g. duplicate email) |
| 500 | Unexpected server error |

---

## Design Decisions & Assumptions

1. **New users default to VIEWER** — only an Admin can elevate a role via `PATCH /api/users/{id}`.
2. **Soft delete everywhere** — both `User` and `FinancialRecord` have a `deletedAt` timestamp. Deleted records are invisible to all API responses but preserved in the DB for audit.
3. **JWT is stateless** — no session store needed. Tokens expire in 24 hours (configurable via `app.jwt.expiration-ms`).
4. **`JpaSpecificationExecutor`** is used for financial records to support any combination of filters without writing N query methods.
5. **`@PreAuthorize` at service layer** — role enforcement is not just in the controller; business logic is protected even if controllers are extended.
6. **H2 for tests** — integration tests run fully without a MySQL installation. Spring auto-creates the schema from JPA entities on `create-drop`.
7. **`PATCH` semantics** — only non-null fields in update requests are applied, enabling partial updates.
8. **Amount stored as `BigDecimal`** — avoids floating-point precision issues for financial data.
9. **Category is a free-text string** — not an enum, allowing flexible categorization without schema migrations.

---

## Project Structure

```
src/main/java/com/zorvyn/demo/
├── config/         SecurityConfig.java
├── controller/     AuthController, UserController, RecordsController, DashboardController
├── dto/            Request/Response DTOs (8 files)
├── entity/         User, FinancialRecord, Role, RecordType
├── exception/      GlobalExceptionHandler, ResourceNotFoundException, ConflictException
├── repository/     UserRepository, FinancialRecordRepository
├── security/       JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
└── service/        AuthService, UserService, RecordService, DashboardService
```
