# AGENTS.md

Codex must follow these repository rules for all work in this project.

## Mandatory Startup Context

Before planning, running, editing, testing, or debugging anything in this repository:

1. Read `PROJECT_LOG.md` first to understand recent history, decisions, known issues, and pending work.
2. Read `QUICK_START.md` next to understand current run commands, ports, project status, and required files.
3. For feature or behavior changes, read the relevant document under `docs/design/` before proposing or editing code.
4. If the requested work touches `interval-server/`, also follow `interval-server/AGENTS.md`.
5. If the requested work touches `interval-client/`, also follow `interval-client/AGENTS.md`.
6. Use the project-specific Codex skills when their trigger applies:
   - `interval-sdd-workflow` for new features, modules, non-trivial behavior changes, API contracts, or UI flows.
   - `interval-spring-backend` for backend work under `interval-server/`.
   - `interval-vue-frontend` for frontend work under `interval-client/`.

Do not claim the project has been run, tested, fixed, or verified unless the corresponding command was actually executed and its result was checked.

## Project Overview

This is a monorepo:

- `interval-server/`: Spring Boot 3 backend on Java 17.
- `interval-client/`: Vue 3 frontend with TypeScript.
- `docs/design/`: system design documents.
- `docs/prototypes/`: UI prototypes.

The project uses front-end and back-end separation. The backend is an API-only service. The frontend consumes RESTful JSON APIs.

## Non-Negotiable Development Flow

For any new feature, non-trivial behavior change, or architectural change:

1. Update or create the relevant SDD under `docs/design/`.
2. Wait for explicit user approval when the change introduces a new feature/module or changes the agreed design.
3. Write or update tests before implementation.
4. Implement the smallest scoped change that satisfies the approved design.
5. Run focused tests first, then broader tests/builds appropriate to the affected area.
6. Update `PROJECT_LOG.md` before finishing the session.

Do not skip design or testing steps unless the user explicitly instructs otherwise.

## Running The Project

When asked to run, start, restart, or verify the project locally:

1. Re-read `QUICK_START.md`.
2. Start the backend from `interval-server/`.
3. Start the frontend from `interval-client/`.
4. Report the actual local URLs and ports used.
5. If a service fails to start, inspect the relevant terminal/log output before proposing a fix.

Default ports:

- Backend: `http://localhost:8088`
- Frontend: `http://localhost:5173`
- H2 Console: `http://localhost:8088/h2-console`

Backend commands:

```powershell
cd interval-server
gradle test
gradle bootRun
```

Frontend commands:

```powershell
cd interval-client
npm install
npm run test
npm run build
npm run dev
```

If Gradle Wrapper download is unavailable, the machine may have this Gradle installed:

```text
F:\app\gradle-9.5.1\bin\gradle.bat
```

## Project Log Rules

Every session must end by updating `PROJECT_LOG.md` when repository work was performed.

Log requirements:

- Add the newest entry at the top, below the file title/introduction.
- Use reverse chronological order.
- Include: goal, completed operations, technical decisions, problems, validation results, pending items, and affected files.
- Technical decisions must explain reason and impact.
- Pending items must use Markdown task list syntax: `- [ ]`.

## General Engineering Rules

- Keep changes narrowly scoped to the request.
- Preserve existing architecture and local patterns.
- Do not revert unrelated user changes.
- Do not introduce server-side rendered frontend resources in the backend.
- Do not hardcode frontend API URLs; use environment configuration.
- All backend API responses use `ApiResponse<T>`.
- Frontend API access must go through `src/services/` or the established centralized API layer.
- For UI changes, verify the actual browser-visible result when feasible.

## Cursor Rule Migration Notes

The old Cursor rule files remain for Cursor compatibility:

- `.cursorrules`
- `interval-server/.cursorrules`
- `interval-client/.cursorrules`
- `interval-server/.cursor/rules/*.mdc`
- `interval-client/.cursor/rules/*.mdc`

Codex should use `AGENTS.md`, subdirectory `AGENTS.md`, and the project-specific skills above instead of reading Cursor rules as the primary instruction source.
