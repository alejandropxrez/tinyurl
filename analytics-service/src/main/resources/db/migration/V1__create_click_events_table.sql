CREATE TABLE click_events (
                              id          BIGSERIAL PRIMARY KEY,
                              short_code  VARCHAR(10) NOT NULL,
                              clicked_at  TIMESTAMP NOT NULL,
                              user_agent  TEXT,
                              ip_hash     VARCHAR(64)
);

CREATE INDEX idx_click_events_short_code ON click_events(short_code);