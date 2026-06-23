package com.fallguys.salesservice.adapter.outbound.persistence.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * 발행 완료된 outbox 행을 새벽에 정리한다.
 * PUBLISHED 후 보관 기간(3일)이 지난 행만 삭제한다.
 * PENDING(미발행)·FAILED(정합성 점검 대상)은 보존한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupScheduler {

    private static final Duration RETENTION = Duration.ofDays(3);

    private final OutboxJpaDao outboxJpaDao;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupPublished() {
        Instant cutoff = Instant.now().minus(RETENTION);
        int deleted = outboxJpaDao.deleteByStatusAndPublishedAtBefore(OutboxStatus.PUBLISHED, cutoff);
        log.info("outbox 정리 완료 deleted={} cutoff={}", deleted, cutoff);
    }
}
