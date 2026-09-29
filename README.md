# Digiteen User Service

User registration and authentication service for the Digiteen Digital Wallet assessment.

## Included

- Registration using email, phone, or both
- BCrypt password hashing
- Login with email or phone
- RS256 access tokens and rotating refresh tokens
- Refresh-token reuse detection
- Public JWKS endpoint for Wallet Service token verification
- PostgreSQL migrations with Flyway
- Optimistic locking using JPA `@Version` on `users` and `refresh_tokens`
- Structured logs with `X-Correlation-ID`
- Swagger/OpenAPI documentation

RabbitMQ and the transactional outbox are intentionally not part of this service. The PDF requires them for financial events, so they belong in `digiteen-wallet-service`.

## Build and run with Docker

Requirements: Docker Desktop or Docker Engine with Compose.

If this repository was run before the migrations were consolidated, reset the local
development database once. This deletes its existing local data:

```bash
docker compose down --volumes
```

From this repository, run:

```bash
docker compose up --build
```

Wait until both containers are healthy/running:

```bash
docker compose ps
```

The Compose stack contains only:

- `postgres`
- `user-service`

Flyway applies all migrations automatically during startup. Development RSA keys are generated once and persisted in the `user_jwt_keys` Docker volume.

## Swagger

Open:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Test authentication in Swagger:

1. Execute `POST /api/v1/auth/register`.
2. Copy `accessToken` from the response.
3. Click **Authorize**.
4. Paste only the access token; Swagger adds `Bearer` automatically.
5. Execute `GET /api/v1/users/me`.

## Useful commands

Follow application logs:

```bash
docker compose logs -f user-service
```

Run the Maven tests inside the Docker build:

```bash
docker compose build user-service
```

Stop the stack while keeping data:

```bash
docker compose down
```

Reset all local database data and generated development keys:

```bash
docker compose down --volumes
```

## API endpoints

| Method | Path | Authentication |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Public |
| `POST` | `/api/v1/auth/login` | Public |
| `POST` | `/api/v1/auth/refresh` | Public refresh token |
| `POST` | `/api/v1/auth/logout` | Public refresh token |
| `GET` | `/api/v1/users/me` | Bearer access token |
| `GET` | `/.well-known/jwks.json` | Public |
| `GET` | `/actuator/health` | Public |

## Optimistic locking

Both mutable tables have a `version BIGINT NOT NULL` column mapped with JPA `@Version`:

```text
users.version
refresh_tokens.version
```

Hibernate includes the current version in update statements and increments it after a successful update. If two requests update the same row concurrently, only the first succeeds; the other receives:

```http
409 Conflict
```

```json
{
  "code": "CONCURRENT_MODIFICATION",
  "message": "The resource was modified by another request; retry with the latest state"
}
```

The service does not use pessimistic database locks.

## Configuration

Copy `.env.example` to `.env` to override development defaults:

```bash
cp .env.example .env
```

Never use the example passwords or automatically generated signing keys in production.
