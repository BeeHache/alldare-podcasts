package online.alldare.podcasts.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import online.alldare.podcasts.domain.PodcastShow;
import online.alldare.podcasts.repository.PodcastEpisodeRepository;
import online.alldare.podcasts.repository.PodcastShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PodcastFeedServiceTest {

    @Mock
    private PodcastShowRepository showRepository;

    @Mock
    private PodcastEpisodeRepository episodeRepository;

    @Mock
    private RssFeedGeneratorService feedGeneratorService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache rssCache;

    @Mock
    private Cache atomCache;

    @InjectMocks
    private PodcastFeedService podcastFeedService;

    private PodcastShow testShow;

    @BeforeEach
    void setUp() {
        testShow = new PodcastShow(UUID.randomUUID(), UUID.randomUUID(), "building-alldare", "Building Alldare",
                "Official Alldare Podcast", "Technology", "Ben King", "ben@alldare.online",
                "https://cdn.alldare.online/cover.jpg", false);
    }

    @Test
    void shouldReturnRssFeedForSlug() {
        given(showRepository.findBySlug("building-alldare")).willReturn(Optional.of(testShow));
        given(episodeRepository.findByShowOrderByPublishedAtDesc(testShow)).willReturn(List.of());
        given(feedGeneratorService.generateRss2Feed(eq(testShow), any())).willReturn("<rss></rss>");

        Optional<String> result = podcastFeedService.getRssFeed("building-alldare");

        assertTrue(result.isPresent());
        assertEquals("<rss></rss>", result.get());
    }

    @Test
    void shouldReturnAtomFeedForSlug() {
        given(showRepository.findBySlug("building-alldare")).willReturn(Optional.of(testShow));
        given(episodeRepository.findByShowOrderByPublishedAtDesc(testShow)).willReturn(List.of());
        given(feedGeneratorService.generateAtomFeed(eq(testShow), any())).willReturn("<feed></feed>");

        Optional<String> result = podcastFeedService.getAtomFeed("building-alldare");

        assertTrue(result.isPresent());
        assertEquals("<feed></feed>", result.get());
    }

    @Test
    void shouldEvictFeedCacheWithCacheManager() {
        given(cacheManager.getCache("podcast_rss_feeds")).willReturn(rssCache);
        given(cacheManager.getCache("podcast_atom_feeds")).willReturn(atomCache);

        podcastFeedService.evictFeedCache();

        verify(rssCache).clear();
        verify(atomCache).clear();
    }

    private static PodcastShow eq(PodcastShow expected) {
        return org.mockito.ArgumentMatchers.eq(expected);
    }
}
