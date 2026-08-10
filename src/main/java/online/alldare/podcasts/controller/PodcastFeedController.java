package online.alldare.podcasts.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import online.alldare.podcasts.domain.PodcastEpisode;
import online.alldare.podcasts.domain.PodcastShow;
import online.alldare.podcasts.repository.PodcastEpisodeRepository;
import online.alldare.podcasts.repository.PodcastShowRepository;
import online.alldare.podcasts.service.PodcastFeedService;
import online.alldare.podcasts.service.PodcastStreamProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PodcastFeedController {

    private static final CacheControl FEED_CACHE_CONTROL = CacheControl.maxAge(java.time.Duration.ofSeconds(300))
            .cachePublic()
            .sMaxAge(java.time.Duration.ofSeconds(600));

    @Autowired
    private PodcastShowRepository showRepository;

    @Autowired
    private PodcastEpisodeRepository episodeRepository;

    @Autowired
    private PodcastFeedService feedService;

    @Autowired
    private PodcastStreamProxyService streamProxyService;

    // --- RSS / Atom Feed Endpoints ---

    @GetMapping(value = "/podcasts/shows/{slug}/rss.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getRssFeed(@PathVariable("slug") String slug) {
        Optional<String> rssOpt = feedService.getRssFeed(slug);
        if (rssOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String rssXml = rssOpt.get();
        String etag = computeEtag(rssXml);

        return ResponseEntity.ok()
                .cacheControl(FEED_CACHE_CONTROL)
                .eTag(etag)
                .header(HttpHeaders.CONTENT_TYPE, "application/rss+xml; charset=UTF-8")
                .body(rssXml);
    }

    @GetMapping(value = "/podcasts/shows/{slug}/atom.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getAtomFeed(@PathVariable("slug") String slug) {
        Optional<String> atomOpt = feedService.getAtomFeed(slug);
        if (atomOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String atomXml = atomOpt.get();
        String etag = computeEtag(atomXml);

        return ResponseEntity.ok()
                .cacheControl(FEED_CACHE_CONTROL)
                .eTag(etag)
                .header(HttpHeaders.CONTENT_TYPE, "application/atom+xml; charset=UTF-8")
                .body(atomXml);
    }

    // --- Media Streaming Proxy Endpoints ---

    @GetMapping(value = "/podcasts/stream/{episodeId}.mp3")
    public void streamAudioEpisode(@PathVariable("episodeId") String episodeIdStr,
                                    @RequestParam(name = "subscriber", defaultValue = "false") boolean isSubscriber,
                                    HttpServletResponse response) {
        try {
            UUID episodeId = UUID.fromString(episodeIdStr);
            Optional<PodcastEpisode> episodeOpt = episodeRepository.findById(episodeId);
            if (episodeOpt.isEmpty()) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }

            PodcastEpisode episode = episodeOpt.get();
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("audio/mpeg");
            response.setHeader("Transfer-Encoding", "chunked");

            OutputStream outputStream = response.getOutputStream();
            streamProxyService.streamEpisodeWithDai(episode, outputStream, isSubscriber);
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @GetMapping(value = "/podcasts/stream/{episodeId}.mp4")
    public void streamVideoEpisode(@PathVariable("episodeId") String episodeIdStr,
                                    @RequestParam(name = "subscriber", defaultValue = "false") boolean isSubscriber,
                                    HttpServletResponse response) {
        try {
            UUID episodeId = UUID.fromString(episodeIdStr);
            Optional<PodcastEpisode> episodeOpt = episodeRepository.findById(episodeId);
            if (episodeOpt.isEmpty()) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }

            PodcastEpisode episode = episodeOpt.get();
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("video/mp4");
            response.setHeader("Transfer-Encoding", "chunked");

            OutputStream outputStream = response.getOutputStream();
            streamProxyService.streamEpisodeWithDai(episode, outputStream, isSubscriber);
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // --- Podcast Show CRUD Endpoints ---

    @PostMapping("/api/v1/podcasts/shows")
    public ResponseEntity<PodcastShow> createShow(@RequestBody PodcastShow show) {
        if (show.getId() == null) {
            show.setId(UUID.randomUUID());
        }
        PodcastShow savedShow = showRepository.save(show);
        feedService.evictFeedCache();
        return ResponseEntity.status(HttpStatus.CREATED).body(savedShow);
    }

    @GetMapping("/api/v1/podcasts/shows/{slug}")
    public ResponseEntity<PodcastShow> getShowBySlug(@PathVariable("slug") String slug) {
        return showRepository.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/podcasts/shows/creator/{creatorId}")
    public ResponseEntity<List<PodcastShow>> getShowsByCreatorId(@PathVariable("creatorId") UUID creatorId) {
        List<PodcastShow> shows = showRepository.findAllByCreatorId(creatorId);
        return ResponseEntity.ok(shows);
    }

    @PutMapping("/api/v1/podcasts/shows/{id}")
    public ResponseEntity<PodcastShow> updateShow(@PathVariable("id") UUID id, @RequestBody PodcastShow showDetails) {
        Optional<PodcastShow> showOpt = showRepository.findById(id);
        if (showOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PodcastShow existingShow = showOpt.get();
        if (showDetails.getTitle() != null) {
            existingShow.setTitle(showDetails.getTitle());
        }
        if (showDetails.getDescription() != null) {
            existingShow.setDescription(showDetails.getDescription());
        }
        if (showDetails.getCategory() != null) {
            existingShow.setCategory(showDetails.getCategory());
        }
        if (showDetails.getAuthorName() != null) {
            existingShow.setAuthorName(showDetails.getAuthorName());
        }
        if (showDetails.getEmail() != null) {
            existingShow.setEmail(showDetails.getEmail());
        }
        if (showDetails.getCoverImageUrl() != null) {
            existingShow.setCoverImageUrl(showDetails.getCoverImageUrl());
        }
        if (showDetails.getSlug() != null) {
            existingShow.setSlug(showDetails.getSlug());
        }
        existingShow.setExplicit(showDetails.isExplicit());
        existingShow.setUpdatedAt(Instant.now());

        PodcastShow updatedShow = showRepository.save(existingShow);
        feedService.evictFeedCache();
        return ResponseEntity.ok(updatedShow);
    }

    @DeleteMapping("/api/v1/podcasts/shows/{id}")
    public ResponseEntity<Void> deleteShow(@PathVariable("id") UUID id) {
        if (!showRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        showRepository.deleteById(id);
        feedService.evictFeedCache();
        return ResponseEntity.noContent().build();
    }

    // --- Podcast Episode CRUD Endpoints ---

    @PostMapping("/api/v1/podcasts/episodes")
    public ResponseEntity<PodcastEpisode> createEpisode(@RequestBody PodcastEpisode episode) {
        if (episode.getId() == null) {
            episode.setId(UUID.randomUUID());
        }
        PodcastEpisode savedEpisode = episodeRepository.save(episode);
        feedService.evictFeedCache();
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEpisode);
    }

    @GetMapping("/api/v1/podcasts/episodes/{id}")
    public ResponseEntity<PodcastEpisode> getEpisodeById(@PathVariable("id") UUID id) {
        return episodeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/podcasts/shows/{showId}/episodes")
    public ResponseEntity<List<PodcastEpisode>> getEpisodesByShowId(@PathVariable("showId") UUID showId) {
        List<PodcastEpisode> episodes = episodeRepository.findByShowIdOrderByPublishedAtDesc(showId);
        return ResponseEntity.ok(episodes);
    }

    @PutMapping("/api/v1/podcasts/episodes/{id}")
    public ResponseEntity<PodcastEpisode> updateEpisode(@PathVariable("id") UUID id, @RequestBody PodcastEpisode episodeDetails) {
        Optional<PodcastEpisode> episodeOpt = episodeRepository.findById(id);
        if (episodeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PodcastEpisode existingEpisode = episodeOpt.get();
        if (episodeDetails.getTitle() != null) {
            existingEpisode.setTitle(episodeDetails.getTitle());
        }
        if (episodeDetails.getDescription() != null) {
            existingEpisode.setDescription(episodeDetails.getDescription());
        }
        if (episodeDetails.getMediaUrl() != null) {
            existingEpisode.setMediaUrl(episodeDetails.getMediaUrl());
        }
        if (episodeDetails.getMediaType() != null) {
            existingEpisode.setMediaType(episodeDetails.getMediaType());
        }
        if (episodeDetails.getEpisodeNumber() != null) {
            existingEpisode.setEpisodeNumber(episodeDetails.getEpisodeNumber());
        }
        if (episodeDetails.getSeasonNumber() != null) {
            existingEpisode.setSeasonNumber(episodeDetails.getSeasonNumber());
        }
        if (episodeDetails.getDurationSeconds() > 0) {
            existingEpisode.setDurationSeconds(episodeDetails.getDurationSeconds());
        }
        if (episodeDetails.getFileSizeBytes() > 0) {
            existingEpisode.setFileSizeBytes(episodeDetails.getFileSizeBytes());
        }

        PodcastEpisode updatedEpisode = episodeRepository.save(existingEpisode);
        feedService.evictFeedCache();
        return ResponseEntity.ok(updatedEpisode);
    }

    @DeleteMapping("/api/v1/podcasts/episodes/{id}")
    public ResponseEntity<Void> deleteEpisode(@PathVariable("id") UUID id) {
        if (!episodeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        episodeRepository.deleteById(id);
        feedService.evictFeedCache();
        return ResponseEntity.noContent().build();
    }

    private String computeEtag(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return "\"" + HexFormat.of().formatHex(hash) + "\"";
        } catch (Exception e) {
            return "\"" + Integer.toHexString(content.hashCode()) + "\"";
        }
    }
}
