package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.outbound.BranchUserInfo;
import com.fallguys.salesservice.application.port.outbound.LoadBranchUserPort;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderKpiPort;
import com.fallguys.salesservice.application.port.outbound.SalesOrderKpi;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class GetSalesOrderKpiServiceTest {

    @Mock
    private LoadBranchUserPort loadBranchUserPort;

    @Mock
    private LoadSalesOrderKpiPort loadSalesOrderKpiPort;

    @InjectMocks
    private GetSalesOrderKpiService sut;

    @Test
    void 정상_조회_성공() {
        // given
        String userCode = "EMP-001";
        String branchCode = "WH-BRANCH-01";
        BranchUserInfo branchUser = new BranchUserInfo(branchCode);
        SalesOrderKpi expected = new SalesOrderKpi(10L, 4L, 3L, 3L);

        given(loadBranchUserPort.load(userCode)).willReturn(branchUser);
        given(loadSalesOrderKpiPort.loadByBranchCode(branchCode)).willReturn(expected);

        // when
        SalesOrderKpi result = sut.getKpi(branchCode, userCode);

        // then
        assertThat(result.totalCount()).isEqualTo(10L);
        assertThat(result.requestedCount()).isEqualTo(4L);
        assertThat(result.approvedCount()).isEqualTo(3L);
        assertThat(result.deliveredCount()).isEqualTo(3L);
    }

    @Test
    void 발주_없는_지점_조회_시_전부_0_반환() {
        // given
        String userCode = "EMP-001";
        String branchCode = "WH-BRANCH-01";
        BranchUserInfo branchUser = new BranchUserInfo(branchCode);
        SalesOrderKpi emptyKpi = new SalesOrderKpi(0L, 0L, 0L, 0L);

        given(loadBranchUserPort.load(userCode)).willReturn(branchUser);
        given(loadSalesOrderKpiPort.loadByBranchCode(branchCode)).willReturn(emptyKpi);

        // when
        SalesOrderKpi result = sut.getKpi(branchCode, userCode);

        // then
        assertThat(result.totalCount()).isZero();
        assertThat(result.requestedCount()).isZero();
        assertThat(result.approvedCount()).isZero();
        assertThat(result.deliveredCount()).isZero();
    }

    @Test
    void 사용자_미존재_시_ResourceNotFoundException() {
        // given
        String userCode = "EMP-UNKNOWN";
        String branchCode = "WH-BRANCH-01";

        given(loadBranchUserPort.load(userCode))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.USER_NOT_FOUND));

        // when / then
        assertThatThrownBy(() -> sut.getKpi(branchCode, userCode))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(SalesErrorCode.USER_NOT_FOUND.getDefaultMessage());

        then(loadSalesOrderKpiPort).shouldHaveNoInteractions();
    }

    @Test
    void 요청자_창고_불일치_시_ForbiddenException() {
        // given
        String userCode = "EMP-001";
        String requestedBranchCode = "WH-BRANCH-99";
        BranchUserInfo branchUser = new BranchUserInfo("WH-BRANCH-01");

        given(loadBranchUserPort.load(userCode)).willReturn(branchUser);

        // when / then
        assertThatThrownBy(() -> sut.getKpi(requestedBranchCode, userCode))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining(SalesErrorCode.SO_FORBIDDEN.getDefaultMessage());

        then(loadSalesOrderKpiPort).shouldHaveNoInteractions();
    }

    @Test
    void 창고_일치_시_KPI_포트_호출됨() {
        // given
        String userCode = "EMP-001";
        String branchCode = "WH-BRANCH-01";
        BranchUserInfo branchUser = new BranchUserInfo(branchCode);
        SalesOrderKpi kpi = new SalesOrderKpi(5L, 2L, 2L, 1L);

        given(loadBranchUserPort.load(userCode)).willReturn(branchUser);
        given(loadSalesOrderKpiPort.loadByBranchCode(branchCode)).willReturn(kpi);

        // when
        sut.getKpi(branchCode, userCode);

        // then
        then(loadSalesOrderKpiPort).should().loadByBranchCode(branchCode);
    }
}
