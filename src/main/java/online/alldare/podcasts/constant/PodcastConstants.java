package online.alldare.podcasts.constant;

import online.alldare.common.cache.CacheKeys;
import online.alldare.common.constants.MediaTypes;

public final class PodcastConstants {

    private PodcastConstants() {}

    // Cache Names (Sourced from alldare-common CacheKeys)
    public static final String CACHE_RSS_FEEDS = CacheKeys.PODCAST_RSS_FEEDS;
    public static final String CACHE_ATOM_FEEDS = CacheKeys.PODCAST_ATOM_FEEDS;

    // Media & Content Types (Sourced from alldare-common MediaTypes)
    public static final String CONTENT_TYPE_RSS_XML = MediaTypes.APPLICATION_RSS_XML;
    public static final String CONTENT_TYPE_ATOM_XML = MediaTypes.APPLICATION_ATOM_XML;
    public static final String CONTENT_TYPE_AUDIO_MPEG = MediaTypes.AUDIO_MPEG;
    public static final String CONTENT_TYPE_VIDEO_MP4 = MediaTypes.VIDEO_MP4;
    public static final String TRANSFER_ENCODING_CHUNKED = "chunked";

    // Media Extensions & Formatting (Sourced from alldare-common MediaTypes)
    public static final String MEDIA_EXT_MP3 = MediaTypes.EXT_MP3;
    public static final String MEDIA_EXT_MP4 = MediaTypes.EXT_MP4;
    public static final String DEFAULT_LANGUAGE = "en-us";
    public static final String TIMEZONE_GMT = "GMT";

    // Streaming Configuration & Timeouts
    public static final int STREAM_BUFFER_SIZE = 8192;
    public static final int CONNECT_TIMEOUT_MS = 5000;
    public static final int READ_TIMEOUT_MS = 15000;
    public static final String DEFAULT_PREROLL_AD_PATH = "/ads/sponsors/default_preroll.mp3";

    // Cache Control Durations
    public static final long CACHE_MAX_AGE_SECONDS = 300L;
    public static final long CACHE_S_MAX_AGE_SECONDS = 600L;
}
