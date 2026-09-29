# Digiteen User Service

## Run with Docker

```bash
docker compose up -d --build
```

Check:

```bash
docker compose ps
curl http://localhost:8080/actuator/health
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

In Swagger, call `POST /api/v1/auth/register`, copy `accessToken`, click
**Authorize**, and paste the token.

## Test

Tests run automatically during the Docker build. Run them again with:

```bash
docker compose build user-service
```

## Stop

```bash
docker compose down
```

Delete all local data and start clean:

```bash
docker compose down --volumes
docker compose up -d --build
```
