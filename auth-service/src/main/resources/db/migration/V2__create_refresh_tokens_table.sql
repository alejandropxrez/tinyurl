CREATE TABLE refresh_tokens (
                                id          BIGSERIAL PRIMARY KEY,
                                user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token_hash  VARCHAR(64) UNIQUE NOT NULL,
                                created_at  TIMESTAMP NOT NULL DEFAULT now(),
                                expires_at  TIMESTAMP NOT NULL,
                                revoked_at  TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
