package online.alldare.podcasts.messaging;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.alldare.podcasts.domain.PodcastSyndicationWatermark;
import online.alldare.podcasts.domain.enums.DirectoryName;
import online.alldare.podcasts.repository.PodcastSyndicationWatermarkRepository;
import online.alldare.podcasts.service.DirectorySyndicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledBulkSyndicationWorker {

  private final DirectorySyndicationService directorySyndicationService;
  private final PodcastSyndicationWatermarkRepository watermarkRepository;

  @Value("${alldare.podcasts.syndication.batch-size:100}")
  private int batchSize;

  @Value("${alldare.podcasts.syndication.enabled:true}")
  private boolean syndicationEnabled;

  @Scheduled(cron = "${alldare.podcasts.syndication.schedules.podcast-index:0 0 2 * * *}")
  public void runPodcastIndexBulkSyndication() {
    executeDirectoryBulkSyndication(DirectoryName.PODCAST_INDEX);
  }

  @Scheduled(cron = "${alldare.podcasts.syndication.schedules.listen-notes:0 30 2 * * *}")
  public void runListenNotesBulkSyndication() {
    executeDirectoryBulkSyndication(DirectoryName.LISTEN_NOTES);
  }

  @Scheduled(cron = "${alldare.podcasts.syndication.schedules.spotify:0 0 3 * * *}")
  public void runSpotifyBulkSyndication() {
    executeDirectoryBulkSyndication(DirectoryName.SPOTIFY);
  }

  @Scheduled(cron = "${alldare.podcasts.syndication.schedules.apple:0 30 3 * * *}")
  public void runAppleBulkSyndication() {
    executeDirectoryBulkSyndication(DirectoryName.APPLE);
  }

  public void executeDirectoryBulkSyndication(DirectoryName directoryName) {
    if (!syndicationEnabled) {
      log.info("Syndication disabled by configuration. Skipping {}", directoryName);
      return;
    }

    log.info("Syndication Worker: Running bulk syndication for {}", directoryName);
    PodcastSyndicationWatermark watermark = watermarkRepository.findById(directoryName)
        .orElseGet(() -> PodcastSyndicationWatermark.builder()
            .directoryName(directoryName)
            .processedCount(0)
            .status("IDLE")
            .updatedAt(Instant.now())
            .build());

    Instant startTime = Instant.now();
    watermark.setLastRunAt(startTime);
    watermark.setStatus("RUNNING");
    watermarkRepository.save(watermark);

    try {
      int count = directorySyndicationService.processBulkSyndicationForDirectory(
          directoryName,
          watermark.getLastSuccessfulRunAt(),
          batchSize
      );

      watermark.setStatus("SUCCESS");
      watermark.setLastSuccessfulRunAt(startTime);
      watermark.setProcessedCount(watermark.getProcessedCount() + count);
      watermark.setErrorLog(null);
      watermark.setUpdatedAt(Instant.now());
      watermarkRepository.save(watermark);
      log.info("Syndication Worker: Completed bulk syndication for {}. Processed {} shows.", directoryName, count);

    } catch (Exception ex) {
      log.error("Syndication Worker: Failed bulk syndication for {}", directoryName, ex);
      watermark.setStatus("FAILED");
      watermark.setErrorLog(ex.getMessage());
      watermark.setUpdatedAt(Instant.now());
      watermarkRepository.save(watermark);
    }
  }
}
