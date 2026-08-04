CREATE TABLE urls (
                      id            BIGINT PRIMARY KEY,
                      short_code    VARCHAR(11) UNIQUE NOT NULL,
                      original_url  TEXT NOT NULL,
                      user_id       BIGINT,
                      created_at    TIMESTAMP NOT NULL DEFAULT now(),
                      expires_at    TIMESTAMP,
                      click_count   BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_urls_short_code ON urls(short_code););