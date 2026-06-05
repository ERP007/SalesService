package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.outbound.BranchUserInfo;
import com.fallguys.salesservice.application.port.outbound.LoadBranchUserPort;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderKpiPort;
import com.fallguys.salesservice.application.port.outbound.SalesOrderKpi;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.UserRole;
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
        String warehouseCode = "WH-BRANCH-01";
        BranchUserInfo branchUser = new BranchUserInfo(warehouseCode);
        SalesOrderKpi expected = new SalesOrderKpi(10L, 3L, 4L, 3L);

        given(loadBranchUserPort.load(userCode)).willReturn(branchUser);
        given(loadSalesOrderKpiPort.loadByBranchCode(warehouseCode)).willReturn(expected);

        // when
        SalesOrderKpi result = sut.getKpi(userCode, UserRole.BRANCH_STAFF);

        // then
        assertThat(result.totalCount()).isEqualTo(10L);
        assertThat(result.draftCount()).isEqualTo(3L);
        assertThat(result.requestedCount()).isEqualTo(4L);
        assertThat(result.approvedCount()).isEqualTo(3L);
    }

    @Test
    void 발주_없는_지점_조회_시_전부_0_반환() {
        // given
        String userCode = "EMP-001";
        String warehouseCode = "WH-BRANCH-01";
        BranchUserInfo branchUser = new BranchUserInfo(warehouseCode);
        SalesOrderKpi emptyKpi = new SalesOrderKpi(0L, 0L, 0L, 0L);

        given(loadBranchUserPort.load(userCode)).willReturn(branchUser);
        given(loadSalesOrderKpiPort.loadByBranchCode(warehouseCode)).willReturn(emptyKpi);

        // when
        SalesOrderKpi result = sut.getKpi(userCode, UserRole.BRANCH_STAFF);

        // then
        assertThat(result.totalCount()).isZero();
        assertThat(result.draftCount()).isZero();
        assertThat(result.requestedCount()).isZero();
        assertThat(result.approvedCount()).isZero();
    }

    @Test
    void 사용자_미존재_시_ResourceNotFoundException() {
        // given
        String userCode = "EMP-UNKNOWN";

        given(loadBranchUserPort.load(userCode))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.USER_NOT_FOUND));

        // when / then
        assertThatThrownBy(() -> sut.getKpi(userCode, UserRole.BRANCH_STAFF))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(SalesErrorCode.USER_NOT_FOUND.getDefaultMessage());

        then(loadSalesOrderKpiPort).shouldHaveNoInteractions();
    }

    @Test
    void User서비스_조회_성공_시_해당_창고코드로_KPI_포트_호출됨() {
        // given
        String userCode = "EMP-001";
        String warehouseCode = "WH-BRANCH-01";
        BranchUserInfo branchUser = new BranchUserInfo(warehouseCode);
        SalesOrderKpi kpi = new SalesOrderKpi(5L, 1L, 2L, 2L);

        given(loadBranchUserPort.load(userCode)).willReturn(branchUser);
        given(loadSalesOrderKpiPort.loadByBranchCode(warehouseCode)).willReturn(kpi);

        // when
        sut.getKpi(userCode, UserRole.BRANCH_STAFF);

        // then
        then(loadSalesOrderKpiPort).should().loadByBranchCode(warehouseCode);
    }
}
