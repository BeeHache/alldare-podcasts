package online.alldare.podcasts.service;

import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;
import net.devh.boot.grpc.server.service.GrpcService;
import online.alldare.common.grpc.PodcastFeedRequest;
import online.alldare.common.grpc.PodcastFeedResponse;
import online.alldare.common.grpc.PodcastServiceGrpc;
import online.alldare.podcasts.domain.PodcastEpisode;
import online.alldare.podcasts.domain.PodcastShow;
import online.alldare.podcasts.repository.PodcastEpisodeRepository;
import online.alldare.podcasts.repository.PodcastShowRepository;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class PodcastGrpcService extends PodcastServiceGrpc.PodcastServiceImplBase {

    @Autowired
    private PodcastShowRepository showRepository;

    @Autowired
    private PodcastEpisodeRepository episodeRepository;

    @Autowired
    private RssFeedGeneratorService feedGeneratorService;

    @Override
    public void getPodcastFeed(PodcastFeedRequest request, StreamObserver<PodcastFeedResponse> responseObserver) {
        String identifier = request.getUsername();
        String format = request.getFormat();

        Optional<PodcastShow> showOpt = showRepository.findBySlug(identifier);
        if (showOpt.isEmpty()) {
            responseObserver.onNext(PodcastFeedResponse.newBuilder()
                    .setXmlContent("")
                    .setEpisodeCount(0)
                    .build());
            responseObserver.onCompleted();
            return;
        }

        PodcastShow show = showOpt.get();
        List<PodcastEpisode> episodes = episodeRepository.findByShowOrderByPublishedAtDesc(show);

        String xmlContent;
        if ("atom".equalsIgnoreCase(format)) {
            xmlContent = feedGeneratorService.generateAtomFeed(show, episodes);
        } else {
            xmlContent = feedGeneratorService.generateRss2Feed(show, episodes);
        }

        PodcastFeedResponse response = PodcastFeedResponse.newBuilder()
                .setXmlContent(xmlContent)
                .setEpisodeCount(episodes.size())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
