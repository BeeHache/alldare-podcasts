package online.alldare.podcasts.service;

import online.alldare.common.constants.MediaTypes;
import online.alldare.podcasts.domain.PodcastEpisode;
import online.alldare.podcasts.domain.PodcastShow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RssFeedGeneratorServiceTest {

  private RssFeedGeneratorService rssFeedGeneratorService;

  @BeforeEach
  void setUp() {
    rssFeedGeneratorService = new RssFeedGeneratorService();
    ReflectionTestUtils.setField(rssFeedGeneratorService, "feedBaseUrl", "https://cdn.alldare.online");
  }

  @Test
  void generateRss2Feed_WithValidShowAndEpisodes_RendersValidXml() {
    PodcastShow show = new PodcastShow();
    show.setId(UUID.randomUUID());
    show.setTitle("Tech & Code & Fun");
    show.setSlug("tech-code-fun");
    show.setDescription("A podcast about building high-scale software.");
    show.setAuthorName("Jane Creator");
    show.setEmail("jane@alldare.online");
    show.setCategory("Technology");
    show.setCoverImageUrl("https://cdn.alldare.online/cover.jpg");
    show.setExplicit(false);
    show.setCreatedAt(Instant.now());

    PodcastEpisode episode = new PodcastEpisode();
    episode.setId(UUID.randomUUID());
    episode.setShow(show);
    episode.setTitle("Episode 1 <Introduction>");
    episode.setDescription("Welcome to the first episode!");
    episode.setEpisodeNumber(1);
    episode.setSeasonNumber(1);
    episode.setMediaUrl("https://cdn.alldare.online/audio.mp3");
    episode.setMediaType(MediaTypes.AUDIO_MPEG);
    episode.setDurationSeconds(1200);
    episode.setFileSizeBytes(15000000L);
    episode.setPublishedAt(Instant.now());

    String xml = rssFeedGeneratorService.generateRss2Feed(show, List.of(episode));

    assertThat(xml).isNotNull();
    assertThat(xml).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    assertThat(xml).contains("<title>Tech &amp; Code &amp; Fun</title>");
    assertThat(xml).contains("<atom:link href=\"https://cdn.alldare.online/podcasts/shows/tech-code-fun/rss.xml\"");
    assertThat(xml).contains("<itunes:author>Jane Creator</itunes:author>");
    assertThat(xml).contains("<enclosure url=\"https://cdn.alldare.online/podcasts/stream/" + episode.getId() + ".mp3\"");
    assertThat(xml).contains("<itunes:duration>00:20:00</itunes:duration>");
    assertThat(xml).contains("Episode 1 &lt;Introduction&gt;");
  }

  @Test
  void generateRss2Feed_WithEmptyEpisodes_RendersChannelHeaderOnly() {
    PodcastShow show = new PodcastShow();
    show.setId(UUID.randomUUID());
    show.setTitle("Empty Show");
    show.setSlug("empty-show");
    show.setDescription("No episodes yet.");
    show.setAuthorName("Solo Host");
    show.setEmail("host@alldare.online");

    String xml = rssFeedGeneratorService.generateRss2Feed(show, List.of());

    assertThat(xml).contains("<title>Empty Show</title>");
    assertThat(xml).doesNotContain("<item>");
  }
}
