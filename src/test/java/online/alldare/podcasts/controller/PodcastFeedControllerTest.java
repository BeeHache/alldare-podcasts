package online.alldare.podcasts.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import online.alldare.podcasts.domain.PodcastEpisode;
import online.alldare.podcasts.domain.PodcastShow;
import online.alldare.podcasts.repository.PodcastEpisodeRepository;
import online.alldare.podcasts.repository.PodcastShowRepository;
import online.alldare.podcasts.service.PodcastFeedService;
import online.alldare.podcasts.service.PodcastStreamProxyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PodcastFeedControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PodcastShowRepository showRepository;

    @Mock
    private PodcastEpisodeRepository episodeRepository;

    @Mock
    private PodcastFeedService feedService;

    @Mock
    private PodcastStreamProxyService streamProxyService;

    @InjectMocks
    private PodcastFeedController podcastFeedController;

    private PodcastShow testShow;
    private PodcastEpisode testEpisode;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(podcastFeedController).build();

        UUID showId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        testShow = new PodcastShow(showId, creatorId, "building-alldare", "Building Alldare",
                "Official Alldare Podcast", "Technology", "Ben King", "ben@alldare.online",
                "https://cdn.alldare.online/cover.jpg", false);

        UUID episodeId = UUID.randomUUID();
        testEpisode = new PodcastEpisode(episodeId, testShow, UUID.randomUUID(),
                "Episode 1: Spring Boot 4 & Java 25", "Deep dive into microservices",
                1, 1, "https://cdn.alldare.online/ep1.mp3", "audio/mpeg", 900, 15000000L, Instant.now());
    }

    @Test
    void shouldReturnRss2XmlFeedForShowSlugWithCacheHeaders() throws Exception {
        given(feedService.getRssFeed("building-alldare")).willReturn(Optional.of("<rss><channel><title>Building Alldare</title></channel></rss>"));

        mockMvc.perform(get("/podcasts/shows/building-alldare/rss.xml"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().exists("ETag"))
                .andExpect(content().string("<rss><channel><title>Building Alldare</title></channel></rss>"));
    }

    @Test
    void shouldReturnAtomXmlFeedForShowSlugWithCacheHeaders() throws Exception {
        given(feedService.getAtomFeed("building-alldare")).willReturn(Optional.of("<feed><title>Building Alldare</title></feed>"));

        mockMvc.perform(get("/podcasts/shows/building-alldare/atom.xml"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().exists("ETag"))
                .andExpect(content().string("<feed><title>Building Alldare</title></feed>"));
    }

    @Test
    void shouldReturn404WhenShowNotFound() throws Exception {
        given(feedService.getRssFeed("unknown")).willReturn(Optional.empty());

        mockMvc.perform(get("/podcasts/shows/unknown/rss.xml"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldStreamAudioEpisodeWithChunkedEncoding() throws Exception {
        UUID episodeId = testEpisode.getId();
        given(episodeRepository.findById(episodeId)).willReturn(Optional.of(testEpisode));

        mockMvc.perform(get("/podcasts/stream/" + episodeId + ".mp3"))
                .andExpect(status().isOk());
    }

    // --- Show CRUD Tests ---

    @Test
    void shouldGetShowBySlug() throws Exception {
        given(showRepository.findBySlug("building-alldare")).willReturn(Optional.of(testShow));

        mockMvc.perform(get("/api/v1/podcasts/shows/building-alldare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("building-alldare"))
                .andExpect(jsonPath("$.title").value("Building Alldare"));
    }

    @Test
    void shouldGetShowsByCreatorId() throws Exception {
        UUID creatorId = testShow.getCreatorId();
        given(showRepository.findAllByCreatorId(creatorId)).willReturn(List.of(testShow));

        mockMvc.perform(get("/api/v1/podcasts/shows/creator/" + creatorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("building-alldare"));
    }

    @Test
    void shouldDeleteShowById() throws Exception {
        UUID showId = testShow.getId();
        given(showRepository.findById(showId)).willReturn(Optional.of(testShow));

        mockMvc.perform(delete("/api/v1/podcasts/shows/" + showId))
                .andExpect(status().isNoContent());

        verify(showRepository).delete(testShow);
        verify(feedService).evictFeedCache();
    }

    // --- Episode CRUD Tests ---

    @Test
    void shouldGetEpisodeById() throws Exception {
        UUID episodeId = testEpisode.getId();
        given(episodeRepository.findById(episodeId)).willReturn(Optional.of(testEpisode));

        mockMvc.perform(get("/api/v1/podcasts/episodes/" + episodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Episode 1: Spring Boot 4 & Java 25"));
    }

    @Test
    void shouldGetEpisodesByShowId() throws Exception {
        UUID showId = testShow.getId();
        given(episodeRepository.findByShowIdOrderByPublishedAtDesc(showId)).willReturn(List.of(testEpisode));

        mockMvc.perform(get("/api/v1/podcasts/shows/" + showId + "/episodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Episode 1: Spring Boot 4 & Java 25"));
    }

    @Test
    void shouldDeleteEpisodeById() throws Exception {
        UUID episodeId = testEpisode.getId();
        given(episodeRepository.existsById(episodeId)).willReturn(true);

        mockMvc.perform(delete("/api/v1/podcasts/episodes/" + episodeId))
                .andExpect(status().isNoContent());

        verify(episodeRepository).deleteById(episodeId);
        verify(feedService).evictFeedCache();
    }

    @Test
    void shouldCreateEpisode() throws Exception {
        given(episodeRepository.save(org.mockito.ArgumentMatchers.any())).willReturn(testEpisode);

        mockMvc.perform(post("/api/v1/podcasts/episodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEpisode)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldGetAtomFeed() throws Exception {
        given(feedService.getAtomFeed("building-alldare")).willReturn(Optional.of("<feed></feed>"));

        mockMvc.perform(get("/podcasts/shows/building-alldare/atom.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string("<feed></feed>"));
    }

    @Test
    void shouldReturn404WhenShowNotFoundBySlug() throws Exception {
        given(showRepository.findBySlug("nonexistent")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/podcasts/shows/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateShow() throws Exception {
        given(showRepository.findById(testShow.getId())).willReturn(Optional.of(testShow));
        given(showRepository.save(org.mockito.ArgumentMatchers.any())).willReturn(testShow);

        mockMvc.perform(put("/api/v1/podcasts/shows/" + testShow.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testShow)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldUpdateEpisode() throws Exception {
        given(episodeRepository.findById(testEpisode.getId())).willReturn(Optional.of(testEpisode));
        given(episodeRepository.save(org.mockito.ArgumentMatchers.any())).willReturn(testEpisode);

        mockMvc.perform(put("/api/v1/podcasts/episodes/" + testEpisode.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEpisode)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenEpisodeToUpdateNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        given(episodeRepository.findById(missingId)).willReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/podcasts/episodes/" + missingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEpisode)))
                .andExpect(status().isNotFound());
    }
}
