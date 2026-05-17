# Interval

Interval 是一个前后端分离的时间块记录工具，基于“柳比歇夫时间记录法”：每天固定拆成 96 个 15 分钟格子，用户可以按分类记录、回顾自己的时间使用。

## Repository Structure

- `interval-server/` — Spring Boot 3 backend using Java 17 and Gradle
- `interval-client/` — Vue 3 frontend using TypeScript, Vite, Pinia, Vue Router and Axios
- `docs/design/` — SDD/system design documents
- `docs/prototypes/` — UI prototypes and reference materials

## Current Features

### Backend

- User registration and login
- BCrypt password hashing
- JWT authentication and user context
- Category CRUD with smart delete behavior
- TimeSlot daily query, single-slot upsert and delete
- User data isolation by authenticated JWT user
- Standard JSON response wrapper: `ApiResponse<T>`
- Global CORS for local Vue development

### Frontend

- Login and registration pages
- JWT persistence and Axios Bearer token interceptor
- Protected routes with Vue Router guards
- Time Grid page with 96 slots
- Category query, quick create, edit and smart delete result display
- Single-slot create/edit/delete
- Shift-click continuous range fill using existing TimeSlot upsert API
- Vitest + Vue Test Utils coverage for services, stores, router and components

## Tech Stack

### Backend

- Java 17
- Spring Boot 3
- Spring Web, Spring Data JPA, Spring Security, Spring Validation
- H2 Database for local development and tests
- JWT (`jjwt`)
- Lombok
- Gradle
- JUnit 5

### Frontend

- Vue 3 Composition API
- TypeScript strict mode
- Vite
- Pinia
- Vue Router 4
- Axios
- Vitest + Vue Test Utils + jsdom

## Development Workflow

This repository follows an SDD-first workflow:

1. Update or create the relevant design document in `docs/design/`
2. Write tests
3. Implement the change
4. Run validation
5. Update `PROJECT_LOG.md`

Do not skip the documentation-first step for new features or non-trivial changes.

## Backend Setup

From `interval-server/`:

```bash
gradle test
gradle bootRun
```

The backend starts at:

```text
http://localhost:8088
```

H2 console:

```text
http://localhost:8088/h2-console
```

H2 JDBC URL:

```text
jdbc:h2:mem:testdb
```

If the Gradle wrapper cannot download its distribution on Windows, use an installed system Gradle as a fallback:

```bash
gradle test
gradle bootRun
```

## Frontend Setup

From `interval-client/`:

```bash
npm install
npm run test
npm run build
npm run dev
```

The local frontend dev server starts at:

```text
http://localhost:5173
```

Configure the backend URL via `.env`:

```bash
VITE_API_BASE_URL=http://localhost:8088
```

Use `interval-client/.env.example` as the template.

## Useful Validation Commands

Backend:

```bash
cd interval-server
gradle test
```

Frontend:

```bash
cd interval-client
npm run test
npm run build
```

## Local End-to-End Flow

1. Start backend: `gradle bootRun` from `interval-server/`
2. Start frontend: `npm run dev` from `interval-client/`
3. Open `http://localhost:5173`
4. Register a new user
5. Login
6. Create or edit categories
7. Click a single time slot to create/edit a record
8. Shift-click a second slot to fill a continuous range
9. Delete a time slot
10. Delete a category and observe `DELETED` or `ARCHIVED` smart delete result

## Git Convention

Use Conventional Commits.

Examples:

- `feat: add time grid range fill`
- `fix: correct category smart delete display`
- `test: add category store tests`
- `docs: update quick start status`
