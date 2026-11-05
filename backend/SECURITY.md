# API authentication

Robsen uses stateless HMAC-signed JWT bearer tokens. Set a strong secret of at
least 32 characters before starting Spring; do not commit or share the value:

```sh
JWT_SECRET="$(openssl rand -hex 32)" mvn spring-boot:run
```

The token lifetime defaults to one hour and can be changed with
`JWT_EXPIRATION_MS`. Set `FRONTEND_ORIGIN` to the deployed frontend origin when
using a browser on another origin.

`POST /api/auth/register` creates a regular `USER` account. Sign in with
`POST /api/auth/login` using `{ "identifier": "email-or-username", "password":
"..." }`; the response contains `accessToken`, `tokenType`, `expiresIn`, and a
safe user profile. Send the token on protected API calls using
`Authorization: Bearer <accessToken>`. `GET /api/auth/me` returns the signed-in
profile. Business and distribution endpoints require a valid token. Platform
user, business-role, and tenant administration require the `SUPER_ADMIN`
account role.
Public registration cannot select or change its role.

Platform accounts use `USER` or `SUPER_ADMIN`. Business roles are separate:
users receive business permissions through active business memberships. The
seeded business roles are `OWNER`, `ADMIN`, `MANAGER`, `SALES_MANAGER`,
`SALES_REPRESENTATIVE`, `INVENTORY_MANAGER`, `ORDER_MANAGER`,
`DISTRIBUTION_MANAGER`, `FINANCE_MANAGER`, `STAFF`, and `VIEWER`.

Super admins manage platform users, businesses, memberships, tenants, and
business roles in `/dashboard/admin`. Role permissions use resource/action
entries such as `orders:READ` and `orders:UPDATE`; the backend checks both the
assigned role and the active membership for the affected business. Read access
is limited to records associated with a user's active business memberships.
The product and product-category catalogs remain shared across businesses.

To provision an initial administrator automatically, provide these environment
variables before starting Spring. The password is never stored in source
control, is BCrypt-hashed through the normal user service, and must be at least
12 characters:

```sh
export BOOTSTRAP_ADMIN_EMAIL='admin@your-domain.example'
export BOOTSTRAP_ADMIN_USERNAME='admin'
export BOOTSTRAP_ADMIN_PASSWORD='use-a-unique-secret-of-at-least-12-chars'
mvn spring-boot:run
```

The account is created if its email does not already exist. If the configured
email already belongs to an account, startup promotes that account to
`SUPER_ADMIN` without changing its password. Leave
`BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` unset to disable
automatic admin creation.

## Local test administrator

For local development only, the `dev` Spring profile seeds a test super admin
in the configured database when `admin@robsen.local` does not already exist:

- Email: `admin@robsen.local`
- Username: `admin`
- Password: `RobsenDevAdmin!2026`

Start the backend with `SPRING_PROFILES_ACTIVE=dev`. The account is persisted
using the normal user service, so its password is BCrypt-hashed. Override the
test credentials with the `BOOTSTRAP_ADMIN_*` environment variables if needed.
Do not enable the `dev` profile or use these test credentials in production.

The Next.js frontend stores the access token in an HttpOnly, same-site cookie
and forwards authenticated requests server-side. Configure
`BACKEND_API_URL` for the frontend's connection to Spring.
