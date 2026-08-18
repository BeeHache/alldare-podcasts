-- Migration V4: Add is_public column to podcast_shows table
ALTER TABLE podcast_shows ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_podcast_shows_is_public ON podcast_shows(is_public);
