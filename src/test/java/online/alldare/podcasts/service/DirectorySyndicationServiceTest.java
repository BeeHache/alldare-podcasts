package online.alldare.podcasts.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import online.alldare.podcasts.domain.PodcastShow;
import online.alldare.podcasts.domain.PodcastSyndication;
import online.alldare.podcasts.domain.enums.DirectoryName;
import online.alldare.podcasts.domain.enums.SyndicationStatus;
import online.alldare.podcasts.repository.PodcastShowRepository;
import online.alldare.podcasts.repository.PodcastSyndicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DirectorySyndicationServiceTest {

  private PodcastShowRepository showRepository;
  private PodcastSyndicationRepository syndicationRepository;
  private DirectorySyndicationService syndicationService;

  @BeforeEach
  void setUp() {
    showRepository = mock(PodcastShowRepository.class);
    syndicationRepository = mock(PodcastSyndicationRepository.class);
    syndicationService = new DirectorySyndicationService(showRepository, syndicationRepository);
    ReflectionTestUtils.setField(syndicationService, "feedBaseUrl", "https://podcasts.alldare.online");
  }

  @Test
  void testSyndicateShowToDirectory_PodcastIndex() {
    UUID showId = UUID.randomUUID();
    PodcastShow show = new PodcastShow();
    show.setId(showId);
    show.setSlug("test-podcast");
    show.setTitle("Test Podcast");
    show.setCreatedAt(Instant.now());

    when(syndicationRepository.findByShowIdAndDirectoryName(showId, DirectoryName.PODCAST_INDEX))
        .thenReturn(Optional.empty());
    when(syndicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    PodcastSyndication result = syndicationService.syndicateShowToDirectory(show, DirectoryName.PODCAST_INDEX);

    assertNotNull(result);
    assertEquals(SyndicationStatus.INDEXED, result.getStatus());
    assertTrue(result.getDirectoryShowUrl().contains("test-podcast"));
  }

  @Test
  void testSyndicateShowToDirectory_AppleAndSpotify() {
    UUID showId = UUID.randomUUID();
    PodcastShow show = new PodcastShow();
    show.setId(showId);
    show.setSlug("test-podcast");
    show.setTitle("Test Podcast");
    show.setCreatedAt(Instant.now());

    when(syndicationRepository.findByShowIdAndDirectoryName(any(), any())).thenReturn(Optional.empty());
    when(syndicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    PodcastSyndication appleResult = syndicationService.syndicateShowToDirectory(show, DirectoryName.APPLE);
    assertEquals(SyndicationStatus.INDEXED, appleResult.getStatus());
    assertTrue(appleResult.isManagedByPlatform());

    PodcastSyndication spotifyResult = syndicationService.syndicateShowToDirectory(show, DirectoryName.SPOTIFY);
    assertEquals(SyndicationStatus.INDEXED, spotifyResult.getStatus());
    assertTrue(spotifyResult.isManagedByPlatform());
  }

  @Test
  void testGenerateClaimTokenAndTransferOwnership() {
    UUID showId = UUID.randomUUID();
    PodcastSyndication syn = PodcastSyndication.builder()
        .id(UUID.randomUUID())
        .showId(showId)
        .directoryName(DirectoryName.APPLE)
        .status(SyndicationStatus.INDEXED)
        .isManagedByPlatform(true)
        .isClaimedByCreator(false)
        .build();

    when(syndicationRepository.findByShowIdAndDirectoryName(showId, DirectoryName.APPLE)).thenReturn(Optional.of(syn));
    when(syndicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    PodcastSyndication tokenResult = syndicationService.generateClaimToken(showId, DirectoryName.APPLE);
    assertNotNull(tokenResult.getClaimToken());

    PodcastSyndication transferResult = syndicationService.transferOwnershipToCreator(showId, DirectoryName.APPLE, tokenResult.getClaimToken());
    assertTrue(transferResult.isClaimedByCreator());
    assertFalse(transferResult.isManagedByPlatform());
    assertNotNull(transferResult.getClaimedAt());
  }

  @Test
  void testProcessBulkSyndicationForDirectory() {
    UUID showId = UUID.randomUUID();
    PodcastShow show = new PodcastShow();
    show.setId(showId);
    show.setSlug("bulk-podcast");
    show.setTitle("Bulk Podcast");
    show.setCreatedAt(Instant.now());

    when(showRepository.findAll()).thenReturn(List.of(show));
    when(syndicationRepository.findByShowIdAndDirectoryName(any(), any())).thenReturn(Optional.empty());
    when(syndicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    int count = syndicationService.processBulkSyndicationForDirectory(DirectoryName.LISTEN_NOTES, null, 10);
    assertEquals(1, count);
  }

  @Test
  void testGetSyndicationsForShow() {
    UUID showId = UUID.randomUUID();
    PodcastSyndication syn = PodcastSyndication.builder()
        .id(UUID.randomUUID())
        .showId(showId)
        .directoryName(DirectoryName.PODCAST_INDEX)
        .status(SyndicationStatus.INDEXED)
        .build();

    when(syndicationRepository.findByShowId(showId)).thenReturn(List.of(syn));

    var result = syndicationService.getSyndicationsForShow(showId);
    assertEquals(1, result.size());
  }
}
