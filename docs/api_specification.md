# API Specification: `alldare-podcasts`

This document defines the REST endpoints, open RSS XML feeds, Strategy B streaming proxy, and gRPC interfaces for the **`alldare-podcasts`** microservice.

---

## 1. Public Open RSS & Atom Feed Endpoints

Base URL: `${PODCAST_URL}` (Defaults to `https://podcasts.alldare.online` or Gateway `/podcast/...`)

| Method | Endpoint Path | Content Type | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/podcast/{slug}/rss.xml` | `application/rss+xml` | Open RSS 2.0 channel feed XML with iTunes extensions. |
| `GET` | `/podcast/{slug}/atom.xml` | `application/atom+xml` | Open Atom 1.0 channel feed XML. |
| `GET` | `/podcast/{slug}/index.html` | `text/html` | Public web landing page for podcast show. |
| `GET` | `/podcasts/stream/{episodeId}.mp3` | `audio/mpeg` | Strategy B high-speed chunked MP3 streaming proxy (with optional DAI). |

---

## 2. Creator Management REST Endpoints

Server HTTP Port: `8089` | Ingress Route: `https://alldare.local/api/v1/podcasts/...`

| Method | Endpoint Path | Description | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/podcasts/shows` | Lists podcast show channels for active creator. | Required Bearer |
| `POST` | `/api/v1/podcasts/shows` | Creates a new podcast show channel. | Required Bearer |
| `POST` | `/api/v1/podcasts/episodes` | Publishes a new podcast episode. | Required Bearer |
| `GET` | `/api/v1/podcasts/shows/{slug}/syndications` | Checks global directory syndication status (Spotify, Apple, etc.). | Required Bearer |

---

## 3. Inter-Service gRPC Interface Specification

gRPC Server Port: `9089`
Proto Contract: `alldare-common/src/main/proto/podcasts.proto`

### `PodcastService` (gRPC)

```protobuf
service PodcastService {
  rpc GetShowBySlug (GetShowBySlugRequest) returns (ShowResponse);
  rpc GetEpisodes (GetEpisodesRequest) returns (EpisodesListResponse);
}
```
