package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderKpiPort;
import com.fallguys.salesservice.application.port.outbound.SalesOrderKpi;
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
    private LoadSalesOrderKpiPort loadSalesOrderKpiPort;

    @InjectMocks
    private GetSalesOrderKpiService sut;

    @Test
    void 정상_조회_성공() {
        // given
        String warehouseCode = "WH-BRANCH-01";
        SalesOrderKpi expected = new SalesOrderKpi(10L, 3L, 4L, 3L);

        given(loadSalesOrderKpiPort.loadByBranchCode(warehouseCode)).willReturn(expected);

        // when
        SalesOrderKpi result = sut.getKpi(warehouseCode, UserRole.BRANCH_STAFF);

        // then
        assertThat(result.totalCount()).isEqualTo(10L);
        assertThat(result.draftCount()).isEqualTo(3L);
        assertThat(result.requestedCount()).isEqualTo(4L);
        assertThat(result.approvedCount()).isEqualTo(3L);
    }

    @Test
    void 발주_없는_지점_조회_시_전부_0_반환() {
        // given
        String warehouseCode = "WH-BRANCH-01";
        SalesOrderKpi emptyKpi = new SalesOrderKpi(0L, 0L, 0L, 0L);

        given(loadSalesOrderKpiPort.loadByBranchCode(warehouseCode)).willReturn(emptyKpi);

        // when
        SalesOrderKpi result = sut.getKpi(warehouseCode, UserRole.BRANCH_STAFF);

        // then
        assertThat(result.totalCount()).isZero();
        assertThat(result.draftCount()).isZero();
        assertThat(result.requestedCount()).isZero();
        assertThat(result.approvedCount()).isZero();
    }

    @Test
    void 미허용_역할_시도시_ForbiddenException() {
        assertThatThrownBy(() -> sut.getKpi("WH-BRANCH-01", UserRole.HQ_MANAGER))
                .isInstanceOf(com.fallguys.salesservice.domain.exception.ForbiddenException.class);

        then(loadSalesOrderKpiPort).shouldHaveNoInteractions();
    }
}
