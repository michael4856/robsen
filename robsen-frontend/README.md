# Robsen frontend

This Next.js application provides Robsen sign-in/registration and the
authenticated workspace for the Spring API.

## Configuration

Copy `.env.example` to `.env.local` and set `BACKEND_API_URL` to the Spring
server URL. The Spring API also requires `JWT_SECRET`; see
[`../backend/SECURITY.md`](../backend/SECURITY.md).

## Getting Started

Start the Spring backend and then run the frontend:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

Create an account from the landing page or sign in with an existing user, then
open the workspace to manage the backend's users, businesses, products,
distribution, orders, roles, and tenants.
