# Finance Dashboard — Spring Boot Backend

A production-ready REST API backend for a finance dashboard system built with Spring Boot 3.2, PostgreSQL, and JWT authentication. Supports role-based access control with three user roles, full financial record management, and aggregated dashboard analytics.


---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.3 |
| Security | Spring Security 6 + JWT (jjwt 0.12.3) |
| Database | PostgreSQL 14+ |
| ORM | Spring Data JPA / Hibernate 6 |
| Validation | Jakarta Bean Validation |
| Build Tool | Maven 3.8+ |

---

## Features

- JWT-based stateless authentication
- Role-based access control (ADMIN / ANALYST / VIEWER)
- Full CRUD for financial transactions (income & expense)
- Soft delete — deleted records are hidden, not removed
- Filtering by type, category, and date range
- Pagination on all list endpoints
- Dashboard summary — totals, net balance, category breakdown
- Monthly trend analytics
- Global exception handling with clean error responses
- Input validation with field-level error messages

---

## Roles & Permissions

| Action | VIEWER | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| View transactions | ✅ | ✅ | ✅ |
| Create transactions | ❌ | ✅ | ✅ |
| Update transactions | ❌ | ❌ | ✅ |
| Delete transactions | ❌ | ❌ | ✅ |
| View dashboard summary | ❌ | ✅ | ✅ |
| View monthly trends | ❌ | ✅ | ✅ |
| Manage users | ❌ | ❌ | ✅ |

---

## Project Structure

```
src/main/java/com/finance/dashboard/
├── config/
│   ├── JwtUtil.java              # JWT generation & validation
│   ├── SecurityConfig.java       # Spring Security + CORS config
│   └── DataSeeder.java           # App startup hook
├── controller/
│   ├── AuthController.java       # Register & login
│   ├── TransactionController.java
│   ├── DashboardController.java
│   └── UserController.java
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── AuthResponse.java
│   ├── TransactionRequest.java
│   ├── TransactionResponse.java
│   ├── UserResponse.java
│   ├── UpdateUserRequest.java
│   ├── DashboardSummaryResponse.java
│   ├── MonthlyTrendResponse.java
│   └── ApiResponse.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   └── ConflictException.java
├── filter/
│   └── JwtAuthFilter.java        # Reads JWT from every request
├── model/
│   ├── User.java
│   └── Transaction.java
├── repository/
│   ├── UserRepository.java
│   └── TransactionRepository.java
└── service/
    ├── AuthService.java
    ├── UserService.java
    ├── TransactionService.java
    └── DashboardService.java
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/finance-backend.git
cd finance-backend
```

### 2. Create the database

```sql
CREATE DATABASE finance_db;
```

### 3. Configure environment

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/finance_db
spring.datasource.username=postgres
spring.datasource.password=yourpassword
jwt.secret=your-secret-key-must-be-at-least-32-characters-long
```

Or set environment variables directly:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/finance_db
DB_USERNAME=postgres
DB_PASSWORD=yourpassword
JWT_SECRET=your-secret-key-here
```

### 4. Run the application

```bash
mvn spring-boot:run
```

Server starts at `http://localhost:8080`

### 5. Run tests

```bash
mvn test
```

---

## API Reference

All responses follow this structure:

```json
{
  "success": true,
  "message": "optional message",
  "data": { }
}
```

---

### Authentication

#### Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "pass1234"
}
```

**Response `201 Created`**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "id": "uuid",
    "email": "john@example.com",
    "fullName": "John Doe",
    "role": "VIEWER"
  }
}
```

---

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "pass1234"
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "role": "VIEWER"
  }
}
```

> Use the token in all subsequent requests:  
> `Authorization: Bearer <token>`

---

### Transactions

#### Create Transaction
```http
POST /api/transactions
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2024-03-01",
  "notes": "March salary"
}
```

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "amount": 5000.00,
    "type": "INCOME",
    "category": "Salary",
    "date": "2024-03-01",
    "notes": "March salary",
    "createdByName": "John Doe",
    "createdAt": "2024-03-01T10:00:00"
  }
}
```

---

#### List Transactions
```http
GET /api/transactions
Authorization: Bearer <token>
```

**Query Parameters**

| Parameter | Type | Description | Example |
|---|---|---|---|
| `type` | string | Filter by INCOME or EXPENSE | `?type=INCOME` |
| `category` | string | Filter by category name | `?category=Salary` |
| `from` | date | Start date (yyyy-MM-dd) | `?from=2024-01-01` |
| `to` | date | End date (yyyy-MM-dd) | `?to=2024-12-31` |
| `page` | int | Page number (default 0) | `?page=0` |
| `size` | int | Page size (default 20, max 100) | `?size=10` |

