---
name: interval-sdd-workflow
description: Use when working in the Interval repo on any new feature, module, non-trivial behavior change, API contract change, UI flow change, or implementation request that may require project design approval.
---

# Interval SDD Workflow

This project uses System Design Document driven development. The design and approval gate is part of the task, not optional overhead.

## When To Use

Use this skill before implementation when the request involves:

- A new backend or frontend feature.
- A new API, changed API contract, or changed data model.
- A non-trivial business logic change.
- A non-trivial UI flow, component, state management, routing, or API integration change.
- Any change where the existing design docs may become inaccurate.

For tiny mechanical edits, typo fixes, dependency-free formatting, or direct user instructions to skip design, keep the change narrow and still update `PROJECT_LOG.md`.

## Required Context

Before proposing or changing anything:

1. Read `PROJECT_LOG.md`.
2. Read `QUICK_START.md`.
3. Read the relevant existing SDD in `docs/design/`.
4. If no relevant SDD exists, create one under `docs/design/sdd-<feature-name>.md`.

## Required Flow

For new features or non-trivial changes:

1. Present the SDD/design proposal.
2. Ask for explicit user approval.
3. Do not write implementation code until approval is given.
4. Define API or component documentation before implementation.
5. Write tests before implementation.
6. Implement the smallest approved change.
7. Run focused validation first, then broader validation appropriate to the affected area.
8. Update the SDD if scope changed during implementation.
9. Update `PROJECT_LOG.md` before finishing.

## Backend SDD Minimum Sections

Backend SDDs must include:

- Overview
- Architecture
- Data model
- API contract: method, path, request/response body, status codes, validation rules
- Business logic
- Error handling
- Testing strategy

After approval, backend output order is:

1. API documentation if not already covered by the SDD
2. JUnit 5 tests
3. Implementation
4. Validation and fixes

## Frontend SDD Minimum Sections

Frontend SDDs must include:

- Overview
- Component architecture
- Data flow
- API integration points
- State management design
- UI/UX considerations
- Testing strategy

After approval, frontend output order is:

1. Component/API documentation
2. Vitest and Vue Test Utils tests
3. Implementation
4. Validation and fixes

## Red Lines

- Do not implement a new feature first and document it afterward.
- Do not skip the approval gate for new feature/module design unless the user explicitly says to skip it.
- Do not use tests passing as a substitute for design approval.
- If an existing implementation conflicts with this workflow, explain the conflict before proceeding.

