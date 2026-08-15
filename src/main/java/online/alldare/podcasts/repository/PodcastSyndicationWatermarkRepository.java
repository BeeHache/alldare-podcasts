package online.alldare.podcasts.repository;

import online.alldare.podcasts.domain.PodcastSyndicationWatermark;
import online.alldare.podcasts.domain.enums.DirectoryName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PodcastSyndicationWatermarkRepository extends JpaRepository<PodcastSyndicationWatermark, DirectoryName> {
}
