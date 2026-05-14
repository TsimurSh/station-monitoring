## Getting Started
- Setup environment var. in docker-compose.yml or set it in application.yml if you don't use the Docker.

----------------------------------------------------------
### examples:
##### base-url: https://oba-example-server.org
##### key: "the same key as for OBA API"
##### agency: "agency_id" 

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
./gradlew build

# Run tests
./gradlew test

# Docker deployment (requires pre-built JAR)
./gradlew build && docker compose up -d
```

## Architecture

A Spring Boot 3.2 microservice that proxies the OneBusAway (OBA) transit API, exposing real-time station arrival data in SIRI XML format.

### Data flow

1. **Startup + daily 4 AM cron** — `StopIdsScheduler` fetches all stop IDs for the configured agency from OBA, groups them by station ID, and stores them in `StationRepository` (a thread-safe in-memory `ConcurrentHashMap` singleton).

2. **Request** — `StationController` exposes two endpoints:
    - `GET /station-monitoring` — real-time vehicle arrivals for a station
    - `GET /stations` — list all stations with their stop IDs

3. **Service** (`StationService`) validates the API key from the request, then delegates to `StopMonitoringClient`.

4. **Client** (`StopMonitoringClient`) calls OBA's SIRI stop-monitoring endpoint using Spring `RestClient`, using `parallelStream()` for concurrent requests across multiple stops per station. Responses are deserialized into `SiriDto` (Jackson, `@JsonIgnoreProperties(ignoreUnknown = true)`).

### Security: two independent layers

- **HTTP Basic Auth** — Spring Security in-memory user (`ADMIN` role required). Credentials come from `api.security.user` / `api.security.password` in `application.yml`.
- **API key** — validated in `StationService` against `api.key` from config. Returned as 401 if missing/wrong.

CORS is configured to allow all origins/methods/headers (`WebConfig` + `SecurityConfig`).

## Key configuration (`application.yml`)

All sensitive values come from environment variables:

| Property | Env var | Purpose |
|---|---|---|
| `api.base-url` | `OBA_BASE_URL` | OBA API base URL |
| `api.key` | `API_KEY` | OBA + internal API key |
| `api.agency` | `AGENCY_ID` | Transit agency ID |
| `api.security.user` / `.password` | — | HTTP Basic Auth credentials |

Profile `application-dev.yml` overrides for local development.

## Tech stack

- **Spring Boot 3.2.2** — web, security, scheduling
- **Spring RestClient** — HTTP client for OBA API calls
- **SpringDoc OpenAPI 2.6** — Swagger UI at `/swagger-ui.html`
- **Lombok** — `@Slf4j`, `@Data`, `@RequiredArgsConstructor` used throughout
- **Jackson** with `jackson-datatype-jsr310` for Java time support
- **Gradle Kotlin DSL** (`build.gradle.kts`)
- **Docker** — `eclipse-temurin:17` base image; `docker-compose.yml` for deployment

## Check OpenApi Docs
http://localhost:8080/swagger-ui/index.html
