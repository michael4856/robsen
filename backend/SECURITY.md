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
profile. Business and distribution endpoints require a valid token. User,
system-role, and tenant administration additionally require the `ADMIN` role.
Public registration cannot select or change its role.

Admins can assign users to active roles from the Users page and configure each
role's visible dashboard sections from the Roles page. The profile endpoint
returns the assigned role's `navigationItems`, which the frontend uses for
navigation and page visibility. This is a presentation feature; backend API
authorization remains authoritative, and user, role, and tenant administration
still requires `ADMIN`.

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

The account is created only if its email does not already exist; startup will
not reset an existing account's password or role. Leave
`BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` unset to disable
automatic admin creation.

## Local test administrator

For local development only, the `dev` Spring profile seeds a test administrator
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
