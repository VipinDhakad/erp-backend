# ERP Backend

Spring Boot backend for a single-school ERP: students, teachers, academics
(classes/sections/subjects/years), attendance, marks/exams, report cards,
and analytics. JWT-based auth with two roles, `ADMIN` and `TEACHER`.

- Java 21, Spring Boot 4.0.6 (Spring Framework 7)
- PostgreSQL, Flyway migrations
- springdoc-openapi (Swagger UI) for interactive API docs

## Running locally

```bash
docker compose up -d          # starts Postgres on localhost:5432
./mvnw spring-boot:run        # profile defaults to "local" — auth disabled
```

App: `http://localhost:8081`
Swagger UI: `http://localhost:8081/swagger-ui/index.html`
Raw OpenAPI spec: `http://localhost:8081/v3/api-docs`

On `local`, every request is auto-authenticated as a synthetic admin
(`local-admin`, see [`LocalAuthFilter`](src/main/java/com/pm/erp/auth/security/LocalAuthFilter.java)) —
no login needed. On `e2e`/`prod`, call `POST /auth/login` first (demo users:
`admin`/`admin123`, `teacher1`/`teacher123`, `teacher2`/`teacher123` — see
[`V900__demo_seed.sql`](src/main/resources/db/demo/V900__demo_seed.sql)) and
send the returned `accessToken` as `Authorization: Bearer <token>`. In Swagger
UI, click **Authorize** and paste the token.

## Profiles

Set via `SPRING_PROFILES_ACTIVE`. Defined in
[`application.yml`](src/main/resources/application.yml):

| Profile | Auth | Demo data | Notes |
|---|---|---|---|
| `local` | disabled (auto-admin) | yes | developer machine |
| `e2e` | JWT required | yes | integration/staging |
| `prod` | JWT required | no | `APP_JWT_SECRET` env var mandatory |

## Module map — where to change what

The codebase is organized by feature (vertical slice), not by technical layer.
Each feature package generally has `api/` (controllers + DTOs), `domain/`
(JPA entities + repositories), and `service/` (business logic).

| Package | Purpose | Change it when... |
|---|---|---|
| `auth/api`, `auth/service` | Login endpoint, "who am I" endpoint | Adding a new auth endpoint or changing the login request/response shape |
| `auth/jwt` | JWT issuing/parsing (`JwtService`), token config (`JwtProperties`) | Changing token claims, expiry, or signing |
| `auth/security` | Security filter chains (`SecurityConfig`), the two auth filters (`JwtAuthFilter` for e2e/prod, `LocalAuthFilter` for local), the `AuthenticatedUser` principal | Changing which paths are public, adding a new profile's filter chain, changing what's on the principal |
| `auth/domain` | `AppUser`, `Role` entities/repos | Changing the login table or adding a role |
| `students/*` | Student CRUD | Adding a student field, changing student endpoints |
| `teachers/*` | Teacher CRUD (also creates the linked login) | Adding a teacher field, changing how teacher accounts are provisioned |
| `academics/*` | Classes, sections, subjects, academic years | Changing the school's structural setup (grades, sections, subjects, year rollover) |
| `assignments/*` | Teacher → section/subject assignment, and the access-control helper (`canAccessSectionSubject`, `listForTeacher`) | Changing which sections/subjects a teacher can access — **this is the single source of truth for teacher scoping**, reused by attendance, marks, report cards, and analytics |
| `attendance/*` | Daily attendance entry + trend | Adding an attendance status, changing bulk-mark behavior |
| `marks/*` | Exams + per-student marks entry | Adding an exam field, changing marks validation/entry |
| `reports/*` | Report card PDF + JSON rendering (`ReportCardService`, uses Thymeleaf template + Flying Saucer for PDF) | Changing report card layout (`src/main/resources/templates/reports/report-card.html`) or the computed fields (rank, grade, totals) |
| `analytics/*` | Read-only computed dashboards (per-student and school-wide) | Adding a new analytics metric/chart |
| `common/error` | `ApiError` response shape, exception types (`NotFoundException`, `ConflictException`), `GlobalExceptionHandler` | Adding a new error type or changing the error response format |
| `common/tenant` | `SchoolContext` — reads the current user's `schoolId` from the security context | Any code that needs to scope a query by school (multi-tenancy is schema-ready but not yet enforced everywhere) |
| `common/config` | `OpenApiConfig` — Swagger metadata + security scheme | Changing API docs title/description or the auth scheme shown in Swagger |
| `common/logging` | `RequestLoggingFilter` — logs every request/response with status + duration | Changing what gets logged per-request |
| `common/audit` | `BaseEntity` — shared `createdAt`/`updatedAt` base class | Adding a new auditable field to all entities |
| `src/main/resources/db/migration` | Flyway schema migrations (`V1__schema.sql`, `V2__assignments_attendance.sql`) | **Any** schema change — add a new `V{n}__description.sql`, never edit an already-applied one |
| `src/main/resources/db/demo` | Demo/seed data (`V900__demo_seed.sql`), loaded on `local`/`e2e` only | Changing the sample school/users/data used for local dev and e2e testing |
| `src/main/resources/templates/reports` | Report card HTML template | Changing report card visual layout |
| `pom.xml` | Dependencies, Java version, build plugins | Adding a library, bumping Spring Boot |
| `docker-compose.yml` | Local Postgres container | Changing local DB port/credentials |
| `Dockerfile` | Multi-stage build for deployment (Render or any container host) | Changing the JDK/JRE version, build steps, or exposed port |
| `render.yaml` | Render Blueprint — provisions the managed Postgres + web service together | Changing what gets provisioned on Render or its env var wiring |

