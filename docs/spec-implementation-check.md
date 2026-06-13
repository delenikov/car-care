# Spec Implementation Check

Date: 2026-06-13

Scope checked:

- `docs/srs.md`
- `docs/mini-spec.md`

Note: no `sds.md` file exists in this repository. This check treats `docs/srs.md` as the intended system specification.

## Summary

FR-1 through FR-42 from `docs/srs.md` are implemented on the backend and frontend and are covered by unit tests, PostgreSQL Testcontainers integration tests, and Playwright e2e tests.

The remaining gaps are outside FR-1 through FR-42 or are non-functional/architecture concerns, including FR-43 and FR-44 loyalty automation, real SMTP transport configuration, binary object storage, `/api/v1` endpoint versioning, UUID identifiers, pagination, and audit-field expansion.

## Implemented Or Mostly Implemented

- User login with JWT access and refresh tokens.
- BCrypt password hashing.
- Refresh token rotation during refresh.
- Secure logout endpoint that revokes the submitted refresh token.
- Authenticated password change that revokes active refresh tokens.
- Basic RBAC for admin endpoints.
- Admin creation of users.
- Customer create/update/list/detail/delete with first-name and last-name search.
- Customer vehicles and customer service-history views.
- Vehicle create/update/list/detail, customer association, and VIN/license-plate/owner search.
- Appointment list/calendar, availability slots, scheduling, conflict prevention, 24-hour cancellation links, confirmation emails, reminder emails, cancellation endpoint, and rescheduling.
- Service record create/list with service type, mileage, replaced parts, parts cost, labor cost, calculated total, customer history, and vehicle history.
- Offer create/list/detail with parts/labor/total breakdown, send email workflow, and PDF export.
- Service document metadata create/list/detail with automatic generation after service-record creation, send email workflow, and PDF export.
- Basic loyalty rule create/list.
- Dashboard summary counts.
- Flyway baseline migrations.
- Macedonian i18n resources exist in the frontend.

## Missing Or Partial

### User Management

- Permission management beyond roles is missing.
- Failed login attempts are counted, but account lockout policy is not implemented.

### Appointment Management

- Public `/schedule` flow from `docs/user-flows.md` is missing.
- Phone verification, attachment upload, holidays, and configurable working hours are missing.

### Quotations And Documents

- Email delivery is configured through Spring Mail and defaults to local Mailpit SMTP on port 1025.
- Generated document files are rendered on demand; binary object storage is not implemented.

### Loyalty And Discount

- Repeat-customer identification is missing.
- Visit frequency tracking is missing.
- Automatic discount calculation is missing.
- Discount application to quotations is missing.

### Non-Functional And Architecture

- `/api/v1/` endpoint versioning is missing.
- UUID primary keys are missing.
- `created_by` and `updated_by` audit fields are missing.
- Soft delete is only partially implemented.
- List endpoints are not paginated.
- Backend modules are not organized into `controller/service/repository/dto/entity/mapper` subfolders.
- Mapper layer is missing.
- `docs/architecture.md` is missing.
- Production config uses `ddl-auto: update` despite Flyway.
- Secrets and database credentials are hardcoded in `application.yaml`.
- Dark mode is missing.
- Table filtering and sorting are missing.
- Backup and restore implementation is missing.
- HTTPS enforcement/deployment configuration is missing.

## Tests Added

- Unit tests for auth logout, password change, refresh-token revocation, and invalid current password.
- Unit tests for admin employee create/update/disable behavior.
- Unit tests for customer create, name parsing, first-name search, last-name search, and soft delete.
- Unit tests for vehicle create, update, customer reassignment, duplicate plate rejection, and VIN/license-plate/owner search routing.
- Unit tests for service-record parts/labor total calculation, backward-compatible total handling, customer/vehicle ownership validation, and vehicle history lookup.
- Unit tests for appointment available slots, conflict prevention, confirmation email, 24-hour cancellation links, cancellation invalidation, and reminders.
- Unit tests for offer parts/labor total calculation, quote email sending, and PDF export delegation.
- Unit tests for generated service-document metadata, service-document email sending, service-document PDF export delegation, and PDF byte rendering.
- PostgreSQL Testcontainers integration tests for:
  - Login, admin user creation/update/delete, RBAC, password change, refresh-token revocation, logout revocation.
  - Implemented operational CRUD path: customer create/search/delete, customer vehicles, customer service history, vehicle create/search/update/detail/history, appointment availability/scheduling/conflict/cancellation/reminders, service record with calculated total and generated document, offer breakdown/send/PDF, document send/PDF, dashboard summary.
- Frontend Playwright e2e tests for:
  - Unauthenticated redirect to login.
  - Login and dashboard summary rendering.
  - Authenticated navigation across implemented module pages.
  - Customer creation request mapping.
  - Customer first-name/last-name search, customer vehicle display, customer service-history display, and customer delete.
  - Vehicle VIN/license-plate/owner search, vehicle create DTO mapping, and vehicle update DTO mapping.
  - Service-record creation with service type, mileage, replaced parts, parts/labor cost breakdown, and calculated total.
  - Appointment available-slot display and scheduling DTO mapping.
  - Offer creation with parts/labor breakdown and quote send action.
  - Quote PDF export action.
  - Generated service-document PDF export and send actions.
  - Admin employee create/update/disable.
  - Password change and logout from the authenticated shell.

The Testcontainers tests are configured with `disabledWithoutDocker = true`, so they run automatically when Docker is available and skip cleanly otherwise.
