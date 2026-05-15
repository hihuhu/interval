# Interval

A monorepo containing the `Interval` backend and frontend applications.

## Repository Structure

- `interval-server/` — Spring Boot 3 backend using Java 17 and Gradle
- `interval-client/` — Vue 3 frontend using TypeScript
- `docs/design/` — system design and workflow documentation
- `docs/prototypes/` — UI prototypes and reference materials

## Tech Stack

### Backend

- Java 17
- Spring Boot 3
- Gradle

### Frontend

- Vue 3
- TypeScript
- Vite

## Development Workflow

This repository follows an SDD-first workflow:

1. Update or create the relevant design document in `docs/design/`
2. Write tests
3. Implement the change

Do not skip the documentation-first step for features.

## Recommended Branch Naming

Create all work branches from `main`.

Recommended pattern:

`<type>/<scope>-<short-description>`

Examples:

- `feat/client-login-page`
- `feat/server-user-api`
- `docs/monorepo-setup-guide`
- `fix/client-router-guard`
- `chore/monorepo-cleanup`

See `docs/design/monorepo-branching-and-initialization.md` for the full convention.

## Initial Setup

### 1. Clone the Repository

```bash
git clone git@github.com:hihuhu/interval.git
cd interval
```

### 2. Create a Working Branch

```bash
git checkout main
git pull origin main
git checkout -b feat/your-change-name
```

## Backend Setup

### Package Structure

The backend follows a **module + layer** structure:

```
com.interval/
├── auth/           # Authentication module
│   ├── entity/
│   ├── repository/
│   ├── service/
│   ├── controller/
│   ├── dto/
│   ├── config/
│   ├── exception/
│   └── util/
├── category/       # Category module
│   ├── entity/
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── dto/
├── timeslot/       # Time slot module
│   ├── entity/
│   └── repository/
└── common/         # Common utilities
    ├── dto/
    └── exception/
```

### Running Tests

From `interval-server/`:

```bash
cd interval-server
gradle test
```

Or using your IDE:
- **IntelliJ IDEA**: Right-click test class → Run
- **VS Code**: Use Java Test Runner extension

### Starting the Server

```bash
gradle bootRun
```

The server will start at `http://localhost:8080`

## Frontend Initialization

From `interval-client/`:

```bash
cd interval-client
npm install
npm run test
npm run dev
```

## Environment Notes

- Keep local secrets in `.env` files that are not committed
- Configure the frontend API base URL through environment variables
- Do not hardcode backend service URLs in client code

## First Contribution Checklist

Before coding:

- pull the latest `main`
- create a topic branch
- add or update the design doc in `docs/design/`
- add tests before implementation

Before opening a PR:

- ensure the change is focused
- ensure tests pass
- ensure docs are updated together with code

## Git Convention

Use Conventional Commits.

Examples:

- `feat: add booking module skeleton`
- `docs: add monorepo onboarding guide`
- `fix: correct client api base url handling`
- `test: add user service tests`

## Current Status

This repository is initialized and connected to GitHub at:

- `git@github.com:hihuhu/interval.git`
