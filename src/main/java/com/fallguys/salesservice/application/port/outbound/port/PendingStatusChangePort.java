package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.domain.model.salesorderhistory.PendingStatusChange;

import java.util.Optional;

/**
 * 재고 saga 확정 대기 중인 상태 변경(in-flight)을 staging한다.
 * 행위 시점에 save, saga 성공 시 findBySoCode로 읽어 이력 승격 후 remove, 실패 시 remove.
 */
public interface PendingStatusChangePort {
    void save(PendingStatusChange pending);

    Optional<PendingStatusChange> findBySoCode(String soCode);

    void removeBySoCode(String soCode);
}
