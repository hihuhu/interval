---
name: interval-spring-backend
description: Use when working in interval-server on Spring Boot 3 Java code, backend APIs, controllers, services, repositories, DTOs, entities, exception handling, ApiResponse, Gradle, H2, JWT, CORS, or backend tests.
---

# Interval Spring Backend

Use these conventions for backend work under `interval-server/`.

## Required Companion Skill

For any new feature or non-trivial backend change, use `interval-sdd-workflow` before implementation.

## Stack

- Spring Boot 3 with Java 17
- Gradle
- Spring Web, Spring Data JPA, Spring Validation, Lombok
- H2 for local development and tests unless instructed otherwise
- JUnit 5
- RESTful JSON API only

## Architecture Rules

- Keep the backend API-only. Do not add server-side templates or serve frontend static resources.
- All API responses must use the standard `ApiResponse<T>` JSON wrapper.
- Configure CORS globally with `WebMvcConfigurer` unless a specific exception is justified.
- Use token-based auth such as JWT, not server sessions.
- Controllers handle request/response mapping.
- Services contain business logic.
- Repositories encapsulate persistence access.
- Use DTOs between controllers and services.
- Entities are persistence models, not API contracts.

## ApiResponse

`ApiResponse<T>` must be generic and include:

- `result`: `SUCCESS` or `ERROR`
- `message`: success or error message
- `data`: nullable payload

Use Lombok `@Data`, `@NoArgsConstructor`, and `@AllArgsConstructor` if that matches the existing class. Prefer static factories such as `success(...)` and `error(...)` when extending the pattern.

## Global Exception Handling

`GlobalExceptionHandler` must:

- Use `@RestControllerAdvice`.
- Return `ResponseEntity<ApiResponse<?>>`.
- Provide a helper for structured error responses.
- Handle `IllegalArgumentException` as HTTP 400.
- Add common handlers when needed, such as validation failures or not-found cases.

## Controller Rules

- Use `@RestController`.
- Use class-level `@RequestMapping`, for example `/api/users`.
- Use resource-based paths and HTTP method annotations.
- Avoid verb paths such as `/create`, `/update`, `/delete`, `/get`, or `/edit`.
- Return `ResponseEntity<ApiResponse<?>>` or the established local equivalent.
- Route errors through the global exception handling pattern.

## Service Rules

- Services are interfaces.
- Implement services in `ServiceImpl` classes.
- Annotate implementations with `@Service`.
- Preserve existing dependency injection style unless refactoring is requested.
- Return DTOs where practical.
- Use repository methods for existence checks with `orElseThrow` where appropriate.
- Use `@Transactional` or `TransactionTemplate` for multiple sequential database operations.

## Repository Rules

- Repositories are interfaces extending `JpaRepository<Entity, IdType>`.
- Use JPQL for `@Query` methods unless there is a clear reason not to.
- Use `@EntityGraph` for relationship queries where needed to avoid N+1 problems.
- Use DTO containers for multi-join query results.

## Entity And DTO Rules

- Entities use `@Entity`.
- Entity IDs use `@Id` and `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- Relationships should be `FetchType.LAZY` unless explicitly required otherwise.
- Validate fields with annotations such as `@Size`, `@NotEmpty`, and `@Email`.
- DTOs should be Java `record` types unless the existing code or user instruction requires otherwise.
- Use compact canonical constructors for DTO input validation when appropriate.

## Testing And Validation

- Write JUnit 5 tests before backend implementation for new features.
- Cover happy path, validation failure, and key exception scenarios unless the user narrows scope.
- Use Gradle for dependency and test commands.
- Run focused tests first, then broader backend validation as appropriate.

