package online.alldare.podcasts.domain;

import online.alldare.common.constants.MediaTypes;
import online.alldare.podcasts.service.RssFeedGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PodcastDomainTest {

  @Test
  void testPodcastShowDomainMethods() {
    UUID id = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    Instant now = Instant.now();

    PodcastShow show = new PodcastShow();
    show.setId(id);
    show.setCreatorId(creatorId);
    show.setTitle("Title");
    show.setSlug("slug");
    show.setDescription("Desc");
    show.setAuthorName("Author");
    show.setEmail("author@alldare.online");
    show.setCategory("Tech");
    show.setCoverImageUrl("https://cdn.alldare.online/cover.jpg");
    show.setExplicit(true);
    show.setCreatedAt(now);
    show.setUpdatedAt(now);

    assertThat(show.getId()).isEqualTo(id);
    assertThat(show.getCreatorId()).isEqualTo(creatorId);
    assertThat(show.getTitle()).isEqualTo("Title");
    assertThat(show.getSlug()).isEqualTo("slug");
    assertThat(show.getDescription()).isEqualTo("Desc");
    assertThat(show.getAuthorName()).isEqualTo("Author");
    assertThat(show.getEmail()).isEqualTo("author@alldare.online");
    assertThat(show.getCategory()).isEqualTo("Tech");
    assertThat(show.getCoverImageUrl()).isEqualTo("https://cdn.alldare.online/cover.jpg");
    assertThat(show.isExplicit()).isTrue();
    assertThat(show.getCreatedAt()).isEqualTo(now);
    assertThat(show.getUpdatedAt()).isEqualTo(now);

    PodcastShow parameterized = new PodcastShow(id, creatorId, "slug", "Title", "Desc", "Tech", "Author", "author@alldare.online", "https://cdn.alldare.online/cover.jpg", true);
    assertThat(parameterized.getSlug()).isEqualTo("slug");
  }

  @Test
  void testPodcastEpisodeDomainMethods() {
    UUID id = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    PodcastShow show = new PodcastShow();
    Instant now = Instant.now();

    PodcastEpisode episode = new PodcastEpisode();
    episode.setId(id);
    episode.setShow(show);
    episode.setPostId(postId);
    episode.setTitle("Ep Title");
    episode.setDescription("Ep Desc");
    episode.setEpisodeNumber(2);
    episode.setSeasonNumber(1);
    episode.setMediaUrl("https://cdn.alldare.online/audio.mp3");
    episode.setMediaType(MediaTypes.AUDIO_MPEG);
    episode.setDurationSeconds(300);
    episode.setFileSizeBytes(500000L);
    episode.setPublishedAt(now);
    episode.setCreatedAt(now);

    assertThat(episode.getId()).isEqualTo(id);
    assertThat(episode.getShow()).isEqualTo(show);
    assertThat(episode.getPostId()).isEqualTo(postId);
    assertThat(episode.getTitle()).isEqualTo("Ep Title");
    assertThat(episode.getDescription()).isEqualTo("Ep Desc");
    assertThat(episode.getEpisodeNumber()).isEqualTo(2);
    assertThat(episode.getSeasonNumber()).isEqualTo(1);
    assertThat(episode.getMediaUrl()).isEqualTo("https://cdn.alldare.online/audio.mp3");
    assertThat(episode.getMediaType()).isEqualTo(MediaTypes.AUDIO_MPEG);
    assertThat(episode.getDurationSeconds()).isEqualTo(300);
    assertThat(episode.getFileSizeBytes()).isEqualTo(500000L);
    assertThat(episode.getPublishedAt()).isEqualTo(now);
    assertThat(episode.getCreatedAt()).isEqualTo(now);
  }

  @Test
  void testAtomFeedGenerator() {
    RssFeedGeneratorService feedGeneratorService = new RssFeedGeneratorService();
    ReflectionTestUtils.setField(feedGeneratorService, "feedBaseUrl", "https://cdn.alldare.online");

    PodcastShow show = new PodcastShow();
    show.setId(UUID.randomUUID());
    show.setTitle("Atom Show");
    show.setSlug("atom-show");
    show.setDescription("Atom desc.");
    show.setAuthorName("Atom Author");
    show.setEmail("atom@alldare.online");
    show.setCreatedAt(Instant.now());

    PodcastEpisode episode = new PodcastEpisode();
    episode.setId(UUID.randomUUID());
    episode.setShow(show);
    episode.setTitle("Atom Ep 1");
    episode.setDescription("Atom ep desc.");
    episode.setMediaUrl("https://cdn.alldare.online/audio.mp3");
    episode.setMediaType(MediaTypes.AUDIO_MPEG);
    episode.setDurationSeconds(600);
    episode.setFileSizeBytes(1000000L);
    episode.setPublishedAt(Instant.now());

    String atomXml = feedGeneratorService.generateAtomFeed(show, List.of(episode));
    assertThat(atomXml).isNotNull();
    assertThat(atomXml).contains("<feed xmlns=\"http://www.w3.org/2005/Atom\">");
    assertThat(atomXml).contains("<title>Atom Show</title>");
    assertThat(atomXml).contains("Atom Ep 1");
  }
}
