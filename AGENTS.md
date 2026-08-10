# Workspace Guidelines for AI Agents (`alldare-podcasts`)

This microservice provides **Open RSS 2.0 / Atom 1.0 Podcast & Vodcast Syndication** and **Strategy B (High-Speed Live Chunked MP3 Streaming Proxy with Dynamic Audio Insertion)** for the Alldare Platform. Follow these rules, architectural constraints, and boundaries strictly when working in this directory.

---

## 1. Domain & Microservice Responsibilities

- **Syndication Engine:** Generates open RSS 2.0 XML feeds with full Apple Podcasts / iTunes extensions (`<itunes:author>`, `<itunes:duration>`, `<itunes:image>`, `<itunes:category>`, `<itunes:explicit>`) and Atom 1.0 feeds.
- **Strategy B Stream Proxy (`GET /podcasts/stream/{episodeId}.mp3`):** Pours raw binary MP3 frames directly into HTTP chunked response streams from DigitalOcean Spaces S3 CDN without on-the-fly CPU transcode re-encoding.
- **Dynamic Audio Insertion (DAI):** Queries `alldare-ads` via gRPC (`ads-service`) to inject audio sponsor spots (pre-roll/post-roll) into unmonetized free-tier streams while bypassing ads for `Alldare+` subscribers.
- **Port Mapping:** HTTP Server Port: `8089` | gRPC Server Port: `9089`.

---

## 2. Core Technical Constraints & Coding Standards

### Java & Spring Boot Standards
- **Runtime:** Java 25 with Spring Boot 4.x.
- **Mocking:** Always use `@MockitoBean` for bean mock injection in `@WebMvcTest` and `@SpringBootTest` suites.
- **Datastore & ORM:** PostgreSQL. JPA entities **must** use `java.util.UUID` for primary keys and `java.time.Instant` mapped to `TIMESTAMP WITH TIME ZONE` for temporal fields.
- **Database Migrations:** Schema migrations are managed exclusively via Flyway located in `src/main/resources/db/migration/`.

### Messaging & gRPC Inter-Service Integration
- **Stream Constants:** Never hardcode literal stream key strings (e.g. `"stream:posts"`). Always import centralized constants from `online.alldare.common.messaging.StreamKeys` (e.g. `StreamKeys.STREAM_POSTS`, `StreamKeys.STREAM_ADS_IMPRESSIONS`).
- **Error Resiliency:** Catch serialization and streaming IO exceptions gracefully. Stream failures must never cause memory leaks or unhandled thread crashes.
- **Protobuf Schemas:** Maintain gRPC contracts aligned with `online.alldare.common.grpc` schema definitions.

### Prohibition of Hardcoded Literals
- **No Magic Strings or Numbers:** Avoid inline hardcoded string literals, raw route paths, or magic numbers. Route paths must use centralized constants (e.g. `PodcastEndpoints`), and environment parameters must be loaded from `application.yaml` / `@Value` / `BuildKonfig`.

---

## 3. Operational & Safety Boundaries

- **No Automatic Commits:** Do **NOT** run `git commit`, `git push`, or alter git history autonomously.
- **Test Coverage:** Maintain a minimum **80% code coverage** (measured via JaCoCo) across controllers, services, and feed generators.
- **PCI & PII Compliance:** Never log or store raw PII or sensitive user tokens. Audio streaming metrics publish anonymized telemetry to Redis streams.
