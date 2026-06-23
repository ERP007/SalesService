-- 트랜잭셔널 outbox. 비즈니스 변경과 같은 트랜잭션에 메시지를 적재해 발행 원자성을 보장한다.
-- payload는 직렬화된 BaseEvent envelope 전체(JSON 문자열).
CREATE TABLE outbox (
    event_id       UUID         NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(255) NOT NULL,
    retry_count    INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    published_at   TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT pk_outbox PRIMARY KEY (event_id)
);

-- 폴러가 미발행 행을 오래된 순으로 스캔한다.
CREATE INDEX idx_outbox_status_created_at ON outbox (status, created_at);
