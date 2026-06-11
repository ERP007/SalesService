package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderKpiUseCase;
import com.fallguys.salesservice.application.port.outbound.HqSalesOrderKpi;
import com.fallguys.salesservice.application.port.outbound.LoadHqSalesOrderKpiPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetHqSalesOrderKpiService implements GetHqSalesOrderKpiUseCase {

    private final LoadHqSalesOrderKpiPort loadHqSalesOrderKpiPort;

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(
            UserRole.ADMIN,
            UserRole.HQ_MANAGER,
            UserRole.HQ_STAFF
    );

    /**
     * 본사 전체 KPI를 조회한다.
     *
     * 흐름:
     * 1) 역할 검증 — ADMIN, HQ_MANAGER, HQ_STAFF만 허용
     * 2) 전체 SO 상태별 집계 + 지연 카운트 반환
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - 지점 계열 또는 미허용 역할: ForbiddenException (ER-403, 403)
     */
    @Override
    @Transactional(readOnly = true)
    public HqSalesOrderKpi getKpi(UserRole role) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }
        return loadHqSalesOrderKpiPort.loadHqKpi();
    }
}
