package online.alldare.podcasts.service;

import java.util.List;
import java.util.Optional;
import online.alldare.podcasts.constant.PodcastConstants;
import online.alldare.podcasts.domain.PodcastEpisode;
import online.alldare.podcasts.domain.PodcastShow;
import online.alldare.podcasts.repository.PodcastEpisodeRepository;
import online.alldare.podcasts.repository.PodcastShowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class PodcastFeedService {

    private static final Logger log = LoggerFactory.getLogger(PodcastFeedService.class);

    @Autowired
    private PodcastShowRepository showRepository;

    @Autowired
    private PodcastEpisodeRepository episodeRepository;

    @Autowired
    private RssFeedGeneratorService feedGeneratorService;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Cacheable(value = PodcastConstants.CACHE_RSS_FEEDS, key = "#slug", unless = "#result == null")
    public Optional<String> getRssFeed(String slug) {
        log.debug("Cache miss for RSS feed of show slug: {}", slug);
        Optional<PodcastShow> showOpt = showRepository.findBySlug(slug);
        if (showOpt.isEmpty() || !showOpt.get().isPublic()) {
            return Optional.empty();
        }
        PodcastShow show = showOpt.get();
        List<PodcastEpisode> episodes = episodeRepository.findByShowOrderByPublishedAtDesc(show);
        return Optional.of(feedGeneratorService.generateRss2Feed(show, episodes));
    }

    @Cacheable(value = PodcastConstants.CACHE_ATOM_FEEDS, key = "#slug", unless = "#result == null")
    public Optional<String> getAtomFeed(String slug) {
        log.debug("Cache miss for Atom feed of show slug: {}", slug);
        Optional<PodcastShow> showOpt = showRepository.findBySlug(slug);
        if (showOpt.isEmpty() || !showOpt.get().isPublic()) {
            return Optional.empty();
        }
        PodcastShow show = showOpt.get();
        List<PodcastEpisode> episodes = episodeRepository.findByShowOrderByPublishedAtDesc(show);
        return Optional.of(feedGeneratorService.generateAtomFeed(show, episodes));
    }

    @CacheEvict(value = {PodcastConstants.CACHE_RSS_FEEDS, PodcastConstants.CACHE_ATOM_FEEDS}, key = "#slug")
    public void evictShowFeedCache(String slug) {
        log.info("Evicted RSS and Atom feed cache for show slug: {}", slug);
    }

    @CacheEvict(value = {PodcastConstants.CACHE_RSS_FEEDS, PodcastConstants.CACHE_ATOM_FEEDS}, allEntries = true)
    public void evictFeedCache() {
        log.info("Evicted all entries from podcast_rss_feeds and podcast_atom_feeds caches");
        if (cacheManager != null) {
            Optional.ofNullable(cacheManager.getCache(PodcastConstants.CACHE_RSS_FEEDS)).ifPresent(c -> c.clear());
            Optional.ofNullable(cacheManager.getCache(PodcastConstants.CACHE_ATOM_FEEDS)).ifPresent(c -> c.clear());
        }
    }
}
