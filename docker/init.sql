CREATE TABLE IF NOT EXISTS notifications (
    id              UUID            NOT NULL PRIMARY KEY,
    recipient_id    VARCHAR(255)    NOT NULL,
    channel         VARCHAR(10)     NOT NULL,
    template        VARCHAR(50)     NOT NULL,
    recipient       VARCHAR(500)    NOT NULL,
    params_json     TEXT,
    status          VARCHAR(10)     NOT NULL,
    error_message   VARCHAR(1000),
    attempt_count   INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    sent_at         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_id ON notifications(recipient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status       ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_channel      ON notifications(channel);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at   ON notifications(created_at DESC);
