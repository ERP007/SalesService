package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.usecase.GetBranchSalesOrderKpiUseCase;
import com.fallguys.salesservice.application.port.outbound.model.BranchSalesOrderKpi;
import com.fallguys.salesservice.application.port.outbound.port.LoadBranchSalesOrderKpiPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class GetBranchSalesOrderKpiService implements GetBranchSalesOrderKpiUseCase {

    private final LoadBranchSalesOrderKpiPort loadBranchSalesOrderKpiPort;
    /**
     * 지점 KPI를 조회한다.
     *
     * 흐름:
     * 1) 역할 검증 — BRANCH_MANAGER, BRANCH_STAFF만 허용
     * 2) 해당 창고 코드로 SO 상태별 집계 반환
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - HQ 계열 또는 미허용 역할: ForbiddenException (ER-403, 403)
     */
    @Override
    @Transactional(readOnly = true)
    public BranchSalesOrderKpi getKpi(String warehouseCode, UserRole role) {
        if (!role.isBranchUser()) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }
        return loadBranchSalesOrderKpiPort.loadByBranchCode(warehouseCode);
    }
}