## Access control model

There's no `@PreAuthorize`-only story here — two layers combine:

1. **Role gate** (`@PreAuthorize("hasRole('ADMIN')")`) on endpoints that are
   admin-only outright (creating teachers, classes, subjects, exams, etc).
2. **Scope gate** — endpoints that both `ADMIN` and `TEACHER` can call
   (attendance, marks, analytics, report cards) additionally check, in the
   controller, that a non-admin caller's `TeacherAssignment` rows cover the
   requested section/subject. That logic lives in
   [`TeacherAssignmentService`](src/main/java/com/pm/erp/assignments/service/TeacherAssignmentService.java)
   and is duplicated as a `requireXAccess`/`requireSectionSubjectAccess`
   private method in each of `AttendanceController`, `MarksController`,
   `AnalyticsController`, `ReportCardController`. If this pattern needs to
   change, update all four call sites.

## API documentation

All endpoints are documented via springdoc-openapi/Swagger annotations
(`@Tag`, `@Operation`, `@Parameter`) directly on the controllers and DTOs —
see [`OpenApiConfig`](src/main/java/com/pm/erp/common/config/OpenApiConfig.java)
for the global setup. Run the app and open Swagger UI to browse and try
requests interactively; the same spec is available as raw JSON at
`/v3/api-docs` for generating clients.

## Tests

```bash
./mvnw test
```

## Deploying to Render

The repo ships a [`Dockerfile`](Dockerfile) (multi-stage: builds with the
Maven wrapper on `eclipse-temurin:21-jdk-alpine`, runs on the `21-jre-alpine`
slim image) and a [`render.yaml`](render.yaml) Blueprint that provisions a
free-tier managed Postgres plus the web service in one shot.

### Option A — Blueprint (recommended)

1. Push this repo to GitHub (a **public** repo, or connect Render to your
   GitHub account for private repos).
2. In the Render dashboard: **New → Blueprint**, pick this repo. Render reads
   `render.yaml` and creates both `erp-db` (Postgres) and `erp-backend` (web
   service) together, wiring the DB connection automatically.
3. Click **Apply**. First build takes a few minutes (Maven dependency
   download + Docker layer build).
4. Once live, your API is at `https://erp-backend-<random>.onrender.com`,
   Swagger UI at `.../swagger-ui/index.html`.

### Option B — Manual web service

1. **New → Web Service**, connect the repo, runtime = **Docker**
   (Render auto-detects the `Dockerfile`).
2. **New → PostgreSQL** (free tier is fine) to get a managed database —
   copy its **Internal Database URL**.
3. On the web service, set these environment variables:

   | Key | Value |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `e2e` (real JWT auth + demo seed data) or `prod` (real JWT auth, no seed data) |
   | `DATABASE_URL` | the Postgres **Internal Database URL** Render gives you (`postgres://user:pass@host:port/db` form) |
   | `APP_JWT_SECRET` | a random 32+ byte string — use Render's "Generate" button |
   | `APP_CORS_ALLOWED_ORIGINS` | your frontend's deployed origin, e.g. `https://erp-frontend.onrender.com` |

   `DATABASE_URL` doesn't need to be split into JDBC url/username/password —
   [`ErpApplication.applyRenderDatabaseUrl`](src/main/java/com/pm/erp/ErpApplication.java)
   converts it into `spring.datasource.{url,username,password}` system
   properties before Spring starts. If you'd rather set the JDBC datasource
   explicitly instead, use `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` /
   `SPRING_DATASOURCE_PASSWORD` — `SPRING_DATASOURCE_URL` being present skips
   the `DATABASE_URL` conversion entirely.

4. Health check path: `/actuator/health` (already public in every profile).
5. Deploy. Render builds the Docker image and starts the container with
   `PORT` set — the app reads `${PORT:8081}` for `server.port`, so no change
   needed there.

**Notes**

- The free Postgres plan expires after 90 days and the free web service
  spins down on idle (cold start ~30–60s on the next request) — fine for a
  demo, not for anything real.
- `render.yaml` defaults to `SPRING_PROFILES_ACTIVE=prod`, which skips demo
  seeding entirely — there's no school, no users, nothing to log in as until
  you create one. Connect to the deployed Postgres (Render dashboard →
  your database → **Connect** → copy the external connection command) and
  run:

  ```sql
  INSERT INTO school (name, code, address, phone, principal_name)
  VALUES ('My School', 'SCH-001', 'Address', '+1-000-000-0000', 'Principal Name');

  -- bcrypt hash of 'admin123' — change the password after first login
  INSERT INTO app_user (school_id, username, password_hash, enabled, role_id)
  VALUES ((SELECT id FROM school WHERE code = 'SCH-001'),
          'admin',
          '$2a$10$ov9sr.sHUZ2tJSDvpkXOMuBY4UpuPLhdxjIBC9Du.fTWBmBmATNYS',
          TRUE,
          (SELECT id FROM role WHERE name = 'ADMIN'));
  ```

  Then log in as `admin` / `admin123` via `POST /auth/login` and change the
  password (there's no change-password endpoint yet — either add one or
  update `password_hash` directly with a fresh bcrypt hash).
- Use `e2e` instead of `prod` in `render.yaml` if you'd rather have the demo
  login users (`admin`/`admin123`, `teacher1`/`teacher123`, `teacher2`/`teacher123`)
  and sample data pre-loaded on the deployed instance — fine for a demo,
  not for real data.
