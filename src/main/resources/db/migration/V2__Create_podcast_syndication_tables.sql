CREATE TABLE IF NOT EXISTS podcast_syndication_watermarks (
    directory_name VARCHAR(50) PRIMARY KEY,
    last_run_at TIMESTAMP WITH TIME ZONE,
    last_successful_run_at TIMESTAMP WITH TIME ZONE,
    processed_count INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'IDLE',
    error_log TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS podcast_syndications (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL REFERENCES podcast_shows(id) ON DELETE CASCADE,
    directory_name VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    directory_show_url VARCHAR(500),
    claim_url VARCHAR(500),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_podcast_syndication_show_dir UNIQUE (show_id, directory_name)
);

CREATE INDEX idx_podcast_syndications_show_id ON podcast_syndications(show_id);
CREATE INDEX idx_podcast_syndications_dir_status ON podcast_syndications(directory_name, status);
