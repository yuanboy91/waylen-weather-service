# Waylen Weather Service

A lightweight Spring Boot service for querying current weather by **city name**, **ZIP / postal code**, or **geographic coordinates**. It sits on top of the [OpenWeatherMap Current Weather API](https://openweathermap.org/current) and is designed to run inside a **private network reachable only via VPN**.

**Repository**: <https://github.com/yuanboy91/waylen-weather-service>

---

## Features

- **Three query modes**: city name / ZIP+country / lat-lon (dedicated REST endpoints)
- **Anti-corruption layer**: upstream JSON stays hidden inside `client/`; the business layer only sees the `WeatherResponse` domain model
- **Caffeine cache**: 10-min default TTL, tunable via `openweathermap.cache.*`; wired at the service layer with `@Cacheable`
- **Shared `RestTemplate`**: explicit connect/read timeouts + `User-Agent` header
- **Unified exception translation**: `404 LOCATION_NOT_FOUND` / `400 INVALID_ARGUMENT` / `502 UPSTREAM_ERROR` / `500 INTERNAL_ERROR`, all wrapped in the same `ApiResponse` shape
- **Input validation**: Bean Validation at the controller boundary, error messages in English (locale-agnostic)
- **Externalised configuration**: API key / base URL / units / timeouts all bound via `@ConfigurationProperties`
- **Actuator health**: `/actuator/health` consumed by the deploy script
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
| Service | `service` | Orchestration, DTO → Domain mapping, logging |
| Domain | `model.domain` | Application-owned models `WeatherResponse` / `ApiResponse` |
| Client | `client` | Upstream integration boundary, provider DTOs and exceptions |
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

- **Retry / circuit breaker**: the client has no retry-on-disconnect logic; Spring Retry could be introduced next
- **Authentication**: currently none — relies on VPN for perimeter security; a token gateway could be added next
- **HTTPS**: currently plain HTTP; external exposure needs nginx + TLS in front
- **Observability**: only `/health` today; Micrometer / Prometheus could be layered on
- **OpenAPI docs**: `springdoc-openapi-ui` could auto-generate a Swagger page
- **CI integration tests**: inject `OPENWEATHERMAP_API_KEY` via a secrets store in CI so `OpenWeatherClientTest` can actually run

---

© 2026 Waylen.
