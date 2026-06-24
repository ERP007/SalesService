package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrdersQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderSortField;
import com.fallguys.salesservice.application.port.inbound.model.SortDirection;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.UserRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public record HqSalesOrderRequest(
        String warehouseCode,
        String search,
        String status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        @Pattern(regexp = "requestedAt",
                message = "sortField는 requestedAt만 허용됩니다")
        String sortField,

        @Pattern(regexp = "asc|desc",
                message = "sortDirection은 asc 또는 desc여야 합니다")
        String sortDirection,

        @Min(value = 1, message = "page는 1 이상이어야 합니다")
        Integer page,

        Integer size
) {
    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 20, 50);
    private static final List<SalesOrderStatus> DEFAULT_STATUSES = List.of(
            SalesOrderStatus.REQUESTED,
            SalesOrderStatus.APPROVED
    );

    @AssertTrue(message = "size는 10, 20, 50 중 하나여야 합니다")
    public boolean isValidSize() {
        return ALLOWED_SIZES.contains(size != null ? size : 20);
    }

    @AssertTrue(message = "유효하지 않은 발주 상태가 포함되어 있습니다")
    public boolean isValidStatus() {
        if (status == null || status.isBlank()) return true;
        try {
            Arrays.stream(status.split(",")).map(String::trim).forEach(SalesOrderStatus::valueOf);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public GetHqSalesOrdersQuery toQuery(UserRole role) {
        List<SalesOrderStatus> statuses = (status != null && !status.isBlank())
                ? Arrays.stream(status.split(",")).map(String::trim).map(SalesOrderStatus::valueOf).toList()
                : DEFAULT_STATUSES;

        return new GetHqSalesOrdersQuery(
                role,
                (warehouseCode != null && !warehouseCode.isBlank()) ? warehouseCode.trim() : null,
                (search != null && !search.isBlank()) ? search.trim() : null,
                statuses,
                startDate != null ? startDate : LocalDate.now().minusDays(90),
                endDate != null ? endDate : LocalDate.now(),
                resolveSortField(sortField),
                resolveSortDirection(sortDirection),
                page != null ? page : 1,
                size != null ? size : 20
        );
    }

    private static SalesOrderSortField resolveSortField(String sortField) {
        // 정렬 기준은 현재 REQUESTED_AT 하나뿐이다.
        return SalesOrderSortField.REQUESTED_AT;
    }

    private static SortDirection resolveSortDirection(String sortDirection) {
        if (sortDirection == null) return SortDirection.DESC;
        return SortDirection.valueOf(sortDirection.toUpperCase());
    }
}
