# Waylen Weather Service

A lightweight Spring Boot service for querying current weather by **city name**, **ZIP / postal code**, or **geographic coordinates**. It sits on top of the [OpenWeatherMap Current Weather API](https://openweathermap.org/current) and is designed to run inside a **private network reachable only via VPN**.

**Repository**: <https://github.com/yuanboy91/waylen-weather-service>

---

## Features

- **Three query modes**: city name / ZIP+country / lat-lon (dedicated REST endpoints)
- **Anti-corruption layer**: upstream JSON stays hidden inside `client/`; the business layer only sees the `WeatherResponse` domain model
- **Caffeine cache**: 10-min default TTL, tunable via `openweathermap.cache.*`; wired at the service layer with `@Cacheable`
- **Shared `RestTemplate`**: explicit connect/read timeouts + `User-Agent` header
- **Unified exception translation**: `404 LOCATION_NOT_FOUND` / `400 INVALID_ARGUMENT` / `429 TOO_MANY_REQUESTS` / `502 UPSTREAM_ERROR` / `500 INTERNAL_ERROR`, all wrapped in the same `ApiResponse` shape
- **Input validation**: Bean Validation at the controller boundary, error messages in English (locale-agnostic)
- **Externalised configuration**: API key / base URL / units / timeouts all bound via `@ConfigurationProperties`
- **Rate limiting**: Guava `RateLimiter` on `/api/weather/**` (default 1 req/s, tunable via `app.rate-limit.per-second`), rejected requests map to `429 TOO_MANY_REQUESTS`
- **Actuator health**: `/actuator/health` consumed by the deploy script
- **Spring Boot Admin**: monitored by the standalone `waylen-admin-server` project (client registers itself at startup; console on port 9090)
- **Single-page UI** (`static/index.html`): tab-based query forms, vanilla `fetch` against `/api/weather/*`
- **VPN-only deployment**: see [VPN Access](#vpn-access--isolation-proof)

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
                          v                             |
                +-------------------+                   |
                |   WeatherService  | <-- logging ------+
                +---------+---------+
                          v
                +-------------------+
                |  OpenWeatherClient|   (interface)
                +---------+---------+
                          v
                +-------------------------+
                | DefaultOpenWeatherClient| --RestTemplate--> [Upstream]
                +-------------------------+
```

| Layer | Package | Responsibility |
|---|---|---|
| Web | `controller`, `exception` | REST mapping, input validation, error translation |
| Service | `service` | Orchestration + result caching |
| Domain | `model.domain` | Application-owned models `WeatherResponse` / `ApiResponse` |
| Client | `client` | Upstream integration boundary, payload → domain conversion (per client impl), provider DTOs and exceptions |
| Config | `config` | `@ConfigurationProperties` bindings |

---

## Project Structure

```
waylen-weather-service/
├── pom.xml
├── README.md / AI_NOTES.md
├── Makefile                          unified build/test/run/package entry
├── script/                           deploy.sh / monitor.sh
├── doc/                              client01.ovpn / openvpn-install-2.4.7-I607.exe / README.md
└── src/
    ├── main/java/com/waylen/weather/
    │   ├── WeatherApplication.java
    │   ├── config/                   OpenWeatherProperties / RestTemplateConfig / CacheConfig / WeatherCacheProperties
    │   ├── client/                   OpenWeatherClient (interface) + DefaultOpenWeatherClient (impl)
    │   ├── controller/               WeatherController
    │   ├── exception/                GlobalExceptionHandler / OpenWeatherMapException / LocationNotFoundException
    │   ├── model/                    domain/{WeatherResponse, ApiResponse} + dto/OpenWeatherDTO
    │   └── service/                  WeatherService
    └── test/java/com/waylen/weather/
        ├── client/OpenWeatherClientTest.java          real-API integration
        ├── client/DefaultOpenWeatherClientTest.java   unit: conversion + error translation
        ├── client/WeatherServiceCacheTest.java        cache hits / isolation / evict
        └── controller/WeatherControllerTest.java      @MockBean + TestRestTemplate
```

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | ≥ 8 | `pom.xml` `maven.compiler.source/target` |
| Maven | 3.6+ | no bundled `mvnw` — install your own |
| Network | outbound HTTPS to `api.openweathermap.org` | only needed for the real integration test |
| VPN | OpenVPN 2.4+ | required to reach the deployment environment |
| API key | OpenWeatherMap free tier | activation takes hours to days after signup |

---

## Quick Start

### 1. Clone

```bash
git clone https://github.com/yuanboy91/waylen-weather-service.git
cd waylen-weather-service
```

### 2. Build

```bash
mvn clean package -DskipTests
```

Artifact: `target/waylen-weather-service.jar`

### 3. Run

```bash
java -jar target/waylen-weather-service.jar
# or: mvn spring-boot:run
```

---

## API Reference

Base path: `/api/weather` &nbsp;·&nbsp; `Content-Type: application/json`

### `GET /city?city=...`

| Parameter | Type | Required | Constraints |
|---|---|---|---|
| `city` | string | yes | 1–100 chars, non-blank; supports `London,GB` suffix |

### `GET /zip?zip=...&country=...`

| Parameter | Type | Required | Constraints |
|---|---|---|---|
| `zip` | string | yes | 1–10 letters / digits / spaces / hyphens |
| `country` | string | yes | exactly 2 uppercase letters (ISO 3166-1 alpha-2) |

### `GET /coordinates?lat=...&lon=...`

| Parameter | Type | Required | Constraints |
|---|---|---|---|
| `lat` | float | yes | `-90.0` ≤ x ≤ `90.0` |
| `lon` | float | yes | `-180.0` ≤ x ≤ `180.0` |

### Swagger UI

Interactive API documentation is auto-generated by springdoc-openapi:

- Swagger UI: `http://localhost:8099/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8099/v3/api-docs`

### Example Calls

```bash
# 1. By city name (optionally with a country suffix)
curl -s 'http://localhost:8099/api/weather/city?city=London,GB'

# 2. By ZIP / postal code + ISO country code
curl -s 'http://localhost:8099/api/weather/zip?zip=10001&country=US'

# 3. By geographic coordinates
curl -s 'http://localhost:8099/api/weather/coordinates?lat=48.8566&lon=2.3522'
```

### Response Shape

Success responses are uniformly wrapped by the controller in `ApiResponse<T>`:

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "locationName": "London",
    "country": "GB",
    "condition": "Clouds",
    "description": "scattered clouds",
    "iconCode": "03d",
    "temperature": 20.9,
    "feelsLike": 20.6,
    "humidity": 64,
    "pressure": 1011,
    "wind": { "speed": 4.6, "degree": 230, "gust": 6.3 },
    "cloudiness": 40,
    "visibility": 10000
  },
  "timestamp": 1757080000000
}
```

### `GET /`

Single-page UI (`static/index.html`) with three query tabs.

### `GET /actuator/health`

`{"status":"UP"}` — used by the deploy script as a readiness probe.

### Spring Boot Admin (standalone server)

This app is monitored by the standalone `waylen-admin-server` project (sibling directory, port 9090): it registers itself at startup via `spring.boot.admin.client.url`, and the admin server aggregates this app's actuator endpoints into its monitoring UI.

- Console: `http://localhost:9090` — health details, metrics, env, caches, thread dump, live log view (`logs/info.log` via `/actuator/logfile`), and more
- Registry JSON: `curl -H 'Accept: application/json' http://localhost:9090/applications`
- Key client properties: `spring.boot.admin.client.url`, `spring.boot.admin.client.instance.service-base-url`, `management.endpoints.web.exposure.include=*`

