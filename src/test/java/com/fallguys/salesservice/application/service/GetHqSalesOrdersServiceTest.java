package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.outbound.model.UserInfo;
import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrdersQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderSortField;
import com.fallguys.salesservice.application.port.inbound.model.SortDirection;
import com.fallguys.salesservice.application.port.outbound.filter.HqSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.model.HqSalesOrderSummaryPage;
import com.fallguys.salesservice.application.port.outbound.port.LoadHqSalesOrdersPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadUserInfoPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.salesorder.HqSalesOrderSummary;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;



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

    @Mock
    private LoadUserInfoPort loadUserInfoPort;

    @InjectMocks
    private GetHqSalesOrdersService sut;

    @Test
    void 정상_조회_성공() {
        // given
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());

        // when
        HqSalesOrderSummaryPage result = sut.getOrders(query);

        // then
        assertThat(result.content()).isEmpty();
        then(loadUserInfoPort).shouldHaveNoInteractions();
    }

    @Test
    void ADMIN_조회_성공() {
        // given
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.ADMIN, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());

        // when / then
        assertThat(sut.getOrders(query).content()).isEmpty();
    }

    @Test
    void HQ_STAFF_조회_성공() {
        // given
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_STAFF, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());

        // when / then
        assertThat(sut.getOrders(query).content()).isEmpty();
    }

    @Test
    void BRANCH_MANAGER는_ForbiddenException() {
        // given
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.BRANCH_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY);

        // when / then
        assertThatThrownBy(() -> sut.getOrders(query))
                .isInstanceOf(ForbiddenException.class);

        then(loadHqSalesOrdersPort).shouldHaveNoInteractions();
        then(loadUserInfoPort).shouldHaveNoInteractions();
    }

    @Test
    void BRANCH_STAFF는_ForbiddenException() {
        // given
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.BRANCH_STAFF, TEST_TODAY.minusDays(30), TEST_TODAY);

        // when / then
        assertThatThrownBy(() -> sut.getOrders(query))
                .isInstanceOf(ForbiddenException.class);

        then(loadHqSalesOrdersPort).shouldHaveNoInteractions();
        then(loadUserInfoPort).shouldHaveNoInteractions();
    }

    @Test
    void endDate가_오늘_이후면_SalesOrderException() {
        // given
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY.plusDays(1));

        // when / then
        assertThatThrownBy(() -> sut.getOrders(query))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining("endDate");

        then(loadHqSalesOrdersPort).shouldHaveNoInteractions();
        then(loadUserInfoPort).shouldHaveNoInteractions();
    }

    @Test
    void startDate가_endDate보다_늦으면_SalesOrderException() {
        // given
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(10), TEST_TODAY.minusDays(20));

        // when / then
        assertThatThrownBy(() -> sut.getOrders(query))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining("startDate");

        then(loadHqSalesOrdersPort).shouldHaveNoInteractions();
        then(loadUserInfoPort).shouldHaveNoInteractions();
    }

    @Test
    void 조회기간이_365일_초과면_SalesOrderException() {
        // given
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(366), TEST_TODAY);

        // when / then
        assertThatThrownBy(() -> sut.getOrders(query))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining("365일");

        then(loadHqSalesOrdersPort).shouldHaveNoInteractions();
        then(loadUserInfoPort).shouldHaveNoInteractions();
    }

    @Test
    void 조회기간이_정확히_365일이면_성공() {
        // given
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(365), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());

        // when / then
        assertThat(sut.getOrders(query).content()).isEmpty();
    }

    @Test
    void warehouseCode_필터가_포트에_전달됨() {
        // given
        GetHqSalesOrdersQuery query = new GetHqSalesOrdersQuery(
                UserRole.HQ_MANAGER, "BR-04", null, defaultStatuses(),
                TEST_TODAY.minusDays(30), TEST_TODAY,
                SalesOrderSortField.REQUESTED_AT, SortDirection.DESC, 1, 20
        );
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());

        ArgumentCaptor<HqSalesOrderFilter> captor = ArgumentCaptor.forClass(HqSalesOrderFilter.class);

        // when
        sut.getOrders(query);

        // then
        then(loadHqSalesOrdersPort).should().loadOrders(captor.capture());
        assertThat(captor.getValue().warehouseCode()).isEqualTo("BR-04");
    }

    @Test
    void page_1이_포트에_0으로_전달됨() {
        // given
        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(emptyPage());

        ArgumentCaptor<HqSalesOrderFilter> captor = ArgumentCaptor.forClass(HqSalesOrderFilter.class);

        // when
        sut.getOrders(query);

        // then
        then(loadHqSalesOrdersPort).should().loadOrders(captor.capture());
        assertThat(captor.getValue().page()).isZero();
    }

    @Test
    void userInfo_조회_후_requesterName_requesterPosition_채워짐() {
        // given
        HqSalesOrderSummary rawSummary = rawSummary("EMP-001");
        HqSalesOrderSummaryPage rawPage = pageOf(List.of(rawSummary));

        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(rawPage);
        given(loadUserInfoPort.loadByUserCodes(List.of("EMP-001")))
                .willReturn(Map.of("EMP-001", new UserInfo("EMP-001", "정유진", "서비스 매니저")));

        // when
        HqSalesOrderSummaryPage result = sut.getOrders(query);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).requesterName()).isEqualTo("정유진");
        assertThat(result.content().get(0).requesterPosition()).isEqualTo("서비스 매니저");
    }

    @Test
    void userInfo_없으면_requesterName_requesterPosition_null() {
        // given
        HqSalesOrderSummary rawSummary = rawSummary("EMP-999");
        HqSalesOrderSummaryPage rawPage = pageOf(List.of(rawSummary));

        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(rawPage);
        given(loadUserInfoPort.loadByUserCodes(List.of("EMP-999"))).willReturn(Map.of());

        // when
        HqSalesOrderSummaryPage result = sut.getOrders(query);

        // then
        assertThat(result.content().get(0).requesterName()).isNull();
        assertThat(result.content().get(0).requesterPosition()).isNull();
    }

    @Test
    void requestedBy_null이면_userInfo_포트_미호출() {
        // given
        HqSalesOrderSummary rawSummary = rawSummary(null);
        HqSalesOrderSummaryPage rawPage = pageOf(List.of(rawSummary));

        GetHqSalesOrdersQuery query = defaultQuery(UserRole.HQ_MANAGER, TEST_TODAY.minusDays(30), TEST_TODAY);
        given(loadHqSalesOrdersPort.loadOrders(any(HqSalesOrderFilter.class))).willReturn(rawPage);

        // when
        sut.getOrders(query);

        // then
        then(loadUserInfoPort).shouldHaveNoInteractions();
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

    private HqSalesOrderSummaryPage emptyPage() {
        return new HqSalesOrderSummaryPage(List.of(), 1, 20, 0L, 0, false, false);
    }

    private HqSalesOrderSummaryPage pageOf(List<HqSalesOrderSummary> content) {
        return new HqSalesOrderSummaryPage(content, 1, 20, content.size(), 1, false, false);
    }

    private HqSalesOrderSummary rawSummary(String requestedBy) {
        return new HqSalesOrderSummary(
                "SO-2026-06-0001", "BR-04", requestedBy,
                null, null,
                SalesOrderStatus.REQUESTED,
                TEST_INSTANT, TEST_TODAY.plusDays(3),
                2, 100, "EA"
        );
    }
}
