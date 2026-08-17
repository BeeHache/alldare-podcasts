# Messaging & Events Specification: `alldare-podcasts`

This document defines the event channels, payload contracts, and telemetry streaming for the **`alldare-podcasts`** microservice.

---

## 1. Event Channel Mapping

`alldare-podcasts` publishes syndication and audio stream listening telemetry to Redis streams using centralized constants from `StreamKeys`.

| Event Type | Redis Stream Key (`StreamKeys`) | Payload DTO (`alldare-common`) | Description |
| :--- | :--- | :--- | :--- |
| `PODCAST_PUBLISHED` | `StreamKeys.PODCASTS` (`"stream:podcasts"`) | `PodcastPublishedEvent` | Emitted when a new podcast show or episode is published. |
| `STREAM_STARTED` | `StreamKeys.PODCASTS` (`"stream:podcasts"`) | `PodcastStreamStartedEvent` | Emitted when Strategy B MP3 stream proxy commences chunked transfer. |
| `DIRECTORY_SUBMITTED`| `StreamKeys.PODCASTS` (`"stream:podcasts"`) | `DirectorySubmittedEvent` | Emitted when RSS feed is submitted to Spotify/Apple Podcasts. |

---

## 2. Event Payload Contracts

### 2.1. `PodcastPublishedEvent`
```json
{
  "episodeId": "4c94b294-8255-4674-8b61-22920fbbad9e",
  "showId": "a823b123-5e92-4911-8e2b-112233445566",
  "slug": "deep-tech-talk",
  "title": "Episode 1: Decentralized RSS Syndication",
  "mediaUrl": "https://podcasts.alldare.online/podcast/deep-tech-talk/ep1.mp3",
  "publishedAt": "2026-08-17T02:00:00Z"
}
```