---

## Error Handling

All error responses share a unified shape:

```json
{
  "code": "LOCATION_NOT_FOUND",
  "message": "city=NoSuchCityXYZ123",
  "path": "/api/weather/city",
  "timestamp": 1757080000000
}
```

---

## Testing

| Test Class | Style | Notes |
|---|---|---|
| `DefaultOpenWeatherClientTest` | unit (Mockito `RestTemplate` mock) | Payload → domain conversion and upstream error translation, no network |
| `WeatherServiceCacheTest` | `@SpringBootTest` | Verifies `@Cacheable` semantics; stubbed client, no network |
| `WeatherControllerTest` | `@SpringBootTest` + `@MockBean` + `TestRestTemplate` | HTTP-layer end-to-end, no API key required |
| `OpenWeatherClientTest` | `@SpringBootTest` (conditionally enabled) | Hits the real upstream API, gated by `@EnabledIfEnvironmentVariable`; skipped without an API key |

---

## Deployment

```bash
./script/deploy.sh            # pull, build, restart, health-check
./script/deploy.sh --rollback # rollback
```

Expected layout: `SRC_DIR=/home/deploy/waylen-weather-service`, `DEPLOY_DIR=/opt/application`, with `mvn` / `git` / `curl` / `java` on PATH. The script pulls `origin` → runs `mvn clean package -DskipTests` → backs up the old jar → starts the new one via `BUILD_ID=dontKillMe nohup` → polls `/actuator/health` for up to 30 s.

`script/monitor.sh` tails the log and polls the health endpoint alongside it.

---

## VPN Access & Isolation Proof

The deployment sits behind a private network reachable only through OpenVPN. Materials are in `doc/`:

| File | Purpose | Audience |
|---|---|---|
| `doc/client01.ovpn` | OpenVPN client configuration for the reviewer | reviewer |
| `doc/openvpn-install-2.4.7-I607.exe` | Windows GUI client installer | reviewer |
| `doc/README.md` | Connection steps + isolation-proof procedure | reviewer |

### Connect

1. Install `openvpn-install-2.4.7-I607.exe` (or any OpenVPN 2.4+ client)
2. Import `client01.ovpn`
3. Connect, then visit `http://<vpn-internal-host>:8099/`

---

## Limitations & Future Improvements

- **Circuit breaker**: transient upstream failures (network / 5xx / 429) are now retried by spring-retry (3 attempts, 500 ms backoff doubling each time); a circuit breaker remains a future option
- **Authentication**: currently none — relies on VPN for perimeter security; a token gateway could be added next
- **HTTPS**: currently plain HTTP; external exposure needs nginx + TLS in front
- **Observability**: monitored by the standalone `waylen-admin-server` (port 9090) with the full actuator endpoint set; Micrometer / Prometheus could still be layered on for metric scraping
- **CI integration tests**: inject `OPENWEATHERMAP_API_KEY` via a secrets store in CI so `OpenWeatherClientTest` can actually run

---

© 2026 Waylen.
