-- Requirements: 21.1 - 커밋 후 Kafka 실패 시 다시 발행할 콘텐츠 이벤트.
CREATE TABLE IF NOT EXISTS content_outbox_events (
    id uuid PRIMARY KEY,
    entity_id uuid NOT NULL,
    payload text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    published_at timestamp(6) with time zone
);
CREATE INDEX IF NOT EXISTS idx_content_outbox_pending ON content_outbox_events(created_at, id)
    WHERE published_at IS NULL;
