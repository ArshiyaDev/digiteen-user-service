# Digiteen User Service

Requires Docker Compose. From this repository, start the app and PostgreSQL:

```bash
docker compose up -d --build --wait
```

Swagger: http://localhost:8080/swagger-ui.html

Register/login to get an `accessToken`; paste it into Wallet Swagger → **Authorize**.
Unit tests run during the build.

Stop (keeps data):

```bash
docker compose down
```
