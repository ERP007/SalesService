package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.outbound.model.HqSalesOrderKpi;
import com.fallguys.salesservice.application.port.outbound.port.LoadHqSalesOrderKpiPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
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
class GetHqSalesOrderKpiServiceTest {

    @Mock
    private LoadHqSalesOrderKpiPort loadHqSalesOrderKpiPort;

    @InjectMocks
    private GetHqSalesOrderKpiService sut;

    @Test
    void HQ_MANAGER_정상_조회() {
        HqSalesOrderKpi expected = new HqSalesOrderKpi(214L, 18L, 26L, 7L);
        given(loadHqSalesOrderKpiPort.loadHqKpi()).willReturn(expected);

        HqSalesOrderKpi result = sut.getKpi(UserRole.HQ_MANAGER);

        assertThat(result.totalCount()).isEqualTo(214L);
        assertThat(result.requestedCount()).isEqualTo(18L);
        assertThat(result.approvedCount()).isEqualTo(26L);
        assertThat(result.delayedCount()).isEqualTo(7L);
    }

    @Test
    void HQ_STAFF_정상_조회() {
        HqSalesOrderKpi expected = new HqSalesOrderKpi(10L, 3L, 4L, 1L);
        given(loadHqSalesOrderKpiPort.loadHqKpi()).willReturn(expected);

        HqSalesOrderKpi result = sut.getKpi(UserRole.HQ_STAFF);

        assertThat(result.totalCount()).isEqualTo(10L);
        assertThat(result.requestedCount()).isEqualTo(3L);
        assertThat(result.approvedCount()).isEqualTo(4L);
        assertThat(result.delayedCount()).isEqualTo(1L);
    }

    @Test
    void ADMIN_정상_조회() {
        HqSalesOrderKpi expected = new HqSalesOrderKpi(5L, 1L, 2L, 0L);
        given(loadHqSalesOrderKpiPort.loadHqKpi()).willReturn(expected);

        HqSalesOrderKpi result = sut.getKpi(UserRole.ADMIN);

        assertThat(result.totalCount()).isEqualTo(5L);
        assertThat(result.requestedCount()).isEqualTo(1L);
        assertThat(result.approvedCount()).isEqualTo(2L);
        assertThat(result.delayedCount()).isEqualTo(0L);
    }

    @Test
    void 발주_없을때_전부_0_반환() {
        HqSalesOrderKpi empty = new HqSalesOrderKpi(0L, 0L, 0L, 0L);
        given(loadHqSalesOrderKpiPort.loadHqKpi()).willReturn(empty);

        HqSalesOrderKpi result = sut.getKpi(UserRole.HQ_MANAGER);

        assertThat(result.totalCount()).isZero();
        assertThat(result.requestedCount()).isZero();
        assertThat(result.approvedCount()).isZero();
        assertThat(result.delayedCount()).isZero();
    }

    @Test
    void BRANCH_MANAGER_접근시_ForbiddenException() {
        assertThatThrownBy(() -> sut.getKpi(UserRole.BRANCH_MANAGER))
                .isInstanceOf(ForbiddenException.class);

        then(loadHqSalesOrderKpiPort).shouldHaveNoInteractions();
    }

    @Test
    void BRANCH_STAFF_접근시_ForbiddenException() {
        assertThatThrownBy(() -> sut.getKpi(UserRole.BRANCH_STAFF))
                .isInstanceOf(ForbiddenException.class);

        then(loadHqSalesOrderKpiPort).shouldHaveNoInteractions();
    }
}
