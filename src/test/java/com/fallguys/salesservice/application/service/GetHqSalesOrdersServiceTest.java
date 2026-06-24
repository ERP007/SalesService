package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrdersQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderSortField;
import com.fallguys.salesservice.application.port.inbound.model.SortDirection;
import com.fallguys.salesservice.application.port.outbound.filter.HqSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.model.SalesOrderSummaryPage;
import com.fallguys.salesservice.application.port.outbound.port.LoadHqSalesOrdersPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.UserRole;
import com.fallguys.salesservice.domain.model.WarehouseRef;
import com.fallguys.salesservice.domain.model.salesorder.HqSalesOrderSummary;
import com.fallguys.salesservice.domain.model.salesorder.OrderProgress;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class GetHqSalesOrdersServiceTest {

    private static final LocalDate TEST_TODAY = LocalDate.now();
    private static final Instant TEST_INSTANT = Instant.parse("2026-06-08T09:30:00Z");

    @Mock
    private LoadHqSalesOrdersPort loadHqSalesOrdersPort;

    @InjectMocks
    private GetHqSalesOrdersService sut;

    @Test
    void 정상_조회_성공() {
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());

        assertThat(sut.getOrders(query).content()).isEmpty();
    }

    @Test
    void ADMIN_조회_성공() {
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.ADMIN, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());

        assertThat(sut.getOrders(query).content()).isEmpty();
    }

    @Test
    void HQ_STAFF_조회_성공() {
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_STAFF, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());

        assertThat(sut.getOrders(query).content()).isEmpty();
    }

    @Test
    void BRANCH_MANAGER는_ForbiddenException() {
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.BRANCH_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY);

        assertThatThrownBy(() -> sut.getOrders(query)).isInstanceOf(ForbiddenException.class);
        then(loadHqSalesOrdersPort).shouldHaveNoInteractions();
    }

    @Test
    void BRANCH_STAFF는_ForbiddenException() {
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.BRANCH_STAFF, TEST_TODAY.minusDays(30), TEST_TODAY);

        assertThatThrownBy(() -> sut.getOrders(query)).isInstanceOf(ForbiddenException.class);
        then(loadHqSalesOrdersPort).shouldHaveNoInteractions();
    }

    @Test
    void endDate가_오늘_이후면_SalesOrderException() {
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY.plusDays(1));

        assertThatThrownBy(() -> sut.getOrders(query))
                .isInstanceOf(SalesOrderException.class).hasMessageContaining("endDate");
        then(loadHqSalesOrdersPort).shouldHaveNoInteractions();
    }

    @Test
    void startDate가_endDate보다_늦으면_SalesOrderException() {
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(10), TEST_TODAY.minusDays(20));

        assertThatThrownBy(() -> sut.getOrders(query))
                .isInstanceOf(SalesOrderException.class).hasMessageContaining("startDate");
        then(loadHqSalesOrdersPort).shouldHaveNoInteractions();
    }

    @Test
    void 조회기간이_365일_초과면_SalesOrderException() {
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(366), TEST_TODAY);

        assertThatThrownBy(() -> sut.getOrders(query))
                .isInstanceOf(SalesOrderException.class).hasMessageContaining("365일");
        then(loadHqSalesOrdersPort).shouldHaveNoInteractions();
    }

    @Test
    void 조회기간이_정확히_365일이면_성공() {
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(365), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());

        assertThat(sut.getOrders(query).content()).isEmpty();
    }

    @Test
    void warehouseCode_필터가_포트에_전달됨() {
        GetHqSalesOrdersQuery query = new GetHqSalesOrdersQuery(
                UserRole.HQ_MANAGER, "BR-04", null, defaultStatuses(),
                TEST_TODAY.minusDays(30), TEST_TODAY,
                SalesOrderSortField.REQUESTED_AT, SortDirection.DESC, 1, 20
        );
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());
        ArgumentCaptor<HqSalesOrderFilter> captor = ArgumentCaptor.forClass(HqSalesOrderFilter.class);

        sut.getOrders(query);

        then(loadHqSalesOrdersPort).should().loadOrders(captor.capture());
        assertThat(captor.getValue().warehouseCode()).isEqualTo("BR-04");
    }

    @Test
    void page_1이_포트에_0으로_전달됨() {
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());
        ArgumentCaptor<HqSalesOrderFilter> captor = ArgumentCaptor.forClass(HqSalesOrderFilter.class);

        sut.getOrders(query);

        then(loadHqSalesOrdersPort).should().loadOrders(captor.capture());
        assertThat(captor.getValue().page()).isZero();
    }

    @Test
    void 포트_결과가_그대로_반환됨() {
        SalesOrderSummaryPage<HqSalesOrderSummary> rawPage = pageOf(List.of(summary("EMP-001")));
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(rawPage);

        SalesOrderSummaryPage<HqSalesOrderSummary> result = sut.getOrders(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).request().requestedBy().nameSnapshot()).isEqualTo("정유진");
    }

    private GetHqSalesOrdersQuery defaultQuery(UserRole role, LocalDate startDate, LocalDate endDate) {
        return new GetHqSalesOrdersQuery(
                role, null, null, defaultStatuses(),
                startDate, endDate,
                SalesOrderSortField.REQUESTED_AT, SortDirection.DESC, 1, 20
        );
    }

    private List<SalesOrderStatus> defaultStatuses() {
        return List.of(SalesOrderStatus.REQUESTED, SalesOrderStatus.APPROVED);
    }

    private SalesOrderSummaryPage<HqSalesOrderSummary> emptyPage() {
        return new SalesOrderSummaryPage<>(List.of(), 1, 20, 0L, 0);
    }

    private SalesOrderSummaryPage<HqSalesOrderSummary> pageOf(List<HqSalesOrderSummary> content) {
        return new SalesOrderSummaryPage<>(content, 1, 20, content.size(), 1);
    }

    private HqSalesOrderSummary summary(String requestedBy) {
        SalesOrderRequest request = new SalesOrderRequest(
                ActorRef.of(requestedBy, "정유진", "서비스 매니저"), TEST_INSTANT);
        return new HqSalesOrderSummary(
                "SO-2026-06-0001", WarehouseRef.of("BR-04", "강남 4지점"),
                SalesOrderStatus.REQUESTED, OrderProgress.REQUESTED, request, 2);
    }
}
