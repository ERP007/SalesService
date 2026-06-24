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
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.WarehouseRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 1) 역할 검증 — BRANCH_MANAGER·BRANCH_STAFF만 허용
     * 2) SO 조회 후 소속 창고(fromWarehouse)가 요청자 창고와 일치하는지 검증
     * 3) 창고명은 Inventory에서 조회한다(DRAFT는 스냅샷이 없어 live 조회 필요).
     * 4) requester는 요청 스냅샷, approver는 APPROVED 이력 행 actor 스냅샷에서 읽는다.
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
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

        // 확정(REQUESTED 이후)은 박제된 창고명 스냅샷을 쓴다(이력 보존 + Inventory 호출 제거).
        // DRAFT는 스냅샷이 없으므로 현재값을 live 조회한다(임시저장 = 현재 진실).
        boolean draft = salesOrder.getStatus() == SalesOrderStatus.DRAFT;
        String fromWarehouseName = warehouseName(salesOrder.getFrom(), draft);
        String toWarehouseName = warehouseName(salesOrder.getTo(), draft);

        ActorRef requester = salesOrder.getRequest() != null ? salesOrder.getRequest().requestedBy() : null;

        return new SalesOrderDetail(salesOrder, fromWarehouseName, toWarehouseName, requester,
                findApprovedActor(query.soCode()));
    }

    private String warehouseName(WarehouseRef ref, boolean draft) {
        if (ref == null || ref.code() == null) {
            return null;
        }
        return draft ? loadWarehousePort.load(ref.code()).warehouseName() : ref.nameSnapshot();
    }

    // 승인자는 상태 변경 이력의 APPROVED 행 actor 스냅샷에서 가져온다(미승인이면 null).
    private ActorRef findApprovedActor(String soCode) {
        return loadHistoryPort.findLatestBySoCodeAndStatus(soCode, SalesOrderStatus.APPROVED)
                .map(SalesOrderStatusHistory::actor)
                .orElse(null);
    }
}
