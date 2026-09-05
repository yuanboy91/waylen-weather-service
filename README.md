# Waylen Weather Service

A small Spring Boot service that exposes current weather lookups by **city name**, **ZIP / postal code**, or **geographic coordinates**, backed by the [OpenWeatherMap Current Weather API](https://openweathermap.org/current) and designed to be deployed inside a **private network accessible only via VPN**.

This repository implements the take-home assignment described in `Desktop/Take Home Assignment.docx`.

---

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Project Layout](#project-layout)
4. [Prerequisites](#prerequisites)
5. [Quick Start](#quick-start)
6. [API Reference](#api-reference)
7. [Error Handling](#error-handling)
8. [Configuration](#configuration)
9. [Tests](#tests)
10. [Deployment](#deployment)
11. [VPN Access & Isolation Proof](#vpn-access--isolation-proof)
12. [AI Tool Usage](#ai-tool-usage)
13. [Limitations & Future Work](#limitations--future-work)

---

## Features

- **Three query modes** supported through a uniform REST interface:
  - `GET /api/weather/city?city=London`
  - `GET /api/weather/zip?zip=10001&country=US`
  - `GET /api/weather/coordinates?lat=39.9042&lon=116.4074`
- **Anti-corruption layer**: third-party JSON shape stays inside `client/`; the rest of the app sees only the project's own `WeatherResponse` domain model, so the upstream provider can be swapped without rippling changes.
- **Built-in cache**: a Caffeine-backed `weather` cache (10-minute TTL by default, tunable via `openweathermap.cache.*`) absorbs repeated queries without re-hitting OpenWeatherMap. Wired through `@Cacheable` on the service layer; see [`CacheConfig`](src/main/java/com/waylen/weather/config/CacheConfig.java).
- **Shared `RestTemplate`** with explicit connect / read timeouts and a stable `User-Agent` header. Adding a new outbound HTTP dependency is a one-line bean change. See [`RestTemplateConfig`](src/main/java/com/waylen/weather/config/RestTemplateConfig.java).
- **Uniform exception translation**: `404 LOCATION_NOT_FOUND`, `400 INVALID_REQUEST`, `502 UPSTREAM_ERROR`, `500 INTERNAL_ERROR` — all delivered as a single `ApiErrorResponse` JSON shape.
- **Input validation** at the controller boundary (Bean Validation / JSR-303), with locale-stable English error messages.
- **Externalized configuration** for the upstream API key, base URL, units, and connect / read timeouts — no secrets in source code.
- **Health endpoint** exposed via Spring Boot Actuator (`/actuator/health`) and consumed by the deployment script.
- **Decoupled, mockable client interface** (`OpenWeatherClient`) for unit testing without hitting the network.
- **Single-page UI** (`static/index.html`) with a tabbed query form that calls the API via `fetch`.
- Designed for **private network deployment**: see [VPN Access](#vpn-access--isolation-proof).

---

## Architecture

```
                +-------------------+
   Browser ---> | /api/weather/*    |
                | static/index.html |        +---------------------+
                +---------+---------+        |   OpenWeatherMap    |
                          |                  |   Current Weather   |
                          v                  +----------+----------+
                +-------------------+                   |
                | WeatherController |                   |
                +---------+---------+                   |
                          |                             |
                          v                             |
                +-------------------+                   |
                |   WeatherService  | <-- logs every --+
                +---------+---------+   call with query
                          |             params
                          v
                +-------------------+
                |  OpenWeatherClient|
                |   (interface)     |
                +---------+---------+
                          |
                          v
                +-------------------------+
                | DefaultOpenWeatherClient|--- RestTemplate ---> [Upstream API]
                +-------------------------+
```

### Layering at a glance

| Layer          | Package                              | Responsibility                                                 |
|----------------|--------------------------------------|----------------------------------------------------------------|
| Web            | `controller`, `exception`            | REST mapping, request validation, error translation            |
| Service        | `service`                            | Orchestration, upstream → domain mapping, logging               |
| Domain         | `model.domain`                       | Application-facing immutable models (`WeatherResponse`, `ApiErrorResponse`) |
| Client         | `client`                             | Third-party integration boundary; wire types + its own exceptions |
| Config         | `config`                             | `@ConfigurationProperties` binding for upstream settings        |

---

## Project Layout

```
waylen-weather-service/
├── pom.xml
├── README.md                      <- this file
├── AI_NOTES.md                    <- disclosure of AI tooling usage
├── script/
│   ├── deploy.sh                  <- production deployment script (uses /actuator/health)
│   └── monitor.sh                 <- lightweight liveness probe
├── doc/
│   └── vpn/                       <- VPN client profile + installer
│       ├── client01.ovpn
│       └── openvpn-install-2.4.7-I607.exe
└── src/
    ├── main/
    │   ├── java/com/waylen/weather/
    │   │   ├── WeatherApplication.java
    │   │   ├── config/OpenWeatherProperties.java
    │   │   ├── client/
    │   │   │   ├── OpenWeatherClient.java                 (interface)
    │   │   │   └── DefaultOpenWeatherClient.java          (RestTemplate impl)
    │   │   ├── controller/WeatherController.java
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── LocationNotFoundException.java
    │   │   │   └── OpenWeatherMapException.java
    │   │   ├── model/
    │   │   │   ├── domain/
    │   │   │   │   ├── WeatherResponse.java
    │   │   │   │   └── ApiErrorResponse.java
    │   │   │   └── dto/OpenWeatherDTO.java                (upstream wire format)
    │   │   └── service/WeatherService.java
    │   └── resources/
    │       ├── application.properties
    │       ├── logback.xml
    │       └── static/index.html                          (UI)
    └── test/java/com/waylen/weather/
        ├── controller/WeatherControllerTest.java
        └── client/OpenWeatherClientTest.java
```

---

## Prerequisites

| Tool       | Version       | Notes                                                       |
|------------|---------------|-------------------------------------------------------------|
| JDK        | 1.8 (≥ 8)     | Required by `pom.xml` `maven.compiler.source` / `target`    |
| Maven      | 3.6+          | `mvnw` is **not** vendored; supply your own installation    |
| Network    | outbound HTTPS to `api.openweathermap.org` | For real integration tests                  |
| VPN client | OpenVPN 2.4+  | For reaching the deployment when running behind the VPN     |
| API key    | OpenWeatherMap | Free tier is sufficient; activate early — it takes hours to days |

> All commands below assume your working directory is the project root.

---

## Quick Start

### 1. Configure the API key

The application reads its OpenWeatherMap key from the `OPENWEATHERMAP_API_KEY` environment variable, with a fallback to a placeholder in `application.properties` for local convenience.

```bash
# macOS / Linux
export OPENWEATHERMAP_API_KEY=<your-key-from-openweathermap.org>

# Windows (PowerShell)
$env:OPENWEATHERMAP_API_KEY = "<your-key>"
```

A free key is generated at <https://openweathermap.org/api>.

### 2. Build

```bash
mvn clean package -DskipTests
```

The runnable jar lands at `target/waylen-weather-service.jar`.

### 3. Run

```bash
# From the jar (preferred for production-like runs)
java -jar target/waylen-weather-service.jar

# Or, with Maven
mvn spring-boot:run
```

By default the service listens on `http://localhost:8099`.

### 4. Open the UI

Visit <http://localhost:8099/> in a browser — a tabbed query form is served from `static/index.html`.

### 5. Probe health

```bash
curl -s http://localhost:8099/actuator/health
# {"status":"UP"}
```

---

## API Reference

Base path: `/api/weather` &nbsp;·&nbsp; Content-Type: `application/json`

### `GET /city`

Look up current weather by city name. The `city` value is forwarded to OpenWeatherMap as-is, so suffixes like `London,GB` are accepted.

| Parameter | Type   | Required | Notes                                       |
|-----------|--------|----------|---------------------------------------------|
| `city`    | string | yes      | 1 – 100 chars; must not be blank            |

```bash
curl -s 'http://localhost:8099/api/weather/city?city=London'
```

Sample response (`200 OK`):

```json
{
  "locationName": "London",
  "country": "GB",
  "coordinates": { "lat": 51.5074, "lon": -0.1278 },
  "condition": "Clouds",
  "description": "scattered clouds",
  "iconCode": "03d",
  "temperature": 20.9,
  "feelsLike": 20.6,
  "tempMin": 19.4,
  "tempMax": 22.1,
  "humidity": 64,
  "pressure": 1011,
  "wind": { "speed": 4.6, "degree": 230, "gust": null },
  "cloudiness": 40,
  "visibility": 10000,
  "timestamp": 1757080000,
  "sunrise": 1757043600,
  "sunset": 1757092800,
  "timezoneOffset": 3600
}
```

### `GET /zip`

Look up current weather by ZIP / postal code. Country is required (ISO 3166-1 alpha-2, uppercase).

| Parameter | Type   | Required | Constraint                                                       |
|-----------|--------|----------|------------------------------------------------------------------|
| `zip`     | string | yes      | 1–10 letters / digits / spaces / hyphens                         |
| `country` | string | yes      | exactly 2 uppercase letters (e.g. `US`, `GB`)                    |

```bash
curl -s 'http://localhost:8099/api/weather/zip?zip=10001&country=US'
```

### `GET /coordinates`

Look up current weather by latitude / longitude.

| Parameter | Type  | Required | Constraint          |
|-----------|-------|----------|---------------------|
| `lat`     | float | yes      | `-90.0` ≤ x ≤ `90.0` |
| `lon`     | float | yes      | `-180.0` ≤ x ≤ `180.0` |

```bash
curl -s 'http://localhost:8099/api/weather/coordinates?lat=39.9042&lon=116.4074'
```

### Response shape

All success responses share the `WeatherResponse` shape shown above. `null` fields are omitted from the JSON payload (`@JsonInclude(NON_NULL)`), and all numeric fields use the unit configured by `openweathermap.api.units` (default `metric` → °C, m/s, hPa, m).

### `GET /` (UI)

A single-page static UI is served from `static/index.html`. It exposes three query tabs (City / ZIP / Coordinates) and renders the result as a card.

### `GET /actuator/health`

Standard Spring Boot Actuator endpoint — returns `{"status":"UP"}` when the app is healthy. Consumed by `script/deploy.sh` for deploy-time readiness probing and rollback validation.

---

## Error Handling

All error responses use the following shape (returned by `GlobalExceptionHandler`):

```json
{
  "code": "LOCATION_NOT_FOUND",
  "message": "city=NoSuchCityXYZ123",
  "path": "/api/weather/city",
  "timestamp": 1757080000000
}
```

| HTTP | `code`               | When                                                |
|------|----------------------|-----------------------------------------------------|
| 400  | `INVALID_REQUEST`    | A `@RequestParam` failed Bean Validation (blank, bad regex, lat/lon out of range) |
| 404  | `LOCATION_NOT_FOUND` | Upstream reported the location does not exist       |
| 502  | `UPSTREAM_ERROR`     | Upstream returned 4xx (other than 404) / 5xx, timed out, or could not be reached |
| 500  | `INTERNAL_ERROR`     | Any uncaught exception                              |

The external `message` for 502/500 deliberately hides internal details to avoid leaking upstream error bodies or stack frames.

---

## Configuration

All configuration lives in `src/main/resources/application.properties`.

| Key                                       | Default                | Description                                      |
|-------------------------------------------|------------------------|--------------------------------------------------|
| `server.port`                             | `8099`                 | HTTP listener port                               |
| `openweathermap.api.base-url`             | `…/data/2.5`           | Upstream root, `/weather` is appended internally |
| `openweathermap.api.key`                  | `${OPENWEATHERMAP_API_KEY:REPLACE_WITH_OPENWEATHERMAP_KEY}` | Resolved from env var; the placeholder is intentionally non-key-looking so secret scanners do not flag it |
| `openweathermap.api.units`                | `metric`               | `metric` / `imperial` / `standard`               |
| `openweathermap.api.connect-timeout-ms`   | `3000`                 | TCP connect timeout                              |
| `openweathermap.api.read-timeout-ms`      | `5000`                 | Socket read timeout                              |
| `openweathermap.cache.ttl`                | `10m`                  | Caffeine TTL on the `weather` cache              |
| `openweathermap.cache.maximum-size`       | `500`                  | Caffeine max entries on the `weather` cache      |
| `management.endpoints.web.exposure.include` | `health,info`        | Which actuator endpoints to expose               |

The key is intentionally **not** hard-coded; an environment variable override is the production expectation. The literal in the properties file is a non-key-looking marker (`REPLACE_WITH_OPENWEATHERMAP_KEY`) — GitHub's secret scanner would otherwise flag a real key with a similar shape. Set the env var (or replace the marker locally) before any deployment.

---

## Tests

```bash
# Offline tests (cache + controller): run without an API key
make test-offline
# or directly:
mvn test -Dtest='WeatherServiceCacheTest,WeatherControllerTest'

# Full suite, including the live OpenWeatherMap client test
export OPENWEATHERMAP_API_KEY=<your-key>
mvn test

# Compile only
mvn test-compile
```

| Test class                         | Style            | Notes                                                                                          |
|------------------------------------|------------------|------------------------------------------------------------------------------------------------|
| `WeatherServiceCacheTest`          | `@SpringBootTest` | Verifies `@Cacheable` semantics: same key → upstream called once; different keys → separate calls. Stubbed client, no network. |
| `WeatherControllerTest`            | `@SpringBootTest` + `@MockBean` + `TestRestTemplate` | Drives the HTTP layer end-to-end with the client stubbed. Verifies parameter validation, normal responses, and the error contract — does **not** require an API key. |
| `OpenWeatherClientTest`            | `@SpringBootTest` (gated) | Hits the real upstream API. Class is annotated `@EnabledIfEnvironmentVariable("OPENWEATHERMAP_API_KEY")` so it is skipped when the env var is missing. Acts as a smoke / regression test for the client integration. |

> The client is an **interface** (`OpenWeatherClient`) precisely so future test additions can plug in a `MockRestServiceServer` or a hand-rolled stub without depending on network reachability.

---

## Deployment

`script/deploy.sh` is the production deployment entry-point. It expects:

- A working tree at `SRC_DIR=/home/deploy/waylen-weather-service`
- A writable application directory at `DEPLOY_DIR=/opt/application`
- `mvn`, `git`, `curl`, `java` on the deployer's `PATH`

Usage:

```bash
./script/deploy.sh            # Pull, build, restart, and health-check
./script/deploy.sh --rollback # Roll back to the previous jar
```

The script pulls from `origin`, packages with `mvn clean package -DskipTests`, backs up the running jar, launches the new jar with `BUILD_ID=dontKillMe nohup`, and probes `http://localhost:8099/actuator/health` up to 10 × 3 s before declaring the deploy a failure.

`script/monitor.sh` is a companion one-shot script that polls the same health endpoint and tails the application log.

---

## VPN Access & Isolation Proof

The project expects the service to run inside a **private network reachable only through an OpenVPN tunnel**. The deployment artifacts are bundled in `doc/vpn/`:

| File                                       | Purpose                                                              |
|--------------------------------------------|----------------------------------------------------------------------|
| `doc/vpn/client01.ovpn`                    | OpenVPN client profile for the reviewer                               |
| `doc/vpn/openvpn-install-2.4.7-I607.exe`   | Windows installer for the OpenVPN GUI client                          |

### Connecting

1. Install `openvpn-install-2.4.7-I607.exe` on a Windows host (or use any OpenVPN 2.4+ client).
2. Import `client01.ovpn`.
3. Connect. Once the tunnel is up, the service is reachable at `http://<vpn-internal-host>:8099/`.

### Proof of isolation

The reason the assignment specifically asks for evidence is that VPN-only accessibility is **the** hardest requirement to verify visually. Captured for the reviewer:

- A successful `GET http://<host>:8099/actuator/health` **with the VPN connected** (`{"status":"UP"}`).
- A failed `curl --max-time 5 http://<host>:8099/` **without the VPN connected**, ending in either `Connection timed out` or a `No route to host` error.
- Network configuration screenshots from the reviewer’s host showing: (a) the OpenVPN adapter assigned and route pushed to the service subnet, (b) the same query failing when the VPN is disconnected.

These commands and screenshots live alongside the deployment package and can be regenerated by anyone who follows the deployment steps.

---

## AI Tool Usage

A disclosure of which AI tooling was used, where, and how, lives in [`AI_NOTES.md`](AI_NOTES.md). Reviewers are encouraged to read it alongside this README.

---

## Limitations & Future Work

- **Retry / circuit breaker**: the client has no retries for transient 5xx or connection drops. Adding Spring Retry or Resilience4j would harden it under flaky network conditions.
- **Authentication**: there is currently no auth on the API; because the service sits behind a VPN, this is assumed to be sufficient for the assignment. A token-based gateway in front would be the next step.
- **HTTPS**: the service serves plain HTTP. A terminating reverse proxy (nginx) with TLS would be required before exposing it beyond the VPN.
- **Observability beyond `/health`**: bringing in Micrometer / Prometheus would make the actuator surface a lot more useful operationally.
- **OpenAPI doc**: adding `springdoc-openapi-ui` would auto-generate a Swagger page from the controller.
- **Real-environment integration test gating**: `OpenWeatherClientTest` is gated on `OPENWEATHERMAP_API_KEY` being present, so `mvn test` is green without one but skips the live calls; in CI the key should be injected via secret store.

---

© 2026 Waylen. Code released for review of the take-home submission.
