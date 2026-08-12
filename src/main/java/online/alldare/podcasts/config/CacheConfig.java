package online.alldare.podcasts.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import online.alldare.podcasts.constant.PodcastConstants;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(PodcastConstants.CACHE_MAX_AGE_SECONDS))
                    .disableCachingNullValues();

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(config)
                    .initialCacheNames(java.util.Set.of(PodcastConstants.CACHE_RSS_FEEDS, PodcastConstants.CACHE_ATOM_FEEDS))
                    .build();
        } catch (Exception e) {
            return new ConcurrentMapCacheManager(PodcastConstants.CACHE_RSS_FEEDS, PodcastConstants.CACHE_ATOM_FEEDS);
        }
    }
}
