# Finance Dashboard — Spring Boot Backend

A production-ready REST API backend for a finance dashboard system with JWT authentication,
role-based access control, financial record management, and summary analytics.

---

## Tech Stack

| Layer       | Technology                          |
|-------------|-------------------------------------|
| Language    | Java 17                             |
| Framework   | Spring Boot 3.2                     |
| Security    | Spring Security + JWT (jjwt 0.12)   |
| Database    | PostgreSQL (H2 for tests)           |
| ORM         | Spring Data JPA / Hibernate         |
| Validation  | Jakarta Bean Validation             |
| Build       | Maven                               |

---

## Roles & Permissions

| Action                        | VIEWER | ANALYST | ADMIN |
|-------------------------------|--------|---------|-------|
| View transactions (list/get)  | ✅     | ✅      | ✅    |
| Create transactions           | ❌     | ✅      | ✅    |
| Update transactions           | ❌     | ❌      | ✅    |
| Delete transactions           | ❌     | ❌      | ✅    |
| View dashboard summary        | ❌     | ✅      | ✅    |
| View monthly trends           | ❌     | ✅      | ✅    |
| View category breakdown       | ❌     | ✅      | ✅    |
| Manage users (CRUD)           | ❌     | ❌      | ✅    |

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### 1. Create the database
```sql
CREATE DATABASE finance_db;
```

### 2. Configure environment
Copy and edit the properties file:
```bash
cp src/main/resources/application.properties .env.local
```

Or set these environment variables:
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/finance_db
DB_USERNAME=postgres
DB_PASSWORD=yourpassword
JWT_SECRET=your-super-secret-key-at-least-32-chars-long
```

### 3. Run
```bash
mvn spring-boot:run
```

The server starts at `http://localhost:8080`.

On first start, three seed users are created automatically:

| Email                  | Password    | Role    |
|------------------------|-------------|---------|
| admin@finance.com      | admin123    | ADMIN   |
| analyst@finance.com    | analyst123  | ANALYST |
| viewer@finance.com     | viewer123   | VIEWER  |

### 4. Run tests
```bash
mvn test
```

---

## API Reference

All responses follow this envelope:
```json
{
  "success": true,
  "message": "optional message",
  "data": { ... }
}
```

### Auth

| Method | Endpoint             | Auth | Description        |
|--------|----------------------|------|--------------------|
| POST   | /api/auth/register   | No   | Create new account |
| POST   | /api/auth/login      | No   | Get JWT token      |

**Register**
```json
POST /api/auth/register
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "password": "secret123",
  "role": "VIEWER"   // optional, defaults to VIEWER
}
```

**Login**
```json
POST /api/auth/login
{
  "email": "jane@example.com",
  "password": "secret123"
}
```
Response includes `token`. Use it as: `Authorization: Bearer <token>`

---

### Transactions

| Method | Endpoint                  | Auth | Roles              | Description         |
|--------|---------------------------|------|--------------------|---------------------|
| GET    | /api/transactions         | Yes  | ALL                | List (with filters) |
| GET    | /api/transactions/{id}    | Yes  | ALL                | Get one             |
| POST   | /api/transactions         | Yes  | ANALYST, ADMIN     | Create              |
| PUT    | /api/transactions/{id}    | Yes  | ADMIN              | Update              |
| DELETE | /api/transactions/{id}    | Yes  | ADMIN              | Soft delete         |

**Query params for GET /api/transactions:**
```
category  — filter by category (case-insensitive)
type      — INCOME or EXPENSE
from      — start date (yyyy-MM-dd)
to        — end date   (yyyy-MM-dd)
page      — page number (default 0)
size      — page size   (default 20, max 100)
```

**Create / Update body:**
```json
{
  "amount": 1500.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2024-03-15",
  "notes": "March salary"
}
```

---

### Dashboard

| Method | Endpoint                      | Roles           | Description              |
|--------|-------------------------------|-----------------|--------------------------|
| GET    | /api/dashboard/summary        | ANALYST, ADMIN  | Overall totals + recent  |
| GET    | /api/dashboard/trends?year=   | ANALYST, ADMIN  | Monthly income/expense   |
| GET    | /api/dashboard/categories     | ANALYST, ADMIN  | Category breakdown       |

**Summary response:**
```json
{
  "totalIncome": 5000.00,
  "totalExpenses": 3200.00,
  "netBalance": 1800.00,
  "totalTransactions": 42,
  "incomeByCategory": { "Salary": 4500.00, "Freelance": 500.00 },
  "expenseByCategory": { "Rent": 1500.00, "Food": 800.00 },
  "recentTransactions": [ ... ]
}
```

---

### Users (Admin only)

| Method | Endpoint           | Description             |
|--------|--------------------|-------------------------|
| GET    | /api/users         | List all users          |
| GET    | /api/users/active  | List active users       |
| GET    | /api/users/{id}    | Get single user         |
| POST   | /api/users         | Create user (any role)  |
| PUT    | /api/users/{id}    | Update role/status/name |
| DELETE | /api/users/{id}    | Delete user             |

---

## Deployment

### Railway (recommended)

1. Push to GitHub
2. New Project → Deploy from GitHub repo
3. Add PostgreSQL plugin
4. Set environment variables:
   - `JWT_SECRET` → any long random string
   - `CORS_ALLOWED_ORIGINS` → your Vercel frontend URL
5. Railway injects `DATABASE_URL` automatically

### Environment Variables

| Variable              | Description                          | Default                     |
|-----------------------|--------------------------------------|-----------------------------|
| DATABASE_URL          | Full JDBC connection string          | jdbc:postgresql://localhost/finance_db |
| DB_USERNAME           | Database username                    | postgres                    |
| DB_PASSWORD           | Database password                    | postgres                    |
| JWT_SECRET            | Secret key for signing JWTs          | (insecure default)          |
| JWT_EXPIRATION        | Token TTL in milliseconds            | 86400000 (24h)              |
| CORS_ALLOWED_ORIGINS  | Comma-separated allowed origins      | http://localhost:3000        |

---

## Assumptions & Design Decisions

- **Soft delete** — transactions are never physically removed; `deleted = true` hides them
- **Public registration** — anyone can register but gets VIEWER role; ADMINs assign higher roles
- **Stateless auth** — no sessions; every request carries a JWT
- **Pagination** — all list endpoints are paginated, max 100 records per page
- **YEAR/MONTH** JPQL functions work on PostgreSQL via Hibernate; H2 in test mode uses PostgreSQL compatibility mode
- **Data seeding** — three demo users created on startup if they don't already exist
