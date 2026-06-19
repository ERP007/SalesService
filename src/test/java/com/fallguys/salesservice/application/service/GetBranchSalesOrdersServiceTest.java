package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetBranchSalesOrdersQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderSortField;
import com.fallguys.salesservice.application.port.inbound.model.SortDirection;
import com.fallguys.salesservice.application.port.outbound.filter.BranchSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.port.LoadBranchSalesOrdersPort;
import com.fallguys.salesservice.application.port.outbound.model.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class GetBranchSalesOrdersServiceTest {

    @Mock
    private LoadBranchSalesOrdersPort loadBranchSalesOrdersPort;

    @InjectMocks
    private GetBranchSalesOrdersService sut;

    @Test
    void 정상_조회_성공() {
        // given
        GetBranchSalesOrdersQuery query = defaultQuery("EMP-001", LocalDate.now().minusDays(30), LocalDate.now());
        SalesOrderSummaryPage expected = emptyPage();

        given(loadBranchSalesOrdersPort.load(any(BranchSalesOrderFilter.class))).willReturn(expected);

        // when
        SalesOrderSummaryPage result = sut.getBranchOrders(query);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void 창고코드로_포트_호출됨() {
        // given
        GetBranchSalesOrdersQuery query = defaultQuery("EMP-001", LocalDate.now().minusDays(30), LocalDate.now());

        given(loadBranchSalesOrdersPort.load(any(BranchSalesOrderFilter.class))).willReturn(emptyPage());

        ArgumentCaptor<BranchSalesOrderFilter> captor = ArgumentCaptor.forClass(BranchSalesOrderFilter.class);

        // when
        sut.getBranchOrders(query);

        // then
        then(loadBranchSalesOrdersPort).should().load(captor.capture());
        assertThat(captor.getValue().warehouseCode()).isEqualTo("WH-BRANCH-01");
    }

    @Test
    void 검색어_있을_때_필터에_전달됨() {
        // given
        GetBranchSalesOrdersQuery query = new GetBranchSalesOrdersQuery(
                "EMP-001", "WH-BRANCH-01", UserRole.BRANCH_STAFF, "엔진오일", defaultStatuses(),
                LocalDate.now().minusDays(30), LocalDate.now(),
                SalesOrderSortField.REQUESTED_AT, SortDirection.DESC, 1, 20
        );

        given(loadBranchSalesOrdersPort.load(any(BranchSalesOrderFilter.class))).willReturn(emptyPage());

        ArgumentCaptor<BranchSalesOrderFilter> captor = ArgumentCaptor.forClass(BranchSalesOrderFilter.class);

        // when
        sut.getBranchOrders(query);

        // then
        then(loadBranchSalesOrdersPort).should().load(captor.capture());
        assertThat(captor.getValue().search()).isEqualTo("엔진오일");
    }

    @Test
    void endDate가_오늘_이후면_SalesOrderException() {
        // given
        GetBranchSalesOrdersQuery query = defaultQuery("EMP-001", LocalDate.now().minusDays(30), LocalDate.now().plusDays(1));

        // when / then
        assertThatThrownBy(() -> sut.getBranchOrders(query))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining("endDate");

        then(loadBranchSalesOrdersPort).shouldHaveNoInteractions();
    }

    @Test
    void startDate가_endDate보다_늦으면_SalesOrderException() {
        // given
        GetBranchSalesOrdersQuery query = defaultQuery("EMP-001", LocalDate.now().minusDays(10), LocalDate.now().minusDays(20));

        // when / then
        assertThatThrownBy(() -> sut.getBranchOrders(query))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining("startDate");

        then(loadBranchSalesOrdersPort).shouldHaveNoInteractions();
    }

    @Test
    void 조회기간이_365일_초과면_SalesOrderException() {
        // given
        GetBranchSalesOrdersQuery query = defaultQuery("EMP-001", LocalDate.now().minusDays(366), LocalDate.now());

        // when / then
        assertThatThrownBy(() -> sut.getBranchOrders(query))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining("365일");

        then(loadBranchSalesOrdersPort).shouldHaveNoInteractions();
    }

    @Test
    void 조회기간이_정확히_365일이면_성공() {
        // given
        GetBranchSalesOrdersQuery query = defaultQuery("EMP-001", LocalDate.now().minusDays(365), LocalDate.now());

        given(loadBranchSalesOrdersPort.load(any(BranchSalesOrderFilter.class))).willReturn(emptyPage());

        // when / then
        assertThat(sut.getBranchOrders(query)).isEqualTo(emptyPage());
    }

    private GetBranchSalesOrdersQuery defaultQuery(String userCode, LocalDate startDate, LocalDate endDate) {
        return new GetBranchSalesOrdersQuery(
                userCode, "WH-BRANCH-01", UserRole.BRANCH_STAFF, null, defaultStatuses(),
                startDate, endDate,
                SalesOrderSortField.REQUESTED_AT, SortDirection.DESC, 1, 20
        );
    }

    private List<SalesOrderStatus> defaultStatuses() {
        return List.of(SalesOrderStatus.DRAFT, SalesOrderStatus.REQUESTED,
                SalesOrderStatus.APPROVED, SalesOrderStatus.DELIVERED);
    }

    private SalesOrderSummaryPage emptyPage() {
        return new SalesOrderSummaryPage(List.of(), 1, 20, 0L, 0);
    }
}
