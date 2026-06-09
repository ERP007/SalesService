package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrdersQuery;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrdersUseCase;
import com.fallguys.salesservice.application.port.outbound.BranchSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.LoadBranchSalesOrdersPort;
import com.fallguys.salesservice.application.port.outbound.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class GetBranchSalesOrdersService implements GetBranchSalesOrdersUseCase {

    private final LoadBranchSalesOrdersPort loadBranchSalesOrdersPort;

    /**
     * 자신 지점의 발주 목록을 페이지네이션으로 조회한다.
     *
     * 흐름:
     * 1) User 서비스 호출 → 사번으로 지점 창고 코드 확보
     * 2) 날짜 파라미터를 Instant로 변환 후 필터 구성
     * 3) 창고 코드 기준으로 필터·정렬·페이지네이션 조회 후 반환
     *
     * 트랜잭션: 읽기 전용. 쿼리 파라미터 기본값은 호출 전 이미 적용된 상태.
     *
     * 예외:
     * - HQ 계열 또는 미허용 역할: ForbiddenException (SO-05-03, 403)
     * - 사번 미존재: ResourceNotFoundException (SO-05-06, 404)
     * - endDate가 오늘 이후: SalesOrderException (SO-05-08, 400)
     * - startDate가 endDate보다 늦음: SalesOrderException (SO-05-08, 400)
     * - 조회 기간 365일 초과: SalesOrderException (SO-05-08, 400)
     */
    @Override
    @Transactional(readOnly = true)
    public SalesOrderSummaryPage getBranchOrders(GetBranchSalesOrdersQuery query) {
        if (query.role() != UserRole.BRANCH_MANAGER && query.role() != UserRole.BRANCH_STAFF) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }
        validateDateRange(query.startDate(), query.endDate());

        Instant startInstant = query.startDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = query.endDate().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC).toInstant();

        BranchSalesOrderFilter filter = new BranchSalesOrderFilter(
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

        return loadBranchSalesOrdersPort.load(filter);
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
