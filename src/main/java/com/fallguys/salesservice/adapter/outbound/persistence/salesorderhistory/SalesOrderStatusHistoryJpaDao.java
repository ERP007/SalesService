package com.fallguys.salesservice.adapter.outbound.persistence.salesorderhistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesOrderStatusHistoryJpaDao extends JpaRepository<SalesOrderStatusHistoryEntity, Long> {
    List<SalesOrderStatusHistoryEntity> findBySoCodeOrderByCreatedAtDesc(String soCode);
}
