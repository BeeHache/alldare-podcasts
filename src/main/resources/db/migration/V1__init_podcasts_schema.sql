CREATE TABLE IF NOT EXISTS podcast_shows (
    id UUID PRIMARY KEY,
    creator_id UUID NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT 'Technology',
    author_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    cover_image_url VARCHAR(1024),
    explicit BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS podcast_episodes (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL REFERENCES podcast_shows(id) ON DELETE CASCADE,
    post_id UUID UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    episode_number INT,
    season_number INT,
    media_url VARCHAR(1024) NOT NULL,
    media_type VARCHAR(100) NOT NULL DEFAULT 'audio/mpeg',
    duration_seconds INT NOT NULL DEFAULT 0,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_podcast_shows_slug ON podcast_shows(slug);
CREATE INDEX idx_podcast_episodes_show_id ON podcast_episodes(show_id);
