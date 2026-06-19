package com.fallguys.salesservice.adapter.outbound.persistence.salesorderhistory;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesOrderStatusHistoryJpaDao extends JpaRepository<SalesOrderStatusHistoryEntity, Long> {
    List<SalesOrderStatusHistoryEntity> findBySoCodeOrderByCreatedAtDesc(String soCode);

    Optional<SalesOrderStatusHistoryEntity> findFirstBySoCodeAndStatusOrderByCreatedAtDesc(
            String soCode, SalesOrderStatus status);
}
