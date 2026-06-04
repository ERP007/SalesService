package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.GetSalesOrderKpiUseCase;
import com.fallguys.salesservice.application.port.outbound.BranchUserInfo;
import com.fallguys.salesservice.application.port.outbound.LoadBranchUserPort;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderKpiPort;
import com.fallguys.salesservice.application.port.outbound.SalesOrderKpi;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
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
     * 2) 요청한 branchCode와 일치하는지 검증
     * 3) 일치하면 해당 지점의 SO 상태별 집계 반환
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - 사번 미존재: ResourceNotFoundException (SO-05-06, 404)
     * - 지점 불일치: ForbiddenException (SO-06-02, 403)
     */
    @Override
    @Transactional(readOnly = true)
    public SalesOrderKpi getKpi(String branchCode, String requestedBy) {
        BranchUserInfo branchUser = loadBranchUserPort.load(requestedBy);
        if (!branchUser.warehouseCode().equals(branchCode)) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }
        return loadSalesOrderKpiPort.loadByBranchCode(branchCode);
    }
}
