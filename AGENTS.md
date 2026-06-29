# Automotive Service Management System

## Read First

Before writing code always read:

- srs_mk.md
- mini_specification_mk.md
- business-rules.md
- spec-implementation-check.md
---

## Tech Stack

Frontend:
- React 19
- TypeScript
- Vite
- Material UI
- React Router
- TanStack Query
- Axios

Backend:
- Java 25
- Spring Boot 4.1.0
- Spring Security 7.1.0
- Spring Data JPA 4.1.0
- Hibernate 7.4.1.Final

Database:
- PostgreSQL 18

Authentication:
- JWT
- BCrypt password hashing

Email:
- Spring Mail
- SMTP

Testing:
- JUnit 5
- Mockito
- React Testing Library

Deployment:
- Docker
- Kubernetes

---

## Architecture

Follow Clean Architecture.

Layers:

Controller
↓
Service + (Mappers/Convertors)
↓
Repository
↓
Database

Never place business logic in Controllers.

---

## Backend Structure

Keep the backend as a feature-oriented modular monolith.

Shared infrastructure belongs in dedicated top-level packages:

```text
backend/src/main/java/com/delenicode/carcare
|-- common/
|-- config/
|-- security/
|-- notification/
|-- audit/
`-- <feature>/
```

Feature packages include `auth`, `user`, `customer`, `vehicle`, `appointment`,
`servicerecord`, `offer`, `document`, `dashboard`, and `loyalty`.

Within each feature package, use consistent subpackages:

```text
<feature>/
|-- controller/
|-- service/
|-- repository/
|-- model/
|-- dto/
|   |-- request/
|   `-- response/
|-- mapper/
|-- event/
|-- exception/
`-- scheduler/ (only if needed)
```

Use only the subpackages needed by the feature. Keep JPA entities and domain
value objects in `model/`. Keep feature-specific exceptions in the feature's
`exception/` package. Do not duplicate resource exceptions across features; use
one canonical exception per resource, or the shared `ResourceNotFoundException`
when a feature-specific type is not needed.
---

## Frontend Structure

frontend/src

├── app
├── pages
├── features
├── components
├── services
├── hooks
├── layouts
├── routes
├── utils
├── types

Feature-based organization is required.

Example:

features/
├── customers/
├── vehicles/
├── appointments/
├── services/
├── quotes/

---

## Coding Rules

- Use DTOs.
- Never expose entities directly.
- Use constructor injection only.
- Use pagination for all list endpoints.
- Use UUID identifiers.
- Use soft delete where applicable.

---

## ## Clean Code, Spring Boot, and SOLID Rules

Follow Clean Code principles, SOLID principles, and standard Java/Spring Boot best practices.

### Clean Code Rules

* Write small, focused classes.
* Write small methods that do one thing.
* Use meaningful names for classes, methods, variables, DTOs, and packages.
* Avoid unclear abbreviations.
* Avoid duplicated code.
* Avoid large service classes.
* Avoid long methods.
* Avoid deep nesting.
* Prefer early returns over nested conditionals.
* Keep business rules explicit and easy to read.
* Do not hide important business logic in private utility methods with unclear names.
* Use comments only to explain why something exists, not what obvious code does.
* Remove dead code and unused imports.
* Keep formatting consistent.
* Do not mix unrelated responsibilities in one class.
* Prefer immutable DTOs where possible, especially Java records.
* Validate input at application boundaries.
* Fail fast with clear errors.

### SOLID Principles

* **Single Responsibility Principle:** each class must have one clear reason to change.
* **Open/Closed Principle:** extend behavior through new classes or strategies instead of modifying large existing classes.
* **Liskov Substitution Principle:** subclasses must be usable wherever their parent type is expected.
* **Interface Segregation Principle:** prefer small, focused interfaces over large general-purpose interfaces.
* **Dependency Inversion Principle:** depend on abstractions for external concerns such as email, PDF generation, storage, payments, and notifications.

### Spring Boot Rules

* Keep controllers thin.
* Controllers may only handle HTTP concerns: request mapping, authentication context, validation trigger, and response status.
* Put business logic in services/domain services.
* Use constructor injection only.
* Do not use field injection.
* Use `@Transactional` only at service layer.
* Keep transactions short.
* Do not call external systems inside database transactions.
* Use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` for actions that must happen after commit, such as email delivery.
* Use repositories only for persistence operations.
* Do not return entities from controllers.
* Do not accept entities as request bodies.
* Use DTOs for all API input and output.
* Use mappers to convert between DTOs and entities.
* Use Bean Validation annotations for request validation.
* Use `@Valid` in controller methods.
* Use global exception handling with `@RestControllerAdvice`.
* Group exceptions by category instead of creating one handler method per exception.
* Prefer domain-specific exceptions over generic `IllegalArgumentException`.
* Use pagination for all collection endpoints.
* Use sorting and filtering through explicit request parameters.
* Avoid N+1 queries.
* Use `@EntityGraph`, fetch joins, or projection queries where appropriate.
* Avoid eager fetching by default.
* Use lazy relationships unless there is a clear reason not to.
* Do not expose stack traces or internal exception details in API responses.
* Log exceptions server-side with useful context.
* Never log passwords, JWT tokens, or sensitive customer data.

### Java Rules

* Use `BigDecimal` for money.
* Never use `double` or `float` for monetary values.
* Normalize money values to scale 2 where applicable.
* Use `UUID` for identifiers.
* Use `Optional` only as a return type, not as a field or DTO property.
* Prefer `List.of()` and immutable collections where possible.
* Prefer Java records for simple request/response DTOs.
* Use enums for fixed states such as appointment status, quote status, payment status, and user role.
* Avoid magic strings and magic numbers.
* Extract constants when values are reused or have business meaning.
* Keep package visibility where possible.
* Avoid static utility classes unless the logic is truly stateless and generic.
* Prefer clear object-oriented/domain methods over procedural helper methods.

### Exception Handling Rules

* Do not throw generic exceptions for business cases.
* Use a small exception hierarchy, for example:

  * `ResourceNotFoundException`
  * `ValidationException`
  * `BusinessRuleException`
  * `ConflictException`
  * `UnauthorizedActionException`
* Specific exceptions may extend these categories:

  * `CustomerNotFoundException extends ResourceNotFoundException`
  * `VehicleNotFoundException extends ResourceNotFoundException`
  * `QuoteAlreadySentException extends BusinessRuleException`
* Keep feature-specific exceptions in the owning feature's `exception/` package.
* Do not duplicate resource exceptions across features; use the canonical resource exception from the owning feature, or use `ResourceNotFoundException` when a dedicated type is not needed.
* Global exception handler should handle categories, not every individual exception.
* API errors must return consistent JSON.

Example error response:

```json
{
  "timestamp": "2026-01-01T12:00:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Customer not found",
  "path": "/api/customers/..."
}
```

### Service Design Rules

* Services should orchestrate use cases.
* Keep pricing, loyalty, notification, PDF generation, and email rendering in separate services.
* Do not build large HTML strings inside business services.
* Use template engines such as Thymeleaf for emails and documents.
* Prefer event-based workflows for side effects.
* Email sending, PDF generation, notifications, and audit events should not block the main database transaction unless explicitly required.
* Make side-effect operations idempotent where possible.
* Prevent duplicate sends for quotes, invoices, and notifications.
* Use explicit statuses such as:

  * `DRAFT`
  * `PENDING_DELIVERY`
  * `SENT`
  * `DELIVERY_FAILED`
  * `CANCELLED`

### Testing Rules

* Unit test business logic.
* Unit test mappers where mapping is non-trivial.
* Unit test validators.
* Unit test pricing and loyalty calculations.
* Integration test repositories and complex queries.
* Integration test transactional event behavior.
* Test success, validation failure, authorization failure, and not-found cases.
* Do not rely only on happy-path tests.
* Mock external systems such as SMTP, PDF generation, and payment providers.
* Tests must be readable and follow Arrange-Act-Assert.
* Test names should describe expected behavior.

### Code Review Checklist

Before considering code complete, verify:

* No business logic in controllers.
* No entities exposed through API.
* DTOs and validation are present.
* Transactions are short.
* No external calls inside transactions.
* Exceptions are domain-specific.
* Global exception response is consistent.
* List endpoints are paginated.
* N+1 queries are checked.
* Security-sensitive data is not logged.
* Tests cover normal and failure paths.
* Code follows SOLID and Clean Code principles.


## Database Rules

Primary keys:
- UUID

Audit fields:

- created_at
- updated_at
- created_by
- updated_by

Use Flyway migrations.

---

## UI Requirements

- Responsive desktop-first design.
- Macedonian language support.
- Dark mode support.
- Calendar view for appointments.
- Table filtering and sorting.

---

## Definition of Done

Every feature must include:

- Backend implementation
- Database migration
- REST endpoint
- Frontend page
- Validation
- Tests
