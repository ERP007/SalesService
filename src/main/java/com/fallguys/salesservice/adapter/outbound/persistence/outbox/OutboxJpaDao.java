package com.fallguys.salesservice.adapter.outbound.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxJpaDao extends JpaRepository<OutboxEntity, UUID> {

    // 폴러 보조 발행: 미발행(PENDING) 행을 오래된 순으로 가져온다.
    List<OutboxEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
