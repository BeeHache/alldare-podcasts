package online.alldare.podcasts.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import online.alldare.common.constants.MediaTypes;
import online.alldare.podcasts.constant.PodcastConstants;
import online.alldare.podcasts.controller.PodcastFeedController;
import online.alldare.podcasts.domain.PodcastEpisode;
import online.alldare.podcasts.domain.PodcastShow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RssFeedGeneratorService {

    private static final DateTimeFormatter RFC_1123_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME
            .withZone(ZoneId.of(PodcastConstants.TIMEZONE_GMT));

    @Value("${alldare.podcasts.feed-base-url:https://podcasts.alldare.online}")
    private String feedBaseUrl;

    public String generateRss2Feed(PodcastShow show, List<PodcastEpisode> episodes) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\"\n");
        xml.append("     xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\"\n");
        xml.append("     xmlns:atom=\"http://www.w3.org/2005/Atom\"\n");
        xml.append("     xmlns:content=\"http://purl.org/rss/1.0/modules/content/\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>").append(escapeXml(show.getTitle())).append("</title>\n");
        xml.append("    <link>").append(feedBaseUrl).append("/podcasts/shows/").append(escapeXml(show.getSlug())).append("</link>\n");
        xml.append("    <description>").append(escapeXml(show.getDescription())).append("</description>\n");
        xml.append("    <language>").append(PodcastConstants.DEFAULT_LANGUAGE).append("</language>\n");
        xml.append("    <copyright>Copyright ").append(show.getCreatedAt().atZone(ZoneId.of(PodcastConstants.TIMEZONE_GMT)).getYear()).append(" ").append(escapeXml(show.getAuthorName())).append("</copyright>\n");
        xml.append("    <atom:link href=\"").append(feedBaseUrl).append("/podcasts/shows/").append(show.getSlug()).append("/rss.xml\" rel=\"self\" type=\"").append(MediaTypes.APPLICATION_RSS_XML_RAW).append("\"/>\n");
        xml.append("    <itunes:author>").append(escapeXml(show.getAuthorName())).append("</itunes:author>\n");
        xml.append("    <itunes:summary>").append(escapeXml(show.getDescription())).append("</itunes:summary>\n");
        xml.append("    <itunes:owner>\n");
        xml.append("      <itunes:name>").append(escapeXml(show.getAuthorName())).append("</itunes:name>\n");
        xml.append("      <itunes:email>").append(escapeXml(show.getEmail())).append("</itunes:email>\n");
        xml.append("    </itunes:owner>\n");
        xml.append("    <itunes:explicit>").append(show.isExplicit() ? "yes" : "no").append("</itunes:explicit>\n");
        
        String coverUrl = (show.getCoverImageUrl() != null && !show.getCoverImageUrl().isBlank())
                ? show.getCoverImageUrl()
                : PodcastFeedController.DEFAULT_COVER_URL;
        xml.append("    <itunes:image href=\"").append(escapeXml(coverUrl)).append("\"/>\n");
        xml.append("    <itunes:category text=\"").append(escapeXml(show.getCategory())).append("\"/>\n\n");

        for (PodcastEpisode episode : episodes) {
            String streamExt = episode.getMediaType().contains("video") ? PodcastConstants.MEDIA_EXT_MP4 : PodcastConstants.MEDIA_EXT_MP3;
            String enclosureUrl = feedBaseUrl + "/podcasts/stream/" + episode.getId() + "." + streamExt;
            String pubDateStr = RFC_1123_FORMATTER.format(episode.getPublishedAt());

            xml.append("    <item>\n");
            xml.append("      <title>").append(escapeXml(episode.getTitle())).append("</title>\n");
            xml.append("      <itunes:title>").append(escapeXml(episode.getTitle())).append("</itunes:title>\n");
            xml.append("      <description>").append(escapeXml(episode.getDescription())).append("</description>\n");
            xml.append("      <pubDate>").append(pubDateStr).append("</pubDate>\n");
            xml.append("      <guid isPermaLink=\"false\">").append(episode.getId()).append("</guid>\n");
            xml.append("      <link>https://alldare.online/episodes/").append(episode.getId()).append("</link>\n");
            xml.append("      <enclosure url=\"").append(enclosureUrl).append("\"\n");
            xml.append("                 length=\"").append(episode.getFileSizeBytes()).append("\"\n");
            xml.append("                 type=\"").append(episode.getMediaType()).append("\"/>\n");
            xml.append("      <itunes:duration>").append(formatDuration(episode.getDurationSeconds())).append("</itunes:duration>\n");
            if (episode.getEpisodeNumber() != null) {
                xml.append("      <itunes:episode>").append(episode.getEpisodeNumber()).append("</itunes:episode>\n");
            }
            if (episode.getSeasonNumber() != null) {
                xml.append("      <itunes:season>").append(episode.getSeasonNumber()).append("</itunes:season>\n");
            }
            xml.append("      <itunes:explicit>").append(show.isExplicit() ? "yes" : "no").append("</itunes:explicit>\n");
            xml.append("    </item>\n");
        }

        xml.append("  </channel>\n");
        xml.append("</rss>");
        return xml.toString();
    }

    public String generateAtomFeed(PodcastShow show, List<PodcastEpisode> episodes) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("  <title>").append(escapeXml(show.getTitle())).append("</title>\n");
        xml.append("  <subtitle>").append(escapeXml(show.getDescription())).append("</subtitle>\n");
        xml.append("  <link href=\"").append(feedBaseUrl).append("/podcasts/shows/").append(show.getSlug()).append("/atom.xml\" rel=\"self\"/>\n");
        xml.append("  <id>urn:uuid:").append(show.getId()).append("</id>\n");
        xml.append("  <updated>").append(show.getUpdatedAt()).append("</updated>\n");
        xml.append("  <author>\n");
        xml.append("    <name>").append(escapeXml(show.getAuthorName())).append("</name>\n");
        xml.append("    <email>").append(escapeXml(show.getEmail())).append("</email>\n");
        xml.append("  </author>\n");

        for (PodcastEpisode episode : episodes) {
            String streamExt = episode.getMediaType().contains("video") ? PodcastConstants.MEDIA_EXT_MP4 : PodcastConstants.MEDIA_EXT_MP3;
            String enclosureUrl = feedBaseUrl + "/podcasts/stream/" + episode.getId() + "." + streamExt;

            xml.append("  <entry>\n");
            xml.append("    <title>").append(escapeXml(episode.getTitle())).append("</title>\n");
            xml.append("    <id>urn:uuid:").append(episode.getId()).append("</id>\n");
            xml.append("    <updated>").append(episode.getPublishedAt()).append("</updated>\n");
            xml.append("    <summary>").append(escapeXml(episode.getDescription())).append("</summary>\n");
            xml.append("    <link rel=\"enclosure\" type=\"").append(episode.getMediaType()).append("\" href=\"").append(enclosureUrl).append("\" length=\"").append(episode.getFileSizeBytes()).append("\"/>\n");
            xml.append("  </entry>\n");
        }

        xml.append("</feed>");
        return xml.toString();
    }

    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

}
