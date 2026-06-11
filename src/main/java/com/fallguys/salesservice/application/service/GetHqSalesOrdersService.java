package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrdersQuery;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrdersUseCase;
import com.fallguys.salesservice.application.port.outbound.HqSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.HqSalesOrderSummaryPage;
import com.fallguys.salesservice.application.port.outbound.LoadHqSalesOrdersPort;
import com.fallguys.salesservice.application.port.outbound.LoadUserInfoPort;
import com.fallguys.salesservice.application.port.outbound.UserInfo;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.HqSalesOrderSummary;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetHqSalesOrdersService implements GetHqSalesOrdersUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = Set.of(
            UserRole.ADMIN, UserRole.HQ_MANAGER, UserRole.HQ_STAFF
    );

    private final LoadHqSalesOrdersPort loadHqSalesOrdersPort;
    private final LoadUserInfoPort loadUserInfoPort;

    /**
     * 본사 기준 전체 발주 목록을 페이지네이션으로 조회한다.
     *
     * 흐름:
     * 1) 허용 역할(ADMIN·HQ_MANAGER·HQ_STAFF) 검증
     * 2) 날짜 범위 검증 (endDate ≤ 오늘, startDate ≤ endDate, 기간 ≤ 365일)
     * 3) 날짜를 Instant로 변환 후 필터 구성
     * 4) warehouseCode 지정 시 해당 지점만, 미지정 시 전체 조회
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - endDate가 오늘 이후: SalesOrderException (SO-010, 400)
     * - startDate가 endDate보다 늦음: SalesOrderException (SO-010, 400)
     * - 조회 기간 365일 초과: SalesOrderException (SO-010, 400)
     */
    @Override
    @Transactional(readOnly = true)
    public HqSalesOrderSummaryPage getOrders(GetHqSalesOrdersQuery query) {
        if (!ALLOWED_ROLES.contains(query.role())) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }
        validateDateRange(query.startDate(), query.endDate());

        Instant startInstant = query.startDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = query.endDate().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC).toInstant();

        HqSalesOrderFilter filter = new HqSalesOrderFilter(
                query.warehouseCode(),
                query.search(),
                query.statuses(),
                startInstant,
                endInstant,
                query.sortField(),
                query.sortDirection(),
                query.page() - 1,
                query.size()
        );

        HqSalesOrderSummaryPage rawPage = loadHqSalesOrdersPort.loadOrders(filter);

        List<String> userCodes = rawPage.content().stream()
                .map(HqSalesOrderSummary::requestedBy)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<String, UserInfo> userInfoMap = userCodes.isEmpty()
                ? Map.of()
                : loadUserInfoPort.loadByUserCodes(userCodes);

        List<HqSalesOrderSummary> enriched = rawPage.content().stream()
                .map(summary -> {
                    UserInfo info = summary.requestedBy() != null ? userInfoMap.get(summary.requestedBy()) : null;
                    return new HqSalesOrderSummary(
                            summary.code(),
                            summary.fromWarehouseCode(),
                            summary.requestedBy(),
                            info != null ? info.name() : null,
                            info != null ? info.position() : null,
                            summary.status(),
                            summary.requestedAt(),
                            summary.desiredArrivalDate(),
                            summary.itemCount(),
                            summary.totalQuantity(),
                            summary.unitSnapshot()
                    );
                })
                .toList();

        return new HqSalesOrderSummaryPage(
                enriched,
                rawPage.page(),
                rawPage.size(),
                rawPage.totalElements(),
                rawPage.totalPages(),
                rawPage.hasPrevious(),
                rawPage.hasNext()
        );
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (endDate.isAfter(today)) {
            throw new SalesOrderException(SalesErrorCode.INVALID_QUERY_PARAM,
                    "endDate는 오늘(" + today + ")을 초과할 수 없습니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new SalesOrderException(SalesErrorCode.INVALID_QUERY_PARAM,
                    "startDate가 endDate보다 늦을 수 없습니다.");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 365) {
            throw new SalesOrderException(SalesErrorCode.INVALID_QUERY_PARAM,
                    "조회 기간은 최대 365일입니다.");
        }
    }
}
