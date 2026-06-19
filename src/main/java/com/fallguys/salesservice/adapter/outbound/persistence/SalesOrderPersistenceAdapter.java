package com.fallguys.salesservice.adapter.outbound.persistence;

import com.fallguys.salesservice.adapter.outbound.persistence.salesorder.SalesOrderEntity;
import com.fallguys.salesservice.adapter.outbound.persistence.salesorder.SalesOrderJpaDao;
import com.fallguys.salesservice.adapter.outbound.persistence.salesorderline.SalesOrderLineEntity;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderSortField;
import com.fallguys.salesservice.application.port.inbound.model.SortDirection;
import com.fallguys.salesservice.application.port.outbound.filter.BranchSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.port.GenerateSoCodePort;
import com.fallguys.salesservice.application.port.outbound.port.LoadBranchSalesOrdersPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadBranchSalesOrderKpiPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadHqSalesOrdersPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.model.BranchSalesOrderKpi;
import com.fallguys.salesservice.application.port.outbound.filter.HqSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.model.HqSalesOrderKpi;
import com.fallguys.salesservice.application.port.outbound.model.HqSalesOrderSummaryPage;
import com.fallguys.salesservice.application.port.outbound.port.LoadHqSalesOrderKpiPort;
import com.fallguys.salesservice.application.port.outbound.model.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.salesorder.HqSalesOrderSummary;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SalesOrderPersistenceAdapter implements SaveSalesOrderPort, LoadSalesOrderPort, LoadBranchSalesOrderKpiPort, LoadHqSalesOrderKpiPort, GenerateSoCodePort, LoadBranchSalesOrdersPort, LoadHqSalesOrdersPort {

    // LIKE 와일드카드 이스케이프: 사용자 입력의 %, _, \를 리터럴로 매칭하도록 처리.
    // 페어 쿼리에서 ESCAPE '\\' 절과 함께 사용.
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static Sort.Direction toJpaDirection(SortDirection direction) {
        return direction == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    // 정렬 필드 enum → JPA 경로 매핑. switch exhaustive로 enum 값 추가 시 컴파일 강제 검출.
    private static String toJpaSortField(SalesOrderSortField field) {
        return switch (field) {
            case REQUESTED_AT -> "request.requestedAt";
            case DESIRED_ARRIVAL_DATE -> "desiredArrivalDate";
        };
    }

    private static final Set<SalesOrderStatus> ACTIVE_STATUSES = Set.of(
            SalesOrderStatus.DRAFT,
            SalesOrderStatus.REQUESTED,
            SalesOrderStatus.APPROVED,
            SalesOrderStatus.DELIVERED
    );

    private final SalesOrderJpaDao salesOrderJpaDao;
    private final JdbcTemplate jdbcTemplate;

    // totalCount는 CANCELED·REJECTED 제외한 활성 발주만 집계
    @Override
    public BranchSalesOrderKpi loadByBranchCode(String warehouseCode) {
        List<Object[]> rows = salesOrderJpaDao.countGroupByStatus(warehouseCode);
        Map<SalesOrderStatus, Long> counts = rows.stream()
                .collect(Collectors.toMap(r -> (SalesOrderStatus) r[0], r -> (Long) r[1]));
        long total = ACTIVE_STATUSES.stream()
                .mapToLong(s -> counts.getOrDefault(s, 0L))
                .sum();
        return new BranchSalesOrderKpi(
                total,
                counts.getOrDefault(SalesOrderStatus.DRAFT, 0L),
                counts.getOrDefault(SalesOrderStatus.REQUESTED, 0L),
                counts.getOrDefault(SalesOrderStatus.APPROVED, 0L)
        );
    }

    private static final List<SalesOrderStatus> DELAYED_STATUSES = List.of(
            SalesOrderStatus.REQUESTED,
            SalesOrderStatus.APPROVED
    );

    private static final Set<SalesOrderStatus> HQ_ACTIVE_STATUSES = Set.of(
            SalesOrderStatus.REQUESTED,
            SalesOrderStatus.APPROVED,
            SalesOrderStatus.DELIVERED
    );

    // totalCount는 DRAFT·CANCELED·REJECTED 제외한 활성 발주만 집계
    @Override
    public HqSalesOrderKpi loadHqKpi() {
        List<Object[]> rows = salesOrderJpaDao.countAllGroupByStatus();
        Map<SalesOrderStatus, Long> counts = rows.stream()
                .collect(Collectors.toMap(r -> (SalesOrderStatus) r[0], r -> (Long) r[1]));
        long total = HQ_ACTIVE_STATUSES.stream()
                .mapToLong(s -> counts.getOrDefault(s, 0L))
                .sum();
        long delayed = salesOrderJpaDao.countDelayed(DELAYED_STATUSES, LocalDate.now());
        return new HqSalesOrderKpi(
                total,
                counts.getOrDefault(SalesOrderStatus.REQUESTED, 0L),
                counts.getOrDefault(SalesOrderStatus.APPROVED, 0L),
                delayed
        );
    }

    @Override
    public SalesOrder load(String soCode) {
        return salesOrderJpaDao.findById(soCode)
                .map(SalesOrderEntity::toDomain)
                .orElseThrow(() -> new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND,
                        "발주를 찾을 수 없습니다: " + soCode));
    }

    @Override
    public SalesOrder save(SalesOrder salesOrder) {
        SalesOrderEntity entity = salesOrderJpaDao.findById(salesOrder.getCode())
                .map(existing -> existing.update(salesOrder))
                .orElseGet(() -> SalesOrderEntity.from(salesOrder));
        return salesOrderJpaDao.save(entity).toDomain();
    }

    /**
     * SO 코드를 채번한다.
     *
     * 흐름:
     * 1) 당월 첫날 키로 upsert를 실행한다.
     *    - 행 없음: last_seq = 1 로 INSERT
     *    - 행 있음: last_seq = last_seq + 1 로 UPDATE
     * 2) RETURNING last_seq 로 채번된 값을 단일 쿼리에서 반환한다.
     * 3) SO-YYYY-MM-NNNN 형식으로 코드를 조합한다.
     *
     * 트랜잭션: 호출 측(서비스)의 쓰기 트랜잭션에 참여한다.
     * PostgreSQL 행 수준 원자성으로 별도 락 불필요.
     */
    @Override
    public String generate() {
        LocalDate today = LocalDate.now();
        LocalDate monthKey = today.withDayOfMonth(1);

        Integer lastSeq = jdbcTemplate.queryForObject("""
                INSERT INTO so_number_sequences (seq_date, last_seq)
                VALUES (?, 1)
                ON CONFLICT (seq_date)
                DO UPDATE SET last_seq = so_number_sequences.last_seq + 1
                RETURNING last_seq
                """,
                Integer.class,
                monthKey);

        return String.format("SO-%d-%02d-%04d", today.getYear(), today.getMonthValue(),
                Objects.requireNonNull(lastSeq, "SO 채번 실패: RETURNING last_seq가 null"));
    }

    @Override
    public SalesOrderSummaryPage load(BranchSalesOrderFilter filter) {
        Sort.Direction direction = toJpaDirection(filter.sortDirection());
        String jpaSort = toJpaSortField(filter.sortField());
        Pageable pageable = PageRequest.of(filter.page(), filter.size(), Sort.by(direction, jpaSort));

        String searchPattern = filter.search() != null ? "%" + escapeLike(filter.search()) + "%" : null;

        Page<SalesOrderEntity> page = salesOrderJpaDao.findBranchOrders(
                filter.warehouseCode(),
                searchPattern,
                filter.statuses(),
                filter.startInstant(),
                filter.endInstant(),
                pageable
        );

        List<SalesOrderSummary> summaries = page.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new SalesOrderSummaryPage(
                summaries,
                filter.page() + 1,
                filter.size(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public HqSalesOrderSummaryPage loadOrders(HqSalesOrderFilter filter) {
        Sort.Direction direction = toJpaDirection(filter.sortDirection());
        String jpaSort = toJpaSortField(filter.sortField());
        Pageable pageable = PageRequest.of(filter.page(), filter.size(), Sort.by(direction, jpaSort));

        String searchPattern = filter.search() != null ? "%" + escapeLike(filter.search()) + "%" : null;

        Page<SalesOrderEntity> page = salesOrderJpaDao.findHqOrders(
                filter.warehouseCode(),
                searchPattern,
                filter.statuses(),
                filter.startInstant(),
                filter.endInstant(),
                pageable
        );

        List<HqSalesOrderSummary> summaries = page.getContent().stream()
                .map(this::toHqSummary)
                .toList();

        int currentPage = filter.page() + 1;
        int totalPages = page.getTotalPages();

        return new HqSalesOrderSummaryPage(
                summaries,
                currentPage,
                filter.size(),
                page.getTotalElements(),
                totalPages,
                currentPage > 1,
                currentPage < totalPages
        );
    }

    private HqSalesOrderSummary toHqSummary(SalesOrderEntity entity) {
        List<SalesOrderLineEntity> lines = entity.getLines();
        int totalQuantity = lines.stream().mapToInt(SalesOrderLineEntity::getQuantity).sum();

        String unitSnapshot = null;
        if (!lines.isEmpty()) {
            long distinctUnits = lines.stream()
                    .map(SalesOrderLineEntity::getUnitSnapshot)
                    .distinct()
                    .count();
            unitSnapshot = distinctUnits == 1 ? lines.getFirst().getUnitSnapshot() : null;
        }

        return new HqSalesOrderSummary(
                entity.getCode(),
                entity.getFromWarehouseCode(),
                entity.getRequest() != null ? entity.getRequest().requestedBy() : null,
                null,
                null,
                entity.getStatus(),
                entity.getRequest() != null ? entity.getRequest().requestedAt() : null,
                entity.getDesiredArrivalDate(),
                lines.size(),
                totalQuantity,
                unitSnapshot
        );
    }

    private SalesOrderSummary toSummary(SalesOrderEntity entity) {
        List<SalesOrderLineEntity> lines = entity.getLines();
        int totalQuantity = lines.stream().mapToInt(SalesOrderLineEntity::getQuantity).sum();

        String unitSnapshot = null;
        if (!lines.isEmpty()) {
            long distinctUnits = lines.stream()
                    .map(SalesOrderLineEntity::getUnitSnapshot)
                    .distinct()
                    .count();
            unitSnapshot = distinctUnits == 1 ? lines.getFirst().getUnitSnapshot() : null;
        }

        Instant requestedAt = entity.getRequest() != null ? entity.getRequest().requestedAt() : null;

        return new SalesOrderSummary(
                entity.getCode(),
                entity.getStatus(),
                entity.getDesiredArrivalDate(),
                requestedAt,
                lines.size(),
                totalQuantity,
                unitSnapshot
        );
    }

}
