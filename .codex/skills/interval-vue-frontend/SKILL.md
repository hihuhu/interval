---
name: interval-vue-frontend
description: Use when working in interval-client on Vue 3, TypeScript, Vite, components, views, Pinia stores, router, Axios services, API integration, frontend tests, UI behavior, or frontend project configuration.
---

# Interval Vue Frontend

Use these conventions for frontend work under `interval-client/`.

## Required Companion Skill

For any new feature, non-trivial UI change, API integration change, route change, or state management change, use `interval-sdd-workflow` before implementation.

## Stack

- Vue 3 with Composition API and `<script setup>`
- TypeScript strict mode
- Vite
- Pinia when global/shared state is needed
- Vue Router 4
- Axios through the service layer
- Vitest and Vue Test Utils
- API-only frontend consumer, no SSR or backend logic

## API Service Rules

- Never call Axios directly from components.
- Use the centralized Axios instance, such as `src/services/http.ts` or the established local equivalent.
- Read `baseURL` from `import.meta.env.VITE_API_BASE_URL`; do not hardcode backend URLs.
- Attach auth tokens through request interceptors when authentication is enabled.
- Handle global errors through response interceptors, including 401 login handling where appropriate.
- Keep backend resource calls in resource-specific service files.
- Type service methods and backend responses.
- Backend response shape is:

```ts
interface ApiResponse<T> {
  result: 'SUCCESS' | 'ERROR'
  message: string
  data: T | null
}
```

Service methods should return unwrapped `data` on success or throw/handle errors consistently with the existing project pattern.

## Component Rules

- Components are single-file `.vue` components.
- Use `<script setup lang="ts">`.
- File names use PascalCase.
- Props use `defineProps<T>()` with TypeScript interfaces.
- Use `withDefaults` when default prop values are needed.
- Emits use typed `defineEmits`.
- Use `<style scoped>` when following local component style.
- Keep templates simple; move complex logic into computed values or functions.
- Extract shared logic into `src/composables/`.
- Avoid `v-html`; if unavoidable, sanitize content and document the reason.
- Use `v-for` with stable unique keys, not indexes unless the list is static.

## State Management Rules

- Use Pinia for shared/global state.
- Use setup-style stores with `defineStore`.
- Store files belong under `src/stores/`.
- Name stores as `use<Domain>Store.ts`.
- Store IDs must be unique and descriptive.
- Keep stores small; move complex logic to composables or services.
- Components should use actions or `$patch`, not direct state mutation.
- Async store work must use the service layer, not direct Axios calls.
- Type store state, getters, action parameters, and returns.

## Router Rules

- Use Vue Router 4 with `createRouter` and `createWebHistory`.
- Routes belong in `src/router/index.ts`.
- Use lazy-loaded route components where practical.
- Route names are unique camelCase strings.
- Paths are kebab-case and resource-oriented.
- Use `meta: { requiresAuth: true }` for auth requirements.
- Use navigation guards for auth checks.
- Put catch-all 404 routes last.

## Project Configuration Rules

- Keep TypeScript strict mode enabled.
- Keep path aliases, such as `@ -> src/`, aligned between Vite and TypeScript config.
- Use Vite env variables for backend configuration.
- Vite proxy may be used for local CORS convenience, but production CORS is a backend concern.

## Testing And UI Verification

- Write Vitest and Vue Test Utils tests before implementation for new frontend features.
- Cover rendering, user interaction, prop variations, and API integration behavior where relevant.
- Run focused tests first, then `npm run test` and `npm run build` when appropriate.
- For visual or interaction changes, verify in a browser when feasible.
- Ensure visible text and controls do not overlap or overflow on common desktop and mobile widths.

