package online.alldare.podcasts.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import online.alldare.podcasts.domain.PodcastSyndication;
import online.alldare.podcasts.domain.enums.DirectoryName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PodcastSyndicationRepository extends JpaRepository<PodcastSyndication, UUID> {

  List<PodcastSyndication> findByShowId(UUID showId);

  Optional<PodcastSyndication> findByShowIdAndDirectoryName(UUID showId, DirectoryName directoryName);
}
