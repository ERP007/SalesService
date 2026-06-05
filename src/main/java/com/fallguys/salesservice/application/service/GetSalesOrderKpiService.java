package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.GetSalesOrderKpiUseCase;
import com.fallguys.salesservice.application.port.outbound.BranchUserInfo;
import com.fallguys.salesservice.application.port.outbound.LoadBranchUserPort;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderKpiPort;
import com.fallguys.salesservice.application.port.outbound.SalesOrderKpi;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetSalesOrderKpiService implements GetSalesOrderKpiUseCase {

    private final LoadBranchUserPort loadBranchUserPort;
    private final LoadSalesOrderKpiPort loadSalesOrderKpiPort;

    /**
     * 지점 KPI를 조회한다.
     *
     * 흐름:
     * 1) User 서비스 호출 → 사번으로 요청자의 지점 창고 코드 확보
     * 2) 해당 창고 코드로 SO 상태별 집계 반환
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - HQ 계열 또는 미허용 역할: ForbiddenException (SO-05-03, 403)
     * - 사번 미존재: ResourceNotFoundException (SO-05-06, 404)
     */
    @Override
    @Transactional(readOnly = true)
    public SalesOrderKpi getKpi(String requestedBy, UserRole role) {
        if (role != UserRole.BRANCH_MANAGER && role != UserRole.BRANCH_STAFF) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }
        BranchUserInfo branchUser = loadBranchUserPort.load(requestedBy);
        return loadSalesOrderKpiPort.loadByBranchCode(branchUser.warehouseCode());
    }
}
