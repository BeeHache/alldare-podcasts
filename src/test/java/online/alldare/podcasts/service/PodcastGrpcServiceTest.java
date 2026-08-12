package online.alldare.podcasts.service;

import io.grpc.stub.StreamObserver;
import online.alldare.common.grpc.PodcastFeedRequest;
import online.alldare.common.grpc.PodcastFeedResponse;
import online.alldare.podcasts.domain.PodcastShow;
import online.alldare.podcasts.repository.PodcastEpisodeRepository;
import online.alldare.podcasts.repository.PodcastShowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PodcastGrpcServiceTest {

  @Mock
  private PodcastShowRepository showRepository;

  @Mock
  private PodcastEpisodeRepository episodeRepository;

  @Mock
  private RssFeedGeneratorService feedGeneratorService;

  @Mock
  private StreamObserver<PodcastFeedResponse> responseObserver;

  @InjectMocks
  private PodcastGrpcService podcastGrpcService;

  @Test
  void getPodcastFeed_WhenShowNotFound_ReturnsEmptyResponse() {
    given(showRepository.findBySlug("unknown")).willReturn(Optional.empty());

    PodcastFeedRequest request = PodcastFeedRequest.newBuilder().setUsername("unknown").setFormat("rss").build();
    podcastGrpcService.getPodcastFeed(request, responseObserver);

    ArgumentCaptor<PodcastFeedResponse> captor = ArgumentCaptor.forClass(PodcastFeedResponse.class);
    verify(responseObserver).onNext(captor.capture());
    verify(responseObserver).onCompleted();

    PodcastFeedResponse response = captor.getValue();
    assertThat(response.getXmlContent()).isEmpty();
    assertThat(response.getEpisodeCount()).isEqualTo(0);
  }

  @Test
  void getPodcastFeed_WhenShowFound_ReturnsXmlResponse() {
    PodcastShow show = new PodcastShow();
    show.setId(UUID.randomUUID());
    show.setSlug("test-show");

    given(showRepository.findBySlug("test-show")).willReturn(Optional.of(show));
    given(episodeRepository.findByShowOrderByPublishedAtDesc(show)).willReturn(List.of());
    given(feedGeneratorService.generateRss2Feed(eq(show), any())).willReturn("<rss></rss>");

    PodcastFeedRequest request = PodcastFeedRequest.newBuilder().setUsername("test-show").setFormat("rss").build();
    podcastGrpcService.getPodcastFeed(request, responseObserver);

    ArgumentCaptor<PodcastFeedResponse> captor = ArgumentCaptor.forClass(PodcastFeedResponse.class);
    verify(responseObserver).onNext(captor.capture());
    verify(responseObserver).onCompleted();

    PodcastFeedResponse response = captor.getValue();
    assertThat(response.getXmlContent()).isEqualTo("<rss></rss>");
    assertThat(response.getEpisodeCount()).isEqualTo(0);
  }
}
