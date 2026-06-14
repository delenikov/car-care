# Automotive Service Management System

## Read First

Before writing code always read:

 

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

backend/src/main/java/com/company/autoservice

├── config
├── auth
├── user
├── customer
├── vehicle
├── service
├── appointment
├── quote
├── document
├── loyalty
├── notification
├── common

Each module must contain:

controller/
service/
repository/
dto/
entity/
mapper/

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
- All API endpoints must be versioned:
  /api/v1/

---

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