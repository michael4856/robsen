# Robsen — Project Documentation and Test Scenarios

**Product:** Distribution network management platform  
**Document type:** Technical overview, user journeys, and verification scenarios  
**Repository snapshot:** 2026-11-03

This document describes behavior visible in the repository at the snapshot date. The test scenarios below are recommended verification cases; they do not imply that every manual or end-to-end case has been run.

> **Test-data safety:** Use disposable local accounts, businesses, and non-production data. Do not put real credentials, tokens, or production data in test records or evidence.

## Contents

1. [Product and scope](#1-product-and-scope)
2. [Architecture and request flow](#2-architecture-and-request-flow)
3. [Functional modules](#3-functional-modules)
4. [Identity, access control, and data](#4-identity-access-control-and-data)
5. [API surface and local configuration](#5-api-surface-and-local-configuration)
6. [Existing automated tests](#6-existing-automated-tests)
7. [Test scenarios and acceptance checks](#7-test-scenarios-and-acceptance-checks)
8. [Quality commands and limitations](#8-quality-commands-and-limitations)

## 1. Product and scope

Robsen is a web platform for distribution networks. It connects businesses, product catalogs, wholesale listings, distribution opportunities, partner applications, agreements, and orders. A shared dashboard supports business operations, while a separate platform-administration area manages accounts, business roles, and tenants.

### Primary actors

| Actor | Primary responsibilities |
|---|---|
| Visitor | Register or sign in; use the public-facing landing experience. |
| Business user | Manage a business workspace according to active membership and permissions. |
| Business owner or manager | Invite members, configure eligible business access, and manage business records. |
| Platform superadmin | Manage platform users, business roles, tenants, and cross-platform administration. |

### Key user journeys

- Register and authenticate, then reach the workspace with an authorized profile.
- Create a business; the creator becomes its active OWNER, the business starts pending verification, and manager defaults are initialized.
- Invite an existing user, assign an eligible business role, and allow the invitee to accept their own invitation.
- Publish products and listings, discover approved supplier inventory, then submit an order or partner application.
- Use role- and membership-based permissions to manage business records without crossing business boundaries.

## 2. Architecture and request flow

| Layer | Technology and responsibility |
|---|---|
| Web client | Next.js App Router 16, React 19, TypeScript 5, and Tailwind CSS 4; public site and authenticated dashboard. |
| Frontend API routes | Next.js server routes hold the HttpOnly access-token cookie and proxy API requests to Spring. |
| Application API | Spring Boot 4.1.1 and Java 25; Spring MVC REST controllers, services, validation, and Spring Security. |
| Persistence | Spring Data JPA and PostgreSQL; schema updates currently use Hibernate `ddl-auto=update`. |
| Authentication | Stateless HMAC-signed JWT bearer token, forwarded server-side by the frontend. |

### Request flow

1. The browser submits an action to a same-origin Next.js route.
2. The frontend server reads the HttpOnly cookie and forwards the request with `Authorization` to `BACKEND_API_URL`.
3. Spring validates the JWT, loads the current user authorities, and applies platform or business authorization.
4. A controller delegates to a service; the service validates business rules and persists through JPA repositories.
5. The JSON response returns to the frontend, which renders success or an actionable error.

### Repository layout

| Path | Contents |
|---|---|
| `backend/src/main/java/com/robsen/robsen` | Spring application, REST controllers, domain services, repositories, and security. |
| `backend/src/test/java` | JUnit tests for authentication, authorization, roles, bootstrap, business operations, and user uniqueness. |
| `robsen-frontend/app` | Next.js routes, dashboard pages, shared resource manager, UI components, and styles. |
| `robsen-frontend/app/api` | Same-origin authentication handlers and authenticated backend proxy. |
| `robsen-frontend/app/lib/resources` | Declarative frontend metadata for dashboard resources and forms. |
| `docs` | Project-specific documentation, including business manager permission behavior. |

## 3. Functional modules

| Module | Responsibilities |
|---|---|
| Identity and profile | Registration, login, profile retrieval/update, password change, token refresh, logout/session handling. |
| Platform administration | Users, platform USER/SUPER_ADMIN access, business roles, permission catalog, tenants. |
| Businesses and memberships | Business onboarding, owner membership, invitations, invitation acceptance, member status, per-business manager permissions. |
| Catalog and supply | Product categories, products, business product listings, stock and order constraints. |
| Distribution | Territories, opportunities, partner applications, distribution agreements and lifecycle. |
| Orders | Orders between buyer and supplier businesses, plus order line items, quantities, and prices. |

The dashboard resource metadata covers businesses, users, memberships, roles, categories, products, listings, territories, opportunities, partner applications, agreements, orders, order items, and tenants.

## 4. Identity, access control, and data

- Platform role (`USER` or `SUPER_ADMIN`) is separate from business role. Business permissions come from an active membership and its role/configuration.
- Platform user, business-role, and tenant administration routes require `SUPER_ADMIN`. The backend enforces access independently of frontend visibility.
- Business-scoped service operations verify the actor’s membership and permission against the target business. Include cross-business denial cases in testing.
- Public registration always creates a regular `USER`; it does not accept a client-selected superadmin role.
- The final active superadmin is protected from deactivation or role downgrade.
- The frontend uses an HttpOnly, same-site cookie and server-side request forwarding. Do not expose tokens to browser JavaScript or logs.
- Bootstrap accounts and JWT signing secrets must be configured outside source control. The local dev profile is for development only; never use its test configuration in production.
- Business manager permission defaults are initialized for new and unconfigured existing businesses. Startup backfill does not overwrite customized settings. Owners may reset a business configuration explicitly.

### Core data concepts

`User`, `Business`, `BusinessMembership`, `Role` and permissions, `Tenant`, `ProductCategory`, `Product`, `ProductListing`, `Territory`, `DistributionOpportunity`, `PartnerApplication`, `DistributionAgreement`, `CustomerOrder`, `OrderItem`, and business-specific role permission configuration/audit records.

### Database note

The checked-in configuration points to PostgreSQL for local development and currently relies on Hibernate schema update. No Flyway or Liquibase migration setup was identified. Treat schema evolution and production migrations as an explicit operational concern.

## 5. API surface and local configuration

| Prefix | Representative operations |
|---|---|
| `/api/auth` | Login, register, current profile, profile update, change password, refresh. |
| `/api/users` | Platform user create/list/get/update/deactivate. |
| `/api/business` | Create/list/get/update/deactivate, my businesses, marketplace, dashboard. |
| `/api/business-memberships` | Members, invitations, accept, assignable roles, permissions. |
| `/api/roles` and `/api/tenants` | Platform business-role and tenant administration. |
| `/api/product-categories`, `/api/products`, `/api/product-listings` | Catalog and supplier inventory. |
| `/api/territories`, `/api/distribution-opportunities` | Distribution areas and marketplace opportunities. |
| `/api/partner-applications`, `/api/distribution-agreements` | Partner workflow and agreements. |
| `/api/orders`, `/api/order-items` | Buyer/supplier orders and line items. |

### Local development (summary)

1. Start PostgreSQL and configure the backend datasource for the local database.
2. Configure `JWT_SECRET` with a strong environment-provided value. Configure `BOOTSTRAP_ADMIN_*` values when an initial administrator is needed. Keep secrets out of tracked files.
3. From `backend/`, run `mvn spring-boot:run`. The backend defaults to port 8080 unless overridden.
4. Copy `robsen-frontend/.env.example` to `.env.local` and configure `BACKEND_API_URL` for Spring (commonly `http://localhost:8080`).
5. From `robsen-frontend/`, run `npm ci` and `npm run dev`; open `http://localhost:3000`.
6. Enable the dev Spring profile only for local development. Do not reuse development credentials or secrets outside local testing.

See [`../backend/SECURITY.md`](../backend/SECURITY.md) for authentication and bootstrap configuration, [`../robsen-frontend/README.md`](../robsen-frontend/README.md) for frontend setup, and [`business-manager-permissions.md`](business-manager-permissions.md) for manager permission behavior.

## 6. Existing automated tests

Backend tests use JUnit 5 and Mockito through the Maven test lifecycle.

| Test class | Covered behavior (high level) |
|---|---|
| `RobsenApplicationTests` | Application context loads. |
| `AuthServiceTest` | Valid/invalid login, token refresh, registration role, profile permissions. |
| `JwtServiceTest` | Token creation/validation and expiry rejection. |
| `PublicAuthEndpointSecurityTest` | Public registration and invalid bearer-token responses. |
| `SecurityConfigurationTest` | Authorization routing for memberships, role permissions, and other resources. |
| `BusinessAccessServiceTest` | Permission evaluation against active membership for a business. |
| `BusinessServiceTest` | Business creation, owner membership, and current user’s active businesses. |
| `BootstrapAdminUserTest` | Bootstrap creation, existing-account behavior, disabled bootstrap, weak/missing config rejection. |
| `RoleBootstrapTest` / `RolePermissionsTest` | Default role seeding, permission normalization, and owner restrictions. |
| `BusinessRolePermissionServiceTest` | Default permissions, owner access/reset, cross-business denial, customization persistence. |
| `UserServiceTest` | Duplicate email and username errors. |

Frontend validation currently provides `npm run lint` and `npm run build`; package scripts do not define a frontend unit/E2E test runner. UI scenarios below are therefore manual acceptance scenarios unless a separate test harness is added.

## 7. Test scenarios and acceptance checks

**Priority:** P1 is a core release-blocking flow. P2 is important secondary behavior.

### 7.1 Authentication and profile

#### AUTH-01 — Public registration creates a standard account

- **Priority:** P1
- **Preconditions:** A unique test email and username are available.
- **Steps:** Open registration; submit valid name, email, username, and password.
- **Expected:** Account is created as USER; response/profile contains no password; user can sign in.

#### AUTH-02 — Sign in using email or username

- **Priority:** P1
- **Preconditions:** An active test account exists.
- **Steps:** Sign in once with email and once with username using the valid password.
- **Expected:** Both attempts succeed and open the authenticated workspace.

#### AUTH-03 — Reject invalid credentials

- **Priority:** P1
- **Preconditions:** Sign-in page is available.
- **Steps:** Submit a valid identifier with an incorrect password.
- **Expected:** Sign-in is rejected with a safe message; no protected session is established.

#### AUTH-04 — Expired or invalid token handling

- **Priority:** P1
- **Preconditions:** A signed-in browser session exists.
- **Steps:** Use an expired/invalid token, then request a protected page.
- **Expected:** API returns an authentication response; frontend clears the session and requests sign-in without exposing token contents.

#### AUTH-05 — Profile update and session renewal

- **Priority:** P2
- **Preconditions:** A signed-in user exists.
- **Steps:** Update allowed profile fields; wait for or trigger session refresh; reload profile.
- **Expected:** Updated profile is returned and the session remains valid; role and platform privileges cannot be changed through profile update.

### 7.2 Platform users and access control

#### USER-01 — Create a platform user with standard role

- **Priority:** P1
- **Preconditions:** Signed in as SUPER_ADMIN; unique email and username available.
- **Steps:** Open Administration > Platform users; create a USER with required fields.
- **Expected:** Record is created, password is stored as a hash, and user appears in the list.

#### USER-02 — Create a platform superadmin

- **Priority:** P1
- **Preconditions:** Signed in as SUPER_ADMIN; test email and username are unique.
- **Steps:** Create a user and select SUPER_ADMIN platform access.
- **Expected:** User is created with platform administrator access and can use protected admin routes after sign-in.

#### USER-03 — Duplicate email gives specific error

- **Priority:** P1
- **Preconditions:** A user already exists with the test email.
- **Steps:** Create another user using the same email, including a case-only variation.
- **Expected:** Creation is rejected; UI clearly identifies that the email is already registered; no duplicate row is created.

#### USER-04 — Duplicate username gives specific error

- **Priority:** P1
- **Preconditions:** A user already exists with the test username.
- **Steps:** Create another user with that username and a different email.
- **Expected:** Creation is rejected; UI says the username is already taken; existing data remains unchanged.

#### USER-05 — Protect the final active superadmin

- **Priority:** P1
- **Preconditions:** Only one active SUPER_ADMIN remains.
- **Steps:** Attempt to deactivate or downgrade that account.
- **Expected:** Change is rejected with a conflict message; final administrator remains active.

#### ACCESS-01 — Non-admin cannot access platform administration

- **Priority:** P1
- **Preconditions:** Signed in as a regular USER.
- **Steps:** Call platform user, role, or tenant endpoints and open admin-only screens.
- **Expected:** API denies unauthorized actions; frontend does not disclose or mutate admin data.

### 7.3 Business, membership, and permissions

#### BUS-01 — Create a business and owner membership

- **Priority:** P1
- **Preconditions:** Signed in as an active user.
- **Steps:** Submit a business with its required legal name.
- **Expected:** Business is created pending verification; creator becomes active OWNER; defaults are initialized.

#### MEM-01 — Invite a registered user to a business

- **Priority:** P1
- **Preconditions:** Actor has membership-create permission; target user and assignable role are active.
- **Steps:** Search for the target, select role, and send an invitation.
- **Expected:** Invitation is pending, associated with the selected business and role, and has an expiry.

#### MEM-02 — Prevent duplicate business invitations

- **Priority:** P1
- **Preconditions:** Target already has an ACTIVE membership or pending INVITED membership.
- **Steps:** Attempt to invite that same user to the same business again.
- **Expected:** API rejects the duplicate with a conflict; original membership/invitation is unchanged.

#### MEM-03 — Accept invitation with ownership and expiry checks

- **Priority:** P1
- **Preconditions:** One valid invitation exists for the test user; another belongs to a different user or is expired.
- **Steps:** Accept the valid invitation; try to accept the other user’s and expired invitations.
- **Expected:** Owner accepts and becomes ACTIVE; other-user acceptance is denied; expired invitation cannot be accepted.

#### PERM-01 — Owner changes manager permission configuration

- **Priority:** P1
- **Preconditions:** Business has OWNER and MANAGER; owner is signed in.
- **Steps:** Disable a manager permission, save, then reload configuration and evaluate manager access.
- **Expected:** Change persists only for that business and is effective for its managers; another business remains unchanged.

#### PERM-02 — Reset manager permissions to defaults

- **Priority:** P2
- **Preconditions:** Owner has customized manager permissions for a business.
- **Steps:** Choose Reset to defaults and confirm.
- **Expected:** Configuration returns to documented defaults; unsaved changes prompt before discard.

### 7.4 Catalog, distribution, and orders

#### CAT-01 — Manage catalog and business listings

- **Priority:** P1
- **Preconditions:** Admin or suitably authorized business user exists; category and business fixtures are available.
- **Steps:** Create a category, create a product, then create an active listing with quantity, MOQ, and price.
- **Expected:** Records retain valid references; listing appears only when eligible for discovery.

#### DISC-01 — Discover and filter marketplace listings

- **Priority:** P1
- **Preconditions:** At least one eligible supplier listing and one ineligible/unapproved listing exist.
- **Steps:** Open Discover; search by product, category, and supplier.
- **Expected:** Matching eligible listings appear; ineligible listings do not; empty search results show a useful empty state.

#### ORD-01 — Place a valid wholesale order

- **Priority:** P1
- **Preconditions:** Buyer has an eligible business; supplier has an active listing with stock.
- **Steps:** Order at or above MOQ and at or below available quantity.
- **Expected:** Order and line item are created with the selected buyer, supplier, listing, and quantity; confirmation is shown.

#### ORD-02 — Reject invalid order quantity

- **Priority:** P1
- **Preconditions:** A listing has known MOQ and available stock.
- **Steps:** Try quantity below MOQ, zero/non-integer quantity, and quantity above stock.
- **Expected:** Invalid quantities are rejected and stock is not over-allocated; user receives a clear reason.

#### DIST-01 — Apply to an open distribution opportunity

- **Priority:** P2
- **Preconditions:** An open opportunity exists and applicant has an eligible business.
- **Steps:** Submit an application with a message; attempt another invalid/duplicate application if supported.
- **Expected:** Application is saved with the intended opportunity and applicant business and appears in Partner applications.

#### DIST-02 — Review partner application and agreement lifecycle

- **Priority:** P2
- **Preconditions:** A pending application and draft agreement exist; reviewer has required business permissions.
- **Steps:** Update application status; create/update agreement; attempt a status change without permission.
- **Expected:** Permitted lifecycle changes persist; unauthorized status changes are rejected.

### 7.5 Tenant and UI quality

#### TEN-01 — Manage tenant as platform administrator

- **Priority:** P2
- **Preconditions:** Signed in as SUPER_ADMIN.
- **Steps:** Create tenant; edit its details; deactivate and list it.
- **Expected:** Tenant operations succeed for admin; inactive tenant is represented consistently; USER requests are denied.

#### UI-01 — Form validation and API error presentation

- **Priority:** P1
- **Preconditions:** Open a create/edit resource form.
- **Steps:** Submit missing required fields, duplicate identity data, and an API failure.
- **Expected:** Client validation blocks missing fields; API errors are visible and specific; saving state resets and user input is retained.

#### UI-02 — Responsive dashboard and table interactions

- **Priority:** P2
- **Preconditions:** Open dashboard at desktop and mobile viewport sizes.
- **Steps:** Navigate management pages; resize; use keyboard to operate menus, controls, tables, and dialogs.
- **Expected:** Content remains reachable without clipped controls; focus and labels are usable; horizontal table scrolling works where needed.

## 8. Quality commands and limitations

Run commands from the repository root unless a working directory is shown.

| Purpose | Command |
|---|---|
| Backend automated suite | `cd backend && mvn test` |
| Frontend lint | `cd robsen-frontend && npm run lint` |
| Frontend production build/type check | `cd robsen-frontend && npm run build` |
| Frontend local server | `cd robsen-frontend && npm run dev` |
| Backend local server | `cd backend && mvn spring-boot:run` |

### Known verification gaps

- No frontend automated component, integration, or browser E2E test command is configured in `package.json`.
- Manual marketplace/order/invitation scenarios require seeded records and a running PostgreSQL, Spring, and Next.js environment.
- The schema currently uses Hibernate update rather than versioned migration scripts; test upgrade/rollback processes separately before production releases.

Release evidence should record the code revision, environment, test data, pass/fail result, and any defects. Never include passwords, JWTs, personal production data, or connection strings in the evidence.
