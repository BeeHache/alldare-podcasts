package online.alldare.podcasts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import online.alldare.podcasts.domain.enums.DirectoryName;
import online.alldare.podcasts.domain.enums.SyndicationStatus;

@Entity
@Table(name = "podcast_syndications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PodcastSyndication {

  @Id
  private UUID id;

  @Column(name = "show_id", nullable = false)
  private UUID showId;

  @Enumerated(EnumType.STRING)
  @Column(name = "directory_name", nullable = false)
  private DirectoryName directoryName;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SyndicationStatus status;

  @Column(name = "directory_show_url", length = 500)
  private String directoryShowUrl;

  @Column(name = "claim_url", length = 500)
  private String claimUrl;

  @Builder.Default
  @Column(name = "is_managed_by_platform", nullable = false)
  private boolean isManagedByPlatform = true;

  @Builder.Default
  @Column(name = "is_claimed_by_creator", nullable = false)
  private boolean isClaimedByCreator = false;

  @Column(name = "claim_token", length = 100)
  private String claimToken;

  @Column(name = "claimed_at")
  private Instant claimedAt;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
