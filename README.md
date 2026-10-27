# Secure Task Management System

A production-grade, secure, multi-tenant Task Management System engineered with a **clean architecture**, fine-grained **Role-Based Access Control (RBAC)**, object-level ownership checks preventing **Insecure Direct Object Reference (IDOR)** attacks, immutable audit logging, and automated testing across backend and frontend.

---

## 1. Architecture & Technology Stack

### Backend
- **Language**: Java 21 / 23 (Bytecode target: Java 21 LTS)
- **Framework**: Spring Boot 3.4.1
- **Security**: Spring Security with stateless JWT filter chain, BCrypt password hashing (strength 12), and custom `@taskSecurity` method evaluators.
- **Data & ORM**: Spring Data JPA & Hibernate 6.6
- **Database Migrations**: Flyway Core & Flyway PostgreSQL
- **Database**: PostgreSQL 17
- **API Documentation**: SpringDoc OpenAPI 2.7.0 (Swagger UI at `/swagger-ui.html`)
- **Validation**: Jakarta Bean Validation (`@Valid`, custom regex for password security)
- **Rate Limiting**: Sliding token-bucket rate limiter defending authentication endpoints

### Frontend
- **Framework**: React 18
- **Language**: TypeScript 5.6
- **Build Tool**: Vite 6.0
- **Styling**: Tailwind CSS 3.4
- **Routing**: React Router 6.28
- **HTTP Client**: Axios with Bearer request interceptors and 401 session-expiry handling
- **Icons**: Lucide React

### Testing
- **Backend**: JUnit 5, Mockito, Spring Boot Test, Spring Security Test, H2 In-Memory DB (PostgreSQL compatibility mode)
- **Frontend**: Vitest, React Testing Library, JSDOM

---

## 2. Role-Based Access Control & Permission Matrix

Permissions are enforced strictly at both the API controller layer (`@PreAuthorize`) and service layer (object-level security checks). Hiding frontend controls is considered convenience only; the server always validates identity and permissions.

| Resource / Action | Endpoint | HTTP Method | ADMIN | MANAGER | USER | Scoping / Ownership Rule |
| :--- | :--- | :--- | :---: | :---: | :---: | :--- |
| **Register Account** | `/api/v1/auth/register` | `POST` | ✔ | ✔ | ✔ | Public (rate limited; assigns `ROLE_USER`) |
| **Authenticate / Login** | `/api/v1/auth/login` | `POST` | ✔ | ✔ | ✔ | Public (rate limited; issues JWT & HttpOnly cookie) |
| **Current User Info** | `/api/v1/auth/me` | `GET` | ✔ | ✔ | ✔ | Authenticated user profile |
| **Refresh Token** | `/api/v1/auth/refresh` | `POST` | ✔ | ✔ | ✔ | Rotates refresh token in database |
| **Sign Out** | `/api/v1/auth/logout` | `POST` | ✔ | ✔ | ✔ | Revokes active refresh token & clears cookie |
| **List Users** | `/api/v1/users` | `GET` | ✔ | ✖ | ✖ | Admin only |
| **Inspect User** | `/api/v1/users/{id}` | `GET` | ✔ | ✖ | Self | Admin or matching user ID |
| **Toggle User Status** | `/api/v1/users/{id}/status` | `PATCH` | ✔ | ✖ | ✖ | Admin only (Activate/Deactivate account) |
| **Assign Roles** | `/api/v1/users/{id}/roles` | `PUT` | ✔ | ✖ | ✖ | Admin only |
| **List Assignable Users**| `/api/v1/users/team-eligible` | `GET` | ✔ | ✔ | ✖ | Admin gets all; Manager gets team members |
| **Create Team** | `/api/v1/teams` | `POST` | ✔ | ✖ | ✖ | Admin only |
| **List Teams** | `/api/v1/teams` | `GET` | All | Managed | Member | Scoped to membership or management |
| **Add Team Member** | `/api/v1/teams/{id}/members` | `POST` | ✔ | Managed | ✖ | Admin or designated team manager |
| **Remove Team Member** | `/api/v1/teams/{id}/members/{uId}` | `DELETE` | ✔ | Managed | ✖ | Admin or designated team manager |
| **Create Task** | `/api/v1/tasks` | `POST` | ✔ | Managed | ✖ | Admin (any team); Manager (own team members) |
| **List Tasks** | `/api/v1/tasks` | `GET` | All | Team | Assigned | Scoped dynamically based on authenticated role |
| **View Task Details** | `/api/v1/tasks/{id}` | `GET` | Any | Team | Assigned | IDOR protection: returns 403 if unauthorized |
| **Edit Task Details** | `/api/v1/tasks/{id}` | `PUT` | Any | Team | ✖ | Admin or manager of the task's team |
| **Update Task Status** | `/api/v1/tasks/{id}/status` | `PATCH` | Any | Team | Assigned | Assigned user can advance status of own task |
| **Delete Task** | `/api/v1/tasks/{id}` | `DELETE` | Any | Team | ✖ | Admin or manager of the task's team |
| **Add Comment** | `/api/v1/tasks/{id}/comments` | `POST` | ✔ | Team | Assigned | Must have view access on the task |
| **List Comments** | `/api/v1/tasks/{id}/comments` | `GET` | ✔ | Team | Assigned | Must have view access on the task |
| **Task Activity History**| `/api/v1/tasks/{id}/history` | `GET` | ✔ | Team | Assigned | Must have view access on the task |
| **Dashboard Metrics** | `/api/v1/dashboard/stats` | `GET` | Global | Team | Personal | Scoped server-side; non-admin never gets global stats |
| **System Audit Trail** | `/api/v1/audit-logs` | `GET` | ✔ | ✖ | ✖ | Admin only |

