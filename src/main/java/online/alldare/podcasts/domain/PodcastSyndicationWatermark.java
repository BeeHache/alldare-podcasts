package online.alldare.podcasts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import online.alldare.podcasts.domain.enums.DirectoryName;

@Entity
@Table(name = "podcast_syndication_watermarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PodcastSyndicationWatermark {

  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "directory_name", nullable = false)
  private DirectoryName directoryName;

  @Column(name = "last_run_at")
  private Instant lastRunAt;

  @Column(name = "last_successful_run_at")
  private Instant lastSuccessfulRunAt;

  @Column(name = "processed_count", nullable = false)
  private int processedCount;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "error_log", columnDefinition = "TEXT")
  private String errorLog;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
