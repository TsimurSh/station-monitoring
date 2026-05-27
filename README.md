## Getting Started
- Setup environment variables in docker-compose.yml or set it in application.yml if you don't use the Docker.

----------------------------------------------------------
### examples for station monitor:

base-url: https://oba-example-server.org
key: "the same key as for OBA API"
agency: "agency_id" 

### examples for issuer:

secret: "test-hmac-secret"
time-window-ms: 60000

----------------------------------------------------------
## Run
Requirement:
- `java 17 + ` 
- `docker` - optional.

## Commands

```bash
# Run the application
./gradlew bootRun

# Build
./gradlew assemble

# Run tests
./gradlew test

# Docker deployment (requires pre-built JAR)
./gradlew build && docker compose up -d
```

## Architecture

A Spring Boot - microservice that proxies the OneBusAway (OBA) transit API, exposing real-time station arrival data in SIRI XML format.

### Data flow

1. **Startup + daily 4 AM cron** — `StopIdsScheduler` fetches all stop IDs for the configured agency from OBA, groups them by station ID, and stores them in `StationRepository` (a thread-safe in-memory `ConcurrentHashMap` singleton).

2. **Request** — `StationController` exposes two endpoints:
    - `GET /station-monitoring` — real-time vehicle arrivals for a station
    - `GET /stations` — list all stations with their stop IDs

   Plus `VerifyController` exposes `GET /verify-signature` for HMAC auth (see below).

3. **Service** (`StationService`) validates the API key from the request, then delegates to `StopMonitoringClient`.

4. **Client** (`StopMonitoringClient`) calls OBA's SIRI stop-monitoring endpoint using Spring `RestClient`, using `parallelStream()` for concurrent requests across multiple stops per station. Responses are deserialized into `SiriDto` (Jackson, `@JsonIgnoreProperties(ignoreUnknown = true)`).

### Security: two independent layers

- **HMAC SHA-256** — `HmacAuthenticationFilter` validates `x-signature` + `x-timestamp` headers on every protected request against `api.security.hmac.secret` with a `±time-window-ms` replay window (`HmacVerificationService`). On valid HMAC the filter sets a `PreAuthenticatedAuthenticationToken` with `ROLE_CLIENT`; otherwise Spring Security's entry point returns 401. Applied to `/station-monitoring` and `/stations`. The dedicated `/verify-signature` endpoint (for Apache2 sub-request auth) is `permitAll()` and uses `HmacVerificationService` directly — the filter explicitly skips it.
- **API key** — validated in `StationService` against `api.key` (the `key` query param). Returned as 401 if missing/wrong. Independent of HMAC: it gates which OBA agency-level key the caller may use.

Stateless session, CSRF/formLogin/httpBasic all disabled. No in-memory users — `api.security.user`/`password` removed.

CORS is configured to allow all origins/methods/headers (`WebConfig` + `SecurityConfig`).

## Key configuration (`application.yml`)

All sensitive values come from environment variables:

| Property | Env var | Purpose |
|---|---|---|
| `api.base-url` | `OBA_BASE_URL` | OBA API base URL |
| `api.key` | `API_KEY` | OBA + internal API key |
| `api.agency` | `AGENCY_ID` | Transit agency ID |
| `api.security.hmac.secret` | `HMAC_SECRET` | Shared HMAC secret with the frontend issuer |
| `api.security.hmac.time-window-ms` | `WINDOW_MS` | Allowed clock skew for HMAC timestamps (ms) |

Profile `application-dev.yml` provides ready-to-run values for all of the above (activate with `--spring.profiles.active=dev` or `SPRING_PROFILES_ACTIVE=dev`).

## OpenApi Docs
http://localhost:8080/swagger-ui/index.html