---

## 3. Security Design Decisions

1. **IDOR & Object-Level Access Protection**:
   - Knowing a Task ID `999` does not grant access. Before returning or mutating a task, the `TaskSecurityEvaluator` verifies whether the authenticated principal is an administrator, the team's manager, or the specific assigned user.
   - Unauthorized attempts immediately return `HTTP 403 Forbidden`.
2. **Mass Assignment Prevention**:
   - Separate, dedicated Request DTOs (`CreateTaskRequest`, `UpdateTaskRequest`, `UpdateTaskStatusRequest`, `RegisterRequest`, `UpdateUserRolesRequest`) prevent malicious clients from binding protected fields like `id`, `roles`, or `is_active`.
3. **Dual JWT Strategy**:
   - Standard `Authorization: Bearer <token>` header support for API consumers and native clients.
   - Secure HttpOnly `SameSite=Lax` cookie delivery for enhanced browser protection against Cross-Site Scripting (XSS) token theft.
4. **Credential Hashing**:
   - `BCryptPasswordEncoder` configured with an adaptive work factor of 12 rounds.
5. **Brute-Force Rate Limiting**:
   - Authentication endpoints (`/api/v1/auth/login`, `/api/v1/auth/register`) are guarded by an in-memory token bucket rate limiter returning `HTTP 429 Too Many Requests` when thresholds are breached.
6. **Centralized Error Handling**:
   - `GlobalExceptionHandler` ensures stack traces, database schema details, and server internals are never exposed to clients, returning standardized `ApiErrorResponse` payloads.

---

## 4. Default Test Credentials

The database is initialized on startup with default demo accounts:

| Role | Email | Password | Permissions Summary |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin@tasksystem.com` | `Admin@123456` | Full system governance, user activation, role assignment, audit logs |
| **MANAGER** | `manager@tasksystem.com` | `Manager@123456` | Team management, task allocation, team-scoped dashboard metrics |
| **USER** | `user@tasksystem.com` | `User@123456` | Individual task execution, assigned task status updates, comments |

*The login page also provides one-click instant credential buttons for quick testing.*

---

## 5. Prerequisites & Local Setup

### Prerequisites
- Java 21 or later
- Node.js 18+ and npm
- PostgreSQL 17 (or Docker)

### Option A: Local Development

1. **Database Setup**:
   Ensure PostgreSQL is running locally on port 5432 and create the database:
   ```sql
   CREATE DATABASE task_management;
   ```

2. **Backend**:
   ```bash
   cd backend
   # Build and run tests
   ./mvnw clean test

   # Run backend application
   ./mvnw spring-boot:run
   ```
   *The backend will start at `http://localhost:8080`. Flyway will run initial schema migrations and seed default roles and accounts automatically.*

3. **Frontend**:
   ```bash
   cd frontend
   npm install
   npm run build
   npm test
   npm run dev
   ```
   *The frontend development server will start at `http://localhost:5173`.*

---

### Option B: Docker Compose

To start the entire full-stack application (Postgres + Backend + Frontend):

```bash
docker compose up --build -d
```

- **Frontend**: `http://localhost` (or `http://localhost:5173`)
- **Backend API**: `http://localhost:8080`
- **Swagger Documentation**: `http://localhost:8080/swagger-ui.html`

---

## 6. Running Automated Tests

### Backend Tests
```bash
cd backend
./mvnw test
```
**Test Coverage Includes**:
- `JwtTokenProviderTest`: Token signing, validation, claims parsing, and expiration handling.
- `AuthServiceTest`: Account registration, BCrypt password hashing, duplicate email handling, deactivated account rejection.
- `TaskSecurityEvaluatorTest`: Role verification, ownership boundaries, and IDOR negative tests.
- `TaskServiceTest`: Task creation scoping, team assignment constraints, and status completion timestamps.
- `TaskControllerSecurityIntegrationTest`: Spring Security endpoint tests verifying `401 Unauthorized`, `403 Forbidden` on role mismatch, and `403 Forbidden` on IDOR attempts.

### Frontend Tests
```bash
cd frontend
npm test
```
**Test Coverage Includes**:
- `Badge.test.tsx`: Visual status indicators, priority badges, and overdue alerts.
- `ProtectedRoute.test.tsx`: Route authorization guards, loading state, and role restriction enforcement.

---

## 7. OpenAPI / Swagger Documentation

When the backend is running, the interactive OpenAPI specification and Swagger UI can be accessed at:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

To test authenticated endpoints in Swagger UI:
1. Call `POST /api/v1/auth/login` with your credentials.
2. Copy the `accessToken` from the response.
3. Click the **Authorize** button at the top of Swagger UI and enter your token.