**Example Requests**
```http
GET /api/transactions?type=INCOME
GET /api/transactions?category=Salary
GET /api/transactions?from=2024-03-01&to=2024-03-31
GET /api/transactions?type=EXPENSE&category=Rent
GET /api/transactions?page=0&size=5
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "content": [ { "id": "...", "amount": 5000.00, "type": "INCOME", "category": "Salary" } ],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "last": false
  }
}
```

---

#### Get Single Transaction
```http
GET /api/transactions/{id}
Authorization: Bearer <token>
```

---

#### Update Transaction *(Admin only)*
```http
PUT /api/transactions/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 5500.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2024-03-01",
  "notes": "Updated salary"
}
```

---

#### Delete Transaction *(Admin only)*
```http
DELETE /api/transactions/{id}
Authorization: Bearer <token>
```

> Soft delete — record is hidden from all queries but remains in the database.

---

### Dashboard

#### Summary
```http
GET /api/dashboard/summary
Authorization: Bearer <token>
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "totalIncome": 6200.00,
    "totalExpenses": 1850.00,
    "netBalance": 4350.00,
    "totalTransactions": 10,
    "incomeByCategory": {
      "Salary": 5000.00,
      "Freelance": 1200.00
    },
    "expenseByCategory": {
      "Rent": 1500.00,
      "Food": 350.00
    },
    "recentTransactions": [ ]
  }
}
```

---

#### Monthly Trends
```http
GET /api/dashboard/trends?year=2024
Authorization: Bearer <token>
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": [
    { "month": 1, "monthName": "Jan", "income": 0.00, "expenses": 0.00, "net": 0.00 },
    { "month": 2, "monthName": "Feb", "income": 0.00, "expenses": 0.00, "net": 0.00 },
    { "month": 3, "monthName": "Mar", "income": 6200.00, "expenses": 1850.00, "net": 4350.00 }
  ]
}
```

---

#### Category Breakdown
```http
GET /api/dashboard/categories?type=EXPENSE
Authorization: Bearer <token>
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "Rent": 1500.00,
    "Food": 350.00
  }
}
```

---

### User Management *(Admin only)*

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users` | List all users |
| GET | `/api/users/active` | List active users |
| GET | `/api/users/{id}` | Get user by ID |
| POST | `/api/users` | Create user with any role |
| PUT | `/api/users/{id}` | Update role / name / status |
| DELETE | `/api/users/{id}` | Delete user |

#### Create User
```http
POST /api/users
Authorization: Bearer <token>
Content-Type: application/json

{
  "fullName": "New Analyst",
  "email": "analyst@example.com",
  "password": "pass1234",
  "role": "ANALYST"
}
```

#### Update User
```http
PUT /api/users/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "role": "VIEWER",
  "active": false
}
```

---

## Error Responses

| Status | When |
|---|---|
| `400` | Invalid input or validation failed |
| `401` | Missing or expired JWT token |
| `403` | Authenticated but insufficient role |
| `404` | Resource not found or soft-deleted |
| `409` | Email already registered |
| `500` | Unexpected server error |

**Validation Error Example `400`**
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "amount": "Amount must be greater than 0",
    "category": "Category is required",
    "date": "Date is required"
  }
}
```

**Access Denied Example `403`**
```json
{
  "success": false,
  "message": "Access denied: you do not have permission to perform this action",
  "data": null
}
```

---

## Deployment

### Deploy Backend to Railway

1. Push your code to GitHub
2. Go to [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub**
3. Select your repository
4. Click **Add Plugin** → **PostgreSQL** — Railway auto-injects the database URL
5. Go to **Variables** tab and add:

```
JWT_SECRET        = your-long-random-secret-key
CORS_ALLOWED_ORIGINS = https://your-frontend.vercel.app
```

6. Railway builds and deploys automatically on every push

---


## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DATABASE_URL` | PostgreSQL JDBC connection string | `jdbc:postgresql://localhost:5432/finance_db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | Secret key for signing JWT tokens | *(required — min 32 chars)* |
| `JWT_EXPIRATION` | Token expiry in milliseconds | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins | `http://localhost:3000` |

---

## Assumptions & Design Decisions

- **Soft delete** — transactions set `deleted = true` rather than being physically removed, preserving data integrity and audit history
- **Stateless auth** — no sessions; every request must carry a valid JWT in the `Authorization` header
- **Public registration** — anyone can register but receives the VIEWER role by default; only an ADMIN can assign ANALYST or ADMIN roles via `POST /api/users`
- **Pagination** — all list endpoints are paginated with a maximum of 100 records per page to prevent abuse
- **Separate filter queries** — the transaction repository uses dedicated JPQL queries per filter combination instead of a single nullable query, avoiding a PostgreSQL type-cast limitation with null enum parameters

---

## Author

**Balaji** — [github.com/Balaji](https://github.com/Balaji849)
