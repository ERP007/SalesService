package com.fallguys.salesservice.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalesOrderJpaDao extends JpaRepository<SalesOrderEntity, String> {

    // 지점 창고 코드별 상태 집계 — KPI 조회용
    @Query("""
            SELECT s.status, COUNT(s)
            FROM SalesOrderEntity s
            WHERE s.fromWarehouseCode = :branchCode
            GROUP BY s.status
            """)
    List<Object[]> countGroupByStatus(@Param("branchCode") String branchCode);
}
