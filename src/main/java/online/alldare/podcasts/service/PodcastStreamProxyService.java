package online.alldare.podcasts.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import online.alldare.common.messaging.RedisMessagePublisher;
import online.alldare.common.messaging.StreamKeys;
import online.alldare.podcasts.constant.PodcastConstants;
import online.alldare.podcasts.domain.PodcastEpisode;
import online.alldare.podcasts.repository.PodcastEpisodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PodcastStreamProxyService {

    private static final Logger log = LoggerFactory.getLogger(PodcastStreamProxyService.class);

    @Autowired
    private PodcastEpisodeRepository episodeRepository;

    @Autowired(required = false)
    private RedisMessagePublisher redisPublisher;

    @Value("${alldare.podcasts.cdn-base-url:https://cdn.alldare.online}")
    private String cdnBaseUrl;

    public void streamEpisodeWithDai(PodcastEpisode episode, OutputStream outputStream, boolean isSubscriber) {
        try {
            // Step 1: Query alldare-ads for Dynamic Audio Injection spot (if not subscriber)
            if (!isSubscriber) {
                log.info("Piping Dynamic Audio/Video Sponsor Ad pre-roll for episode ID: {}", episode.getId());
                // Stream sponsor ad bytes from CDN
                String adUrl = cdnBaseUrl + PodcastConstants.DEFAULT_PREROLL_AD_PATH;
                pipeUrlToOutputStream(adUrl, outputStream);
                
                // Track impression asynchronously
                if (redisPublisher != null) {
                    redisPublisher.publish(StreamKeys.ADS_IMPRESSIONS, episode.getId().toString());
                }
            }

            // Step 2: Stream content episode bytes from CDN
            log.info("Piping content media bytes for episode ID: {}", episode.getId());
            String mediaUrl = episode.getMediaUrl();
            if (!mediaUrl.startsWith("http")) {
                mediaUrl = cdnBaseUrl + "/" + mediaUrl;
            }
            pipeUrlToOutputStream(mediaUrl, outputStream);

        } catch (Exception e) {
            log.error("Error streaming podcast binary proxy for episode ID: {}", episode.getId(), e);
        }
    }

    private void pipeUrlToOutputStream(String fileUrl, OutputStream outputStream) {
        try {
            URI uri = URI.create(fileUrl);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(PodcastConstants.CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(PodcastConstants.READ_TIMEOUT_MS);

            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[PodcastConstants.STREAM_BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            log.warn("Could not stream media from URL: {} - Error: {}", fileUrl, e.getMessage());
        }
    }
}
