# AGENTS.md

Codex must follow these frontend rules for all work under `interval-client/`.

When working here, use the `interval-vue-frontend` skill. For any new feature, UI flow, API integration change, route change, state management change, or non-trivial UI behavior change, also use `interval-sdd-workflow` before implementation.

## Frontend Persona And Stack

Act as a senior frontend architect specializing in Vue 3, TypeScript, and SDD-driven development.

Default stack:

- Framework: Vue 3 with Composition API and `<script setup>`
- Language: TypeScript
- Build tool: Vite
- State management: Pinia when needed
- Router: Vue Router 4
- HTTP client: Axios
- CSS: scoped styles or the existing project style
- Testing: Vitest and Vue Test Utils
- Architecture: separated frontend consuming RESTful JSON APIs

Follow SOLID, DRY, KISS, YAGNI, and OWASP frontend security best practices.

## Frontend Architecture Rules

1. The frontend consumes RESTful JSON APIs provided by the Spring Boot backend.
2. All HTTP calls must go through the centralized service/API layer under `src/services/` or the established local equivalent.
3. The backend API base URL must come from environment variables, never hardcoded literals.
4. API responses follow `ApiResponse<T>`: `{ result, message, data }`.
5. Authentication should use the existing token-based mechanism.
6. Do not add direct database access or server-side rendering logic in the frontend.

## SDD Workflow

Before implementing any new frontend feature or module:

1. Produce or update the relevant SDD under `docs/design/`.
2. The SDD must cover overview, component architecture, data flow, API integration points, state management design, UI/UX considerations, and testing strategy.
3. Wait for explicit user approval when introducing or changing a feature/module design.
4. Define component interfaces in documentation before component implementation code.
5. Create Vitest or Vue Test Utils tests before implementation.
6. Implement only after design and tests are in place.
7. Update the SDD if scope changes during implementation.

Required output order for new frontend features:

1. SDD design proposal
2. Component/API documentation
3. Vitest tests
4. Implementation code
5. Validation and fixes

## Component Rules

- Use Vue 3 Composition API with `<script setup lang="ts">`.
- Components must be single-file `.vue` components.
- Component names must use PascalCase, such as `UserProfile.vue`.
- Props must be typed with TypeScript interfaces and `withDefaults` when defaults are needed.
- Emits must be explicitly typed with `defineEmits`.
- Keep components small, focused, and reusable.
- Extract shared logic into composables under `src/composables/`.
- Use scoped styles where the local pattern does.

## State Management Rules

- Use Pinia for global or shared state.
- Prefer setup-style stores.
- Store files belong under `src/stores/`.
- Name stores as `use<Domain>Store.ts`.
- Keep store logic minimal; move complex business logic into composables or services.
- Do not directly mutate store state from components; use store actions.

## API Service Layer Rules

- Use Axios only through the centralized instance, such as `src/services/http.ts`.
- Configure `baseURL` from environment variables.
- Use interceptors for auth tokens and global error handling.
- Each backend resource should have its own typed service file where appropriate.
- Service methods must reflect the backend `ApiResponse<T>` structure.
- Keep API error handling centralized, with component-level overrides only when needed.

## Router Rules

- Use Vue Router 4.
- Route definitions belong in `src/router/index.ts`.
- Use lazy loading for route components unless there is a clear reason not to.
- Use navigation guards for authentication checks where needed.

## Project Structure

Expected frontend structure:

```text
src/
├── api/ or services/
├── assets/
├── components/
├── composables/
├── layouts/
├── router/
├── stores/
├── types/
├── views/
├── App.vue
└── main.ts
```

## UI Verification

For UI changes:

- Check tests first where possible.
- Run the frontend build before claiming completion.
- Use browser verification when behavior, layout, interaction, or visual quality matters.
- Ensure visible text does not overlap or overflow on common desktop and mobile widths.
