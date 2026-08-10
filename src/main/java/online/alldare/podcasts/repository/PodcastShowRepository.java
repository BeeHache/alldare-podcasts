package online.alldare.podcasts.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import online.alldare.podcasts.domain.PodcastShow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PodcastShowRepository extends JpaRepository<PodcastShow, UUID> {
    Optional<PodcastShow> findBySlug(String slug);
    Optional<PodcastShow> findByCreatorId(UUID creatorId);
    List<PodcastShow> findAllByCreatorId(UUID creatorId);
}
