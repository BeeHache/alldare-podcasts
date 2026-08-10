package online.alldare.podcasts.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import online.alldare.podcasts.domain.PodcastEpisode;
import online.alldare.podcasts.domain.PodcastShow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PodcastEpisodeRepository extends JpaRepository<PodcastEpisode, UUID> {
    List<PodcastEpisode> findByShowOrderByPublishedAtDesc(PodcastShow show);
    List<PodcastEpisode> findByShowIdOrderByPublishedAtDesc(UUID showId);
    Optional<PodcastEpisode> findByPostId(UUID postId);
}
