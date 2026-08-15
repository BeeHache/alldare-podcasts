package online.alldare.podcasts.messaging;

import java.util.Optional;
import online.alldare.podcasts.domain.enums.DirectoryName;
import online.alldare.podcasts.repository.PodcastSyndicationWatermarkRepository;
import online.alldare.podcasts.service.DirectorySyndicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScheduledBulkSyndicationWorkerTest {

  private DirectorySyndicationService syndicationService;
  private PodcastSyndicationWatermarkRepository watermarkRepository;
  private ScheduledBulkSyndicationWorker worker;

  @BeforeEach
  void setUp() {
    syndicationService = mock(DirectorySyndicationService.class);
    watermarkRepository = mock(PodcastSyndicationWatermarkRepository.class);
    worker = new ScheduledBulkSyndicationWorker(syndicationService, watermarkRepository);

    ReflectionTestUtils.setField(worker, "batchSize", 100);
    ReflectionTestUtils.setField(worker, "syndicationEnabled", true);
  }

  @Test
  void testExecuteDirectoryBulkSyndication_Success() {
    when(watermarkRepository.findById(any())).thenReturn(Optional.empty());
    when(syndicationService.processBulkSyndicationForDirectory(any(), any(), anyInt())).thenReturn(5);

    worker.executeDirectoryBulkSyndication(DirectoryName.PODCAST_INDEX);

    verify(syndicationService).processBulkSyndicationForDirectory(eq(DirectoryName.PODCAST_INDEX), any(), eq(100));
    verify(watermarkRepository, times(2)).save(any());
  }

  @Test
  void testExecuteDirectoryBulkSyndication_Disabled() {
    ReflectionTestUtils.setField(worker, "syndicationEnabled", false);

    worker.executeDirectoryBulkSyndication(DirectoryName.PODCAST_INDEX);

    verify(syndicationService, never()).processBulkSyndicationForDirectory(any(), any(), anyInt());
  }

  @Test
  void testScheduledMethods() {
    when(watermarkRepository.findById(any())).thenReturn(Optional.empty());
    when(syndicationService.processBulkSyndicationForDirectory(any(), any(), anyInt())).thenReturn(1);

    assertDoesNotThrow(() -> worker.runPodcastIndexBulkSyndication());
    assertDoesNotThrow(() -> worker.runListenNotesBulkSyndication());
    assertDoesNotThrow(() -> worker.runSpotifyBulkSyndication());
    assertDoesNotThrow(() -> worker.runAppleBulkSyndication());
  }

  @Test
  void testExecuteDirectoryBulkSyndication_Failure() {
    when(watermarkRepository.findById(any())).thenReturn(Optional.empty());
    when(syndicationService.processBulkSyndicationForDirectory(any(), any(), anyInt()))
        .thenThrow(new RuntimeException("API failure"));

    worker.executeDirectoryBulkSyndication(DirectoryName.LISTEN_NOTES);

    verify(watermarkRepository, times(2)).save(any());
  }
}
