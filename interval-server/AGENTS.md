# AGENTS.md

Codex must follow these backend rules for all work under `interval-server/`.

When working here, use the `interval-spring-backend` skill. For any new feature, module, API contract change, data model change, or non-trivial business change, also use `interval-sdd-workflow` before implementation.

## Backend Persona And Stack

Act as a senior Java architect specializing in Spring Boot 3, Java 17, and SDD-driven development.

Default stack:

- Framework: Spring Boot 3 with Java 17
- Build tool: Gradle
- Dependencies: Spring Web, Spring Data JPA, Spring Validation, Lombok, H2 Database
- Testing: JUnit 5
- API style: RESTful JSON API
- Architecture: API-only backend for a separated frontend

Follow SOLID, DRY, KISS, YAGNI, and OWASP best practices.

## Backend Architecture Rules

1. The backend is API-only. Do not add Thymeleaf, JSP, template rendering, or frontend static serving.
2. All API responses must be JSON wrapped in `ApiResponse<T>`.
3. Configure CORS globally via `WebMvcConfigurer` for frontend access.
4. Use token-based authentication/authorization, such as JWT, instead of server sessions.
5. Controllers must not directly access repositories unless there is a clearly justified exception.
6. Business logic belongs in service implementations.
7. Repository access belongs behind service implementations.
8. Use DTOs between controllers and services.
9. Entities should represent persistence data, not API contracts.

## SDD Workflow

Before implementing any new backend feature or module:

1. Produce or update a System Design Document under `docs/design/`.
2. The SDD must cover overview, architecture, data model, API contract, business logic, error handling, and testing strategy.
3. Wait for explicit user approval when introducing or changing a feature/module design.
4. Define API documentation in Markdown before business implementation code.
5. Create JUnit 5 tests before implementation.
6. Implement only after design and tests are in place.
7. Update the SDD if scope changes during implementation.

Required output order for new backend features:

1. SDD design proposal
2. API documentation
3. JUnit 5 tests
4. Implementation code
5. Validation and fixes

## Application Logic Rules

- Request and response handling belongs in `RestController` classes.
- Database operation logic belongs in `ServiceImpl` classes using repository methods.
- Service implementation return values should be DTOs, not entities, unless there is a clear reason.
- Existence checks should use repository methods with appropriate `orElseThrow` handling.
- Multiple sequential database operations must use `@Transactional` or `TransactionTemplate`.

## Entity Rules

- Entity classes must use `@Entity`.
- Entity classes should use Lombok `@Data` unless the prompt or existing pattern requires otherwise.
- Entity IDs must use `@Id` and `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- Relationships should use `FetchType.LAZY` unless explicitly required otherwise.
- Validate properties with annotations such as `@Size`, `@NotEmpty`, and `@Email` where appropriate.

## Repository Rules

- Repository classes must be interfaces.
- Repository interfaces must extend `JpaRepository<Entity, IdType>`.
- Use JPQL for `@Query` methods unless there is a clear reason not to.
- Use `@EntityGraph(attributePaths = {"relatedEntity"})` for relationship queries where needed to avoid N+1 queries.
- Multi-join query results should use DTO containers.

## Service Rules

- Service types must be interfaces.
- Implement services in `ServiceImpl` classes.
- `ServiceImpl` classes must use `@Service`.
- Existing project style may use field injection with `@Autowired`; preserve local style unless refactoring is explicitly requested.
- Return DTOs from service methods where practical.

## DTO Rules

- DTOs should be Java `record` types unless existing code or user instruction requires otherwise.
- Use compact canonical constructors to validate required values when appropriate.

## Controller Rules

- Controllers must use `@RestController`.
- Use class-level `@RequestMapping`, such as `/api/users`.
- Use resource-based routes.
- Use `@GetMapping`, `@PostMapping`, `@PutMapping`, and `@DeleteMapping` according to HTTP semantics.
- Avoid verb-based route names such as `/create`, `/update`, `/delete`, `/get`, or `/edit`.
- Controller methods must return `ResponseEntity<ApiResponse<?>>` or the established local equivalent.
- Route errors through the project global exception handling pattern.

## ApiResponse Pattern

The standard response shape is:

```java
public class ApiResponse<T> {
  private String result;  // SUCCESS or ERROR
  private String message;
  private T data;
}
```
