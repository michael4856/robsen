# Business manager permissions

Robsen keeps platform role permissions in `roles` and stores the MANAGER permission snapshot separately for each business in `business_role_permission_configurations` and `business_role_enabled_permissions`. A configuration is unique for each business and role. Individual membership grants remain additive, except a manager cannot use an individual grant to restore a manager permission disabled in that business configuration.

## Defaults and existing businesses

New businesses receive the version 1 recommended MANAGER permissions in the same transaction that creates the owner membership. On application startup, businesses without a MANAGER configuration receive the same defaults. Startup only inserts missing configurations; it never overwrites owner changes. A future default change applies to new businesses only. To update an existing business, its owner can use **Reset to defaults**; increment `defaultsVersion` when introducing a new recommended baseline.

The project uses `spring.jpa.hibernate.ddl-auto=update` and has no Flyway or Liquibase migration setup. Hibernate creates the configuration and audit tables and the startup initializer backfills existing businesses.

## API

- `GET /api/business/{businessId}/roles/MANAGER/permissions`
- `PUT /api/business/{businessId}/roles/MANAGER/permissions` with `{ "permissions": ["products:READ", "products:UPDATE"] }`
- `POST /api/business/{businessId}/roles/MANAGER/permissions/reset`

The response includes recommended, current, and effective permission keys plus catalog labels, descriptions, enabled state, and default state. Only the active business OWNER or platform superadmin can use these routes. Permission changes are recorded in `business_role_permission_audit`; audit history is not exposed in the UI yet.

The backend recomputes membership authorities from the saved business configuration on authenticated requests and checks business-scoped actions again in the service layer. The MANAGER default can edit agreement drafts, but business members cannot change an agreement's status through the general update endpoint.
