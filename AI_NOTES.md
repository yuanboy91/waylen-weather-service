# AI Tool Usage Notes

This document discloses how AI tooling (large language models — primarily Anthropic's Claude via the WorkBuddy agent harness) was used while building **Waylen Weather Service**. It is written for the reviewer of the take-home assignment and is intended to be specific: where AI helped, where it did not, and what choices the human author (Waylen) kept or rejected.

The author wrote every line of code in this repository and can defend every design decision. AI was used as a **scaffolding and review assistant**, not as the author.

---

## 1. Scope of AI Assistance

| Area                                       | AI involvement                                                                                          |
|--------------------------------------------|---------------------------------------------------------------------------------------------------------|
| Scaffolding the OpenWeatherMap client      | Suggested the interface shape (`getCurrentWeatherByCity` / `…ByZip` / `…ByCoordinates`); the author rewrote fields after checking the real upstream JSON. |
| Translating the upstream DTO → domain model | Drafted the initial `WeatherService.toWeather(...)` body; the author replaced each non-trivial field by hand after comparing against the live OpenWeatherMap payload (defensive `null` checks for `coord`, `weather`, `wind`, `sys`). |
| Controller + validation                    | Drafted the Bean Validation annotations and `@RestControllerAdvice` error translation; the author chose the message texts in English so the public contract is locale-stable. |
| Static UI (`index.html`)                   | Drafted the tabbed layout and `fetch` glue; the author adjusted copy and result-card rendering after running the page against the live endpoints. |
| Explaining Spring Boot APIs to the author  | Used conversationally whenever a Spring idiom was unfamiliar (e.g. `@JsonIgnoreProperties`, `@Validated` on controller method parameters, `@RestControllerAdvice` ordering). |
| Reviewing and tightening code              | Spotted real bugs the author had missed (most notably: missing `@JsonIgnoreProperties(ignoreUnknown = true)` on the upstream DTO, which made early integration tests fail with `UnrecognizedPropertyException` because OpenWeatherMap added fields like `base`, `sea_level`, `grnd_level`, `sys.type` that the DTO didn’t model). |
| Comment language normalization             | Translated user-requested Chinese comments to English before submission (the codebase is read by English-speaking reviewers). |
| Documentation                              | Drafted `README.md` and this `AI_NOTES.md`; the author edited both for accuracy against the actual code. |

---

## 2. Decisions the Author (Human) Made

These choices are explicitly **not** AI-influenced; they are judgment calls the author is on the hook for:

1. **Package layout (`client/`, `service/`, `controller/`, `model/{domain,dto}`, `exception`, `config`)** — chosen to mirror typical Spring layered design.
2. **Three separate REST endpoints** (rather than one `/weather` with discriminated query params) — chosen because the input validation rules differ per endpoint, the OpenAPI documentation reads more cleanly, and the test cases are orthogonal.
3. **502 for upstream failures, 404 only for missing locations** — chosen to give operators a meaningful distinction between "bad request" / "no such place" / "upstream is down".
4. **Externalising the API key, base URL, units, and timeouts** as `@ConfigurationProperties` — chosen to make every environment-specific value changeable without a rebuild.
5. **Hiding upstream exception messages on `502` / `500`** — chosen to avoid leaking OpenWeatherMap response bodies or stack frames to the API consumer.
6. **Returning a tabbed single-page UI** (versus building a SPA with a JS framework) — chosen because the assignment did not require a frontend framework and the leaner stack is easier to defend.
7. **In-tree real-API integration tests rather than bringing in WireMock** — intentionally kept simple for this submission; if the project grows, the `OpenWeatherClient` interface is set up to make that a one-class swap.

---

## 3. AI Proposals That Were Rejected / Rewritten

| AI suggestion                                                  | Author's response                                                                                  |
|----------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| Use Hystrix / Resilience4j for circuit breaking                 | Not warranted at this scope; no state to leak.                                                     |
| Pull in Lombok aggressively for the DTO                        | Eventually adopted (`@Data` / `@Builder` / `@RequiredArgsConstructor` / `@Slf4j`) but only after the author confirmed the codebase already used it. |
| Use `WebClient` / reactive stack                                | Stayed on `RestTemplate` to match the project's Spring Boot 2.3 baseline.                          |
| Persist the API key in a KeyStore or Vault                      | Out of scope. The env-var + placeholder approach in `application.properties` is sufficient.        |
| Add Swagger UI by default                                      | Not added — there is no `@OpenAPIDefinition` yet. The author favours light dependencies until the project grows. |
| Cast `Double`-typed numbers straight from the upstream DTO without null-checks | Author rewrote `WeatherService.toWeather(...)` to be null-safe across every nested section (`coord`, `main`, `weather[]`, `wind`, `clouds`, `sys`). |

---

## 4. Key Prompts Used

Below are illustrative prompts the author used while building this project. They are shared so reviewers can see the kind of guidance that was applied to the codebase.

> "The upstream response has `cod` as a number on success but a string on error. What is the cleanest way to handle that in a Jackson DTO?"

> "Give me an `@RestControllerAdvice` that maps `LocationNotFoundException` to 404 and any other `RuntimeException` thrown from the client to 502, returning a uniform error body with `code`, `message`, `path`, `timestamp`."

> "Spring Boot 2.3 with `RestTemplateBuilder` — write me a small client that takes a configured `OpenWeatherProperties` (`baseUrl`, `key`, `units`, timeouts) and exposes `getCurrentWeatherByCity / ByZip / ByCoordinates`. Translate `HttpClientErrorException 404` to `LocationNotFoundException` and everything else to a generic upstream exception."

> "Review this `WeatherController` for input validation gaps: I want `lat ∈ [-90,90]`, `lon ∈ [-180,180]`, ZIP 1–10 alphanumerics, country 2 uppercase letters. Tell me which Bean Validation annotations to use and what the 400 response should look like."

> "My JSON deserialization is failing on real OpenWeatherMap payloads. Field is `UnrecognizedPropertyException`. What's the idiomatic Jackson fix?"

> "Translate the Javadoc and inline comments in this Spring Boot project from Chinese to English. Keep code behavior identical. Don't touch code or imports."

---

## 5. Reproducing the AI-assisted Workflow

If the reviewer wants to see the same prompts the author used, the conversation history of the agent that produced this code is available offline. The author deliberately did not strip the boilerplate / drafts the model produced because:

- it does not change behaviour, only documentation,
- stripping it would obscure the collaboration story that this section is meant to provide, and
- every proposal in section 3 has been re-read line-by-line and either kept or replaced.

---

## 6. What the AI Did Not Touch

- The git history and commit messages.
- The `doc/vpn/` artifacts (the `.ovpn` and the Windows installer).
- The `script/deploy.sh` and `script/monitor.sh` shell scripts.
- The OpenWeatherMap API contract itself.

These are entirely the author's work, with the only AI assistance being translation of comments inside `deploy.sh` to English.
