package online.alldare.podcasts.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import online.alldare.podcasts.constant.PodcastConstants;
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

    private static final CacheControl FEED_CACHE_CONTROL = CacheControl.maxAge(Duration.ofSeconds(PodcastConstants.CACHE_MAX_AGE_SECONDS))
            .cachePublic()
            .sMaxAge(Duration.ofSeconds(PodcastConstants.CACHE_S_MAX_AGE_SECONDS));

    @Autowired
    private PodcastShowRepository showRepository;

    @Autowired
    private PodcastEpisodeRepository episodeRepository;

    @Autowired
    private PodcastFeedService feedService;

    @Autowired
    private PodcastStreamProxyService streamProxyService;

    @Autowired
    private online.alldare.podcasts.service.DirectorySyndicationService directorySyndicationService;

    @Autowired
    private online.alldare.podcasts.repository.PodcastSyndicationWatermarkRepository watermarkRepository;

    @Autowired
    private online.alldare.podcasts.messaging.ScheduledBulkSyndicationWorker bulkSyndicationWorker;

    // --- Syndication Endpoints ---

    @GetMapping("/api/v1/podcasts/shows/{showId}/syndication")
    public ResponseEntity<List<online.alldare.podcasts.domain.PodcastSyndication>> getSyndicationStatus(@PathVariable("showId") UUID showId) {
        return ResponseEntity.ok(directorySyndicationService.getSyndicationsForShow(showId));
    }

    @GetMapping("/api/v1/podcasts/syndication/watermarks")
    public ResponseEntity<List<online.alldare.podcasts.domain.PodcastSyndicationWatermark>> getSyndicationWatermarks() {
        return ResponseEntity.ok(watermarkRepository.findAll());
    }

    @PostMapping("/api/v1/podcasts/syndication/trigger/{directory}")
    public ResponseEntity<Void> triggerBulkSyndication(@PathVariable("directory") String directory) {
        try {
            online.alldare.podcasts.domain.enums.DirectoryName dirName = online.alldare.podcasts.domain.enums.DirectoryName.valueOf(directory.toUpperCase());
            bulkSyndicationWorker.executeDirectoryBulkSyndication(dirName);
            return ResponseEntity.accepted().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/api/v1/podcasts/shows/{showId}/syndication/{directory}/claim-token")
    public ResponseEntity<online.alldare.podcasts.domain.PodcastSyndication> generateClaimToken(
            @PathVariable("showId") UUID showId,
            @PathVariable("directory") String directory) {
        try {
            online.alldare.podcasts.domain.enums.DirectoryName dirName = online.alldare.podcasts.domain.enums.DirectoryName.valueOf(directory.toUpperCase());
            return ResponseEntity.ok(directorySyndicationService.generateClaimToken(showId, dirName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/api/v1/podcasts/shows/{showId}/syndication/{directory}/transfer-ownership")
    public ResponseEntity<online.alldare.podcasts.domain.PodcastSyndication> transferOwnership(
            @PathVariable("showId") UUID showId,
            @PathVariable("directory") String directory,
            @RequestParam(name = "claimToken", required = false) String claimToken) {
        try {
            online.alldare.podcasts.domain.enums.DirectoryName dirName = online.alldare.podcasts.domain.enums.DirectoryName.valueOf(directory.toUpperCase());
            return ResponseEntity.ok(directorySyndicationService.transferOwnershipToCreator(showId, dirName, claimToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

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
                .header(HttpHeaders.CONTENT_TYPE, PodcastConstants.CONTENT_TYPE_RSS_XML)
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
                .header(HttpHeaders.CONTENT_TYPE, PodcastConstants.CONTENT_TYPE_ATOM_XML)
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
            response.setContentType(PodcastConstants.CONTENT_TYPE_AUDIO_MPEG);
            response.setHeader("Transfer-Encoding", PodcastConstants.TRANSFER_ENCODING_CHUNKED);

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
            response.setContentType(PodcastConstants.CONTENT_TYPE_VIDEO_MP4);
            response.setHeader("Transfer-Encoding", PodcastConstants.TRANSFER_ENCODING_CHUNKED);

            OutputStream outputStream = response.getOutputStream();
            streamProxyService.streamEpisodeWithDai(episode, outputStream, isSubscriber);
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    public static final String DEFAULT_COVER_URL = "https://alldare.local/assets/images/default-podcast-cover.jpg";

    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("/assets/")) {
            return true;
        }
        try {
            java.net.URI uri = java.net.URI.create(trimmed);
            return uri.isAbsolute() && (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"));
        } catch (Exception e) {
            return false;
        }
    }

    // --- Podcast Show CRUD Endpoints ---

    @PostMapping("/api/v1/podcasts/shows")
    public ResponseEntity<?> createShow(@RequestBody PodcastShow show) {
        if (show.getSlug() == null || show.getSlug().trim().isEmpty()) {
            if (show.getTitle() != null && !show.getTitle().trim().isEmpty()) {
                show.setSlug(show.getTitle().toLowerCase().trim().replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", ""));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Show title or slug is required.");
            }
        } else {
            show.setSlug(show.getSlug().toLowerCase().trim().replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", ""));
        }

        // Slug uniqueness check
        if (showRepository.findBySlug(show.getSlug()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Slug '" + show.getSlug() + "' is already in use by another podcast.");
        }

        if (show.getCoverImageUrl() == null || show.getCoverImageUrl().trim().isEmpty()) {
            show.setCoverImageUrl(DEFAULT_COVER_URL);
        } else if (!isValidUrl(show.getCoverImageUrl())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid cover image URL format.");
        }

        if (show.getId() == null) {
            show.setId(UUID.randomUUID());
        }
        if (show.getCreatedAt() == null) {
            show.setCreatedAt(Instant.now());
        }
        show.setUpdatedAt(Instant.now());

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
    public ResponseEntity<?> updateShow(@PathVariable("id") UUID id, @RequestBody PodcastShow showDetails) {
        Optional<PodcastShow> showOpt = showRepository.findById(id);
        if (showOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PodcastShow existingShow = showOpt.get();
        // The URL slug is strictly immutable after creation to preserve RSS feeds & external directory links.

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
            String trimmedCover = showDetails.getCoverImageUrl().trim();
            if (trimmedCover.isEmpty()) {
                existingShow.setCoverImageUrl(DEFAULT_COVER_URL);
            } else if (!isValidUrl(trimmedCover)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid cover image URL format.");
            } else {
                existingShow.setCoverImageUrl(trimmedCover);
            }
        }
        existingShow.setExplicit(showDetails.isExplicit());
        existingShow.setUpdatedAt(Instant.now());

        PodcastShow updatedShow = showRepository.save(existingShow);
        feedService.evictFeedCache();
        return ResponseEntity.ok(updatedShow);
    }

    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/api/v1/podcasts/shows/{id}")
    public ResponseEntity<Void> deleteShow(@PathVariable("id") UUID id) {
        Optional<PodcastShow> showOpt = showRepository.findById(id);
        if (showOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PodcastShow show = showOpt.get();
        episodeRepository.deleteByShow(show);
        showRepository.delete(show);
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
