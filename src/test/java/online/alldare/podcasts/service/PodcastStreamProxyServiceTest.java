package online.alldare.podcasts.service;

import online.alldare.common.messaging.RedisMessagePublisher;
import online.alldare.common.messaging.StreamKeys;
import online.alldare.podcasts.domain.PodcastEpisode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PodcastStreamProxyServiceTest {

  @Mock
  private RedisMessagePublisher redisPublisher;

  @InjectMocks
  private PodcastStreamProxyService streamProxyService;

  @BeforeEach
  void setUp() {
    org.springframework.test.util.ReflectionTestUtils.setField(streamProxyService, "cdnBaseUrl", "https://cdn.alldare.online");
  }

  @Test
  void streamEpisodeWithDai_WhenSubscriber_DoesNotPublishAdImpression() {
    PodcastEpisode episode = new PodcastEpisode();
    episode.setId(UUID.randomUUID());
    episode.setMediaUrl("invalid-url-for-test");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    streamProxyService.streamEpisodeWithDai(episode, baos, true);

    assertThat(baos.toByteArray()).isNotNull();
  }

  @Test
  void streamEpisodeWithDai_WhenNonSubscriber_PublishesAdImpression() {
    PodcastEpisode episode = new PodcastEpisode();
    UUID episodeId = UUID.randomUUID();
    episode.setId(episodeId);
    episode.setMediaUrl("invalid-url-for-test");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    streamProxyService.streamEpisodeWithDai(episode, baos, false);

    verify(redisPublisher).publish(eq(StreamKeys.ADS_IMPRESSIONS), eq(episodeId.toString()));
  }
}
