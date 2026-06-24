package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetBranchSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.usecase.GetBranchSalesOrderDetailUseCase;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderDetail;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadWarehousePort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.ApprovalPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GetBranchSalesOrderDetailService implements GetBranchSalesOrderDetailUseCase {

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final LoadSalesOrderStatusHistoryPort loadHistoryPort;
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
     * - HQ 계열 또는 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - 소속 창고 불일치: ForbiddenException (SO-013, 403)
     */
    @Override
    @Transactional(readOnly = true)
    public SalesOrderDetail get(GetBranchSalesOrderDetailQuery query) {
        if (!query.role().isBranchUser()) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        SalesOrder salesOrder = loadSalesOrderPort.load(query.soCode());

        if (!query.warehouseCode().equals(salesOrder.getFrom().code())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        String fromWarehouseName = loadWarehousePort.load(salesOrder.getFrom().code()).warehouseName();
        String toWarehouseName = salesOrder.getTo().code() != null
                ? loadWarehousePort.load(salesOrder.getTo().code()).warehouseName()
                : null;

        SalesOrderStatusHistory approved = findApprovedRow(query.soCode());
        Instant approvedAt = approved != null ? approved.createdAt() : null;
        ApprovalPayload payload = approved != null && approved.payload() instanceof ApprovalPayload p ? p : null;
        String invoiceNumber = payload != null ? payload.invoiceNumber() : null;
        CarrierType carrierType = payload != null ? payload.carrierType() : null;

        return new SalesOrderDetail(salesOrder,
                fromWarehouseName,
                toWarehouseName,
                approvedAt,
                invoiceNumber,
                carrierType);
    }

    // 승인 부가 데이터는 상태 변경 이력의 APPROVED 행에서 가져온다(없으면 null).
    private SalesOrderStatusHistory findApprovedRow(String soCode) {
        return loadHistoryPort.findLatestBySoCodeAndStatus(soCode, SalesOrderStatus.APPROVED)
                .orElse(null);
    }
}
