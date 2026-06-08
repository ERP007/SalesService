package com.fallguys.salesservice.adapter.outbound.persistence;

import com.fallguys.salesservice.application.port.outbound.BranchSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.GenerateSoCodePort;
import com.fallguys.salesservice.application.port.outbound.LoadBranchSalesOrdersPort;
import com.fallguys.salesservice.application.port.outbound.LoadBranchSalesOrderKpiPort;
import com.fallguys.salesservice.application.port.outbound.LoadHqSalesOrdersPort;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.BranchSalesOrderKpi;
import com.fallguys.salesservice.application.port.outbound.HqSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.HqSalesOrderKpi;
import com.fallguys.salesservice.application.port.outbound.HqSalesOrderSummaryPage;
import com.fallguys.salesservice.application.port.outbound.LoadHqSalesOrderKpiPort;
import com.fallguys.salesservice.application.port.outbound.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.HqSalesOrderSummary;
import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.SalesOrderSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SalesOrderPersistenceAdapter implements SaveSalesOrderPort, LoadSalesOrderPort, LoadBranchSalesOrderKpiPort, LoadHqSalesOrderKpiPort, GenerateSoCodePort, LoadBranchSalesOrdersPort, LoadHqSalesOrdersPort {

    private static final Map<String, String> SORT_FIELD_TO_JPA = Map.of(
            "requestedAt", "request.requestedAt",
            "desiredArrivalDate", "desiredArrivalDate"
    );

    private static final Set<SalesOrderStatus> ACTIVE_STATUSES = Set.of(
            SalesOrderStatus.DRAFT,
            SalesOrderStatus.REQUESTED,
            SalesOrderStatus.APPROVED,
            SalesOrderStatus.DELIVERED
    );

    private final SalesOrderJpaDao salesOrderJpaDao;
    private final SoNumberSequenceJpaDao soNumberSequenceJpaDao;

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
     * 1) 당월 첫날 키로 시퀀스 행을 비관적 락으로 조회한다.
     * 2) 행이 있으면 lastSeq를 증가, 없으면 1로 신규 생성한다.
     * 3) 저장 후 SO-YYYY-MM-NNNN 형식으로 반환한다.
     *
     * 트랜잭션: 호출 측(서비스)의 쓰기 트랜잭션에 참여한다.
     * 비관적 락으로 동시 채번 시 중복 코드를 방지한다.
     */
    @Override
    public String generate() {
        LocalDate today = LocalDate.now();
        LocalDate monthKey = today.withDayOfMonth(1);

        SoNumberSequenceEntity seq = resolveSequence(monthKey);
        return String.format("SO-%d-%02d-%04d", today.getYear(), today.getMonthValue(), seq.getLastSeq());
    }

    @Override
    public SalesOrderSummaryPage load(BranchSalesOrderFilter filter) {
        Sort.Direction direction = "asc".equals(filter.sortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String jpaSort = SORT_FIELD_TO_JPA.get(filter.sortField());
        Pageable pageable = PageRequest.of(filter.page(), filter.size(), Sort.by(direction, jpaSort));

        String searchPattern = filter.search() != null ? "%" + filter.search() + "%" : null;

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
        Sort.Direction direction = "asc".equals(filter.sortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String jpaSort = SORT_FIELD_TO_JPA.get(filter.sortField());
        Pageable pageable = PageRequest.of(filter.page(), filter.size(), Sort.by(direction, jpaSort));

        String searchPattern = filter.search() != null ? "%" + filter.search() + "%" : null;

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
        int totalQuantity = lines.stream().mapToInt(SalesOrderLineEntity::getRequestedQuantity).sum();

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
        int totalQuantity = lines.stream().mapToInt(SalesOrderLineEntity::getRequestedQuantity).sum();

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

    // 월 첫 채번 시 동시 insert 충돌 방어: DataIntegrityViolationException 발생 시 재조회 후 증가
    private SoNumberSequenceEntity resolveSequence(LocalDate monthKey) {
        try {
            SoNumberSequenceEntity seq = soNumberSequenceJpaDao.findByIdWithLock(monthKey)
                    .map(SoNumberSequenceEntity::increment)
                    .orElseGet(() -> SoNumberSequenceEntity.createFirst(monthKey));
            return soNumberSequenceJpaDao.save(seq);
        } catch (DataIntegrityViolationException e) {
            SoNumberSequenceEntity seq = soNumberSequenceJpaDao.findByIdWithLock(monthKey)
                    .orElseThrow(() -> new IllegalStateException("SO 채번 시퀀스 재조회 실패"));
            seq.increment();
            return soNumberSequenceJpaDao.save(seq);
        }
    }
}
