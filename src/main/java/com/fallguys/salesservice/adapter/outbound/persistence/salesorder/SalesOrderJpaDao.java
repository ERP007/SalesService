package com.fallguys.salesservice.adapter.outbound.persistence.salesorder;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
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
            WHERE s.fromWarehouseCode = :warehouseCode
            GROUP BY s.status
            """)
    List<Object[]> countGroupByStatus(@Param("warehouseCode") String warehouseCode);

    // 전체 상태 집계 — HQ KPI 조회용 (창고 필터 없음)
    @Query("""
            SELECT s.status, COUNT(s)
            FROM SalesOrderEntity s
            GROUP BY s.status
            """)
    List<Object[]> countAllGroupByStatus();

    // 지점 발주 목록 페이지 조회 — search는 발주번호(요청번호)·요청자명 부분 일치, null이면 전체
    // searchPattern은 어댑터에서 %/_/\를 이스케이프 후 전달. ESCAPE '\\'로 와일드카드 인젝션 차단.
    @Query(value = """
            SELECT s FROM SalesOrderEntity s
            WHERE s.fromWarehouseCode = :warehouseCode
              AND (:searchPattern IS NULL
                   OR s.code LIKE :searchPattern ESCAPE '\\'
                   OR s.request.requestedByName LIKE :searchPattern ESCAPE '\\')
              AND s.status IN :statuses
              AND s.creation.createdAt >= :startInstant
              AND s.creation.createdAt <= :endInstant
            """,
            countQuery = """
            SELECT COUNT(s) FROM SalesOrderEntity s
            WHERE s.fromWarehouseCode = :warehouseCode
              AND (:searchPattern IS NULL
                   OR s.code LIKE :searchPattern ESCAPE '\\'
                   OR s.request.requestedByName LIKE :searchPattern ESCAPE '\\')
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

    // HQ 전체 발주 목록 — warehouseCode null이면 전체 지점, 날짜는 requestedAt 기준
    // search는 발주번호(요청번호)·지점명 부분 일치, null이면 전체
    // searchPattern은 어댑터에서 %/_/\를 이스케이프 후 전달. ESCAPE '\\'로 와일드카드 인젝션 차단.
    @Query(value = """
            SELECT s FROM SalesOrderEntity s
            WHERE (:warehouseCode IS NULL OR s.fromWarehouseCode = :warehouseCode)
              AND (:searchPattern IS NULL
                   OR s.code LIKE :searchPattern ESCAPE '\\'
                   OR s.from.name LIKE :searchPattern ESCAPE '\\')
              AND s.status IN :statuses
              AND s.request.requestedAt >= :startInstant
              AND s.request.requestedAt <= :endInstant
            """,
            countQuery = """
            SELECT COUNT(s) FROM SalesOrderEntity s
            WHERE (:warehouseCode IS NULL OR s.fromWarehouseCode = :warehouseCode)
              AND (:searchPattern IS NULL
                   OR s.code LIKE :searchPattern ESCAPE '\\'
                   OR s.from.name LIKE :searchPattern ESCAPE '\\')
              AND s.status IN :statuses
              AND s.request.requestedAt >= :startInstant
              AND s.request.requestedAt <= :endInstant
            """)
    Page<SalesOrderEntity> findHqOrders(
            @Param("warehouseCode") String warehouseCode,
            @Param("searchPattern") String searchPattern,
            @Param("statuses") List<SalesOrderStatus> statuses,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant,
            Pageable pageable
    );
}
