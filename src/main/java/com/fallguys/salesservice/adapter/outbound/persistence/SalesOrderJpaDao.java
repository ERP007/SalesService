package com.fallguys.salesservice.adapter.outbound.persistence;

import com.fallguys.salesservice.domain.model.SalesOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    // 지점 발주 목록 페이지 조회 — search는 발주번호·부품코드·부품명 부분 일치, null이면 전체
    @Query(value = """
            SELECT s FROM SalesOrderEntity s
            WHERE s.fromWarehouseCode = :warehouseCode
              AND (:searchPattern IS NULL
                   OR s.code LIKE :searchPattern
                   OR EXISTS (
                     SELECT 1 FROM SalesOrderLineEntity l
                     WHERE l MEMBER OF s.lines
                       AND (l.itemCode LIKE :searchPattern OR l.itemNameSnapshot LIKE :searchPattern)
                   ))
              AND s.status IN :statuses
              AND s.creation.createdAt >= :startInstant
              AND s.creation.createdAt <= :endInstant
            """,
            countQuery = """
            SELECT COUNT(s) FROM SalesOrderEntity s
            WHERE s.fromWarehouseCode = :warehouseCode
              AND (:searchPattern IS NULL
                   OR s.code LIKE :searchPattern
                   OR EXISTS (
                     SELECT 1 FROM SalesOrderLineEntity l
                     WHERE l MEMBER OF s.lines
                       AND (l.itemCode LIKE :searchPattern OR l.itemNameSnapshot LIKE :searchPattern)
                   ))
              AND s.status IN :statuses
              AND s.creation.createdAt >= :startInstant
              AND s.creation.createdAt <= :endInstant
            """)
    Page<SalesOrderEntity> findBranchOrders(
            @Param("warehouseCode") String warehouseCode,
            @Param("searchPattern") String searchPattern,
            @Param("statuses") List<SalesOrderStatus> statuses,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant,
            Pageable pageable
    );
}
