package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrdersQuery;
import com.fallguys.salesservice.application.port.outbound.BranchSalesOrderFilter;
import com.fallguys.salesservice.application.port.outbound.BranchUserInfo;
import com.fallguys.salesservice.application.port.outbound.LoadBranchSalesOrdersPort;
import com.fallguys.salesservice.application.port.outbound.LoadBranchUserPort;
import com.fallguys.salesservice.application.port.outbound.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.SalesOrderSummary;
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
    private LoadBranchUserPort loadBranchUserPort;

    @Mock
    private LoadBranchSalesOrdersPort loadBranchSalesOrdersPort;

    @InjectMocks
    private GetBranchSalesOrdersService sut;

    @Test
    void 정상_조회_성공() {
        // given
        String userCode = "EMP-001";
        String warehouseCode = "WH-BRANCH-01";
        GetBranchSalesOrdersQuery query = defaultQuery(userCode, LocalDate.now().minusDays(30), LocalDate.now());
        SalesOrderSummaryPage expected = emptyPage();

        given(loadBranchUserPort.load(userCode)).willReturn(new BranchUserInfo(warehouseCode));
        given(loadBranchSalesOrdersPort.load(any(BranchSalesOrderFilter.class))).willReturn(expected);

        // when
        SalesOrderSummaryPage result = sut.getBranchOrders(query);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void 사용자_미존재_시_ResourceNotFoundException_전파() {
        // given
        GetBranchSalesOrdersQuery query = defaultQuery("EMP-UNKNOWN", LocalDate.now().minusDays(30), LocalDate.now());

        given(loadBranchUserPort.load("EMP-UNKNOWN"))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.USER_NOT_FOUND));

        // when / then
        assertThatThrownBy(() -> sut.getBranchOrders(query))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(SalesErrorCode.USER_NOT_FOUND.getDefaultMessage());

        then(loadBranchSalesOrdersPort).shouldHaveNoInteractions();
    }

    @Test
    void 창고코드로_포트_호출됨() {
        // given
        String userCode = "EMP-001";
        String warehouseCode = "WH-BRANCH-01";
        GetBranchSalesOrdersQuery query = defaultQuery(userCode, LocalDate.now().minusDays(30), LocalDate.now());

        given(loadBranchUserPort.load(userCode)).willReturn(new BranchUserInfo(warehouseCode));
        given(loadBranchSalesOrdersPort.load(any(BranchSalesOrderFilter.class))).willReturn(emptyPage());

        ArgumentCaptor<BranchSalesOrderFilter> captor = ArgumentCaptor.forClass(BranchSalesOrderFilter.class);

        // when
        sut.getBranchOrders(query);

        // then
        then(loadBranchSalesOrdersPort).should().load(captor.capture());
        assertThat(captor.getValue().warehouseCode()).isEqualTo(warehouseCode);
    }

    @Test
    void 검색어_있을_때_필터에_전달됨() {
        // given
        String userCode = "EMP-001";
        GetBranchSalesOrdersQuery query = new GetBranchSalesOrdersQuery(
                userCode, "엔진오일", defaultStatuses(),
                LocalDate.now().minusDays(30), LocalDate.now(),
                "requestedAt", "desc", 1, 20
        );

        given(loadBranchUserPort.load(userCode)).willReturn(new BranchUserInfo("WH-BRANCH-01"));
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

        then(loadBranchUserPort).shouldHaveNoInteractions();
    }

    @Test
    void startDate가_endDate보다_늦으면_SalesOrderException() {
        // given
        GetBranchSalesOrdersQuery query = defaultQuery("EMP-001", LocalDate.now().minusDays(10), LocalDate.now().minusDays(20));

        // when / then
        assertThatThrownBy(() -> sut.getBranchOrders(query))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining("startDate");

        then(loadBranchUserPort).shouldHaveNoInteractions();
    }

    @Test
    void 조회기간이_365일_초과면_SalesOrderException() {
        // given
        GetBranchSalesOrdersQuery query = defaultQuery("EMP-001", LocalDate.now().minusDays(366), LocalDate.now());

        // when / then
        assertThatThrownBy(() -> sut.getBranchOrders(query))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining("365일");

        then(loadBranchUserPort).shouldHaveNoInteractions();
    }

    @Test
    void 조회기간이_정확히_365일이면_성공() {
        // given
        GetBranchSalesOrdersQuery query = defaultQuery("EMP-001", LocalDate.now().minusDays(365), LocalDate.now());

        given(loadBranchUserPort.load("EMP-001")).willReturn(new BranchUserInfo("WH-BRANCH-01"));
        given(loadBranchSalesOrdersPort.load(any(BranchSalesOrderFilter.class))).willReturn(emptyPage());

        // when / then
        assertThat(sut.getBranchOrders(query)).isEqualTo(emptyPage());
    }

    private GetBranchSalesOrdersQuery defaultQuery(String userCode, LocalDate startDate, LocalDate endDate) {
        return new GetBranchSalesOrdersQuery(
                userCode, null, defaultStatuses(),
                startDate, endDate,
                "requestedAt", "desc", 1, 20
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
