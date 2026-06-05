package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderDetailUseCase;
import com.fallguys.salesservice.application.port.inbound.SalesOrderDetail;
import com.fallguys.salesservice.application.port.outbound.BranchUserInfo;
import com.fallguys.salesservice.application.port.outbound.LoadBranchUserPort;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.LoadWarehousePort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetBranchSalesOrderDetailService implements GetBranchSalesOrderDetailUseCase {

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final LoadBranchUserPort loadBranchUserPort;
    private final LoadWarehousePort loadWarehousePort;

    /**
     * 지점 발주 상세를 조회한다.
     *
     * 흐름:
     * 1) 역할 검증 — BRANCH_MANAGER, BRANCH_STAFF만 허용
     * 2) SO 존재 확인 (local DB)
     * 3) User 서비스 호출 → 요청자의 소속 창고가 SO의 fromWarehouseCode와 일치하는지 검증
     * 4) 창고 서비스 호출 → fromWarehouse, toWarehouse 이름 조회
     *    (toWarehouseCode가 null이면 toWarehouseName은 null로 반환)
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - HQ 계열 또는 미허용 역할: ForbiddenException (SO-05-03, 403)
     * - SO 미존재: ResourceNotFoundException (SO-06-01, 404)
     * - 소속 창고 불일치: ForbiddenException (SO-06-02, 403)
     */
    @Override
    @Transactional(readOnly = true)
    public SalesOrderDetail get(GetBranchSalesOrderDetailQuery query) {
        if (query.role() != UserRole.BRANCH_MANAGER && query.role() != UserRole.BRANCH_STAFF) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }

        SalesOrder salesOrder = loadSalesOrderPort.load(query.soCode());

        BranchUserInfo branchUser = loadBranchUserPort.load(query.userCode());
        if (!branchUser.warehouseCode().equals(salesOrder.getFromWarehouseCode())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        String fromWarehouseName = loadWarehousePort.load(salesOrder.getFromWarehouseCode()).warehouseName();
        String toWarehouseName = salesOrder.getToWarehouseCode() != null
                ? loadWarehousePort.load(salesOrder.getToWarehouseCode()).warehouseName()
                : null;

        return new SalesOrderDetail(salesOrder, fromWarehouseName, toWarehouseName);
    }
}
