package com.fallguys.salesservice.adapter.outbound.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxJpaDao extends JpaRepository<OutboxEntity, UUID> {

    // 폴러 보조 발행: 미발행(PENDING) 행을 오래된 순으로 가져온다.
    List<OutboxEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    // 보관 정리: 발행 완료(PUBLISHED) 후 cutoff 이전 행을 벌크 삭제한다.
    // PENDING(미발행)·FAILED(점검 대상)는 대상이 아니다.
    @Modifying
    @Query("delete from OutboxEntity o where o.status = :status and o.publishedAt < :cutoff")
    int deleteByStatusAndPublishedAtBefore(@Param("status") OutboxStatus status,
                                           @Param("cutoff") Instant cutoff);
}
