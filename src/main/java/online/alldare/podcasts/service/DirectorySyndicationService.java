package online.alldare.podcasts.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.alldare.podcasts.domain.PodcastShow;
import online.alldare.podcasts.domain.PodcastSyndication;
import online.alldare.podcasts.domain.enums.DirectoryName;
import online.alldare.podcasts.domain.enums.SyndicationStatus;
import online.alldare.podcasts.repository.PodcastShowRepository;
import online.alldare.podcasts.repository.PodcastSyndicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectorySyndicationService {

  private final PodcastShowRepository podcastShowRepository;
  private final PodcastSyndicationRepository podcastSyndicationRepository;

  @Value("${alldare.podcasts.feed-base-url:http://localhost:8089}")
  private String feedBaseUrl;

  @Transactional
  public int processBulkSyndicationForDirectory(DirectoryName directoryName, Instant sinceTimestamp, int batchSize) {
    log.info("Syndication: Starting bulk syndication for directory {} since {}", directoryName, sinceTimestamp);

    List<PodcastShow> candidateShows = podcastShowRepository.findAll().stream()
        .filter(show -> sinceTimestamp == null || show.getCreatedAt().isAfter(sinceTimestamp))
        .filter(show -> podcastSyndicationRepository.findByShowIdAndDirectoryName(show.getId(), directoryName)
            .map(s -> s.getStatus() == SyndicationStatus.PENDING || s.getStatus() == SyndicationStatus.FAILED)
            .orElse(true))
        .limit(batchSize)
        .toList();

    int processedCount = 0;
    for (PodcastShow show : candidateShows) {
      try {
        syndicateShowToDirectory(show, directoryName);
        processedCount++;
      } catch (Exception ex) {
        log.error("Syndication: Failed to syndicate show {} to directory {}", show.getId(), directoryName, ex);
      }
    }

    return processedCount;
  }

  @Transactional
  public PodcastSyndication syndicateShowToDirectory(PodcastShow show, DirectoryName directoryName) {
    String rssFeedUrl = String.format("%s/podcasts/shows/%s/rss.xml", feedBaseUrl, show.getSlug());
    log.info("Syndication: Submitting RSS feed URL {} to {}", rssFeedUrl, directoryName);

    PodcastSyndication record = podcastSyndicationRepository
        .findByShowIdAndDirectoryName(show.getId(), directoryName)
        .orElseGet(() -> PodcastSyndication.builder()
            .id(UUID.randomUUID())
            .showId(show.getId())
            .directoryName(directoryName)
            .createdAt(Instant.now())
            .build());

    record.setUpdatedAt(Instant.now());

    switch (directoryName) {
      case PODCAST_INDEX -> {
        record.setStatus(SyndicationStatus.INDEXED);
        record.setDirectoryShowUrl("https://podcastindex.org/podcast/slug/" + show.getSlug());
      }
      case LISTEN_NOTES -> {
        record.setStatus(SyndicationStatus.INDEXED);
        record.setDirectoryShowUrl("https://www.listennotes.com/podcasts/" + show.getSlug());
      }
      case SPOTIFY -> {
        record.setStatus(SyndicationStatus.INDEXED);
        record.setDirectoryShowUrl("https://open.spotify.com/show/" + show.getSlug());
        record.setClaimUrl("https://creators.spotify.com/podcasts/claim?feedUrl=" + rssFeedUrl);
      }
      case APPLE -> {
        record.setStatus(SyndicationStatus.INDEXED);
        record.setDirectoryShowUrl("https://podcasts.apple.com/us/podcast/" + show.getSlug());
        record.setClaimUrl("https://podcastsconnect.apple.com/my-podcasts/new?feedUrl=" + rssFeedUrl);
      }
      case AMAZON -> {
        record.setStatus(SyndicationStatus.SUBMITTED);
        record.setDirectoryShowUrl("https://music.amazon.com/podcasts/" + show.getSlug());
        record.setClaimUrl("https://podcasters.amazon.com/podcasts/submit?feedUrl=" + rssFeedUrl);
      }
      default -> {
        record.setStatus(SyndicationStatus.SUBMITTED);
      }
    }

    return podcastSyndicationRepository.save(record);
  }

  @Transactional
  public PodcastSyndication generateClaimToken(UUID showId, DirectoryName directoryName) {
    PodcastSyndication record = podcastSyndicationRepository
        .findByShowIdAndDirectoryName(showId, directoryName)
        .orElseThrow(() -> new IllegalArgumentException("Syndication record not found for show " + showId));

    record.setClaimToken(UUID.randomUUID().toString());
    record.setUpdatedAt(Instant.now());
    return podcastSyndicationRepository.save(record);
  }

  @Transactional
  public PodcastSyndication transferOwnershipToCreator(UUID showId, DirectoryName directoryName, String claimToken) {
    PodcastSyndication record = podcastSyndicationRepository
        .findByShowIdAndDirectoryName(showId, directoryName)
        .orElseThrow(() -> new IllegalArgumentException("Syndication record not found for show " + showId));

    if (record.getClaimToken() != null && !record.getClaimToken().equalsIgnoreCase(claimToken)) {
      throw new IllegalArgumentException("Invalid claim token provided.");
    }

    record.setClaimedByCreator(true);
    record.setManagedByPlatform(false);
    record.setClaimedAt(Instant.now());
    record.setUpdatedAt(Instant.now());
    return podcastSyndicationRepository.save(record);
  }

  public List<PodcastSyndication> getSyndicationsForShow(UUID showId) {
    return podcastSyndicationRepository.findByShowId(showId);
  }
}
