package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.usecase.GetHqSalesOrderDetailUseCase;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderDetail;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class GetHqSalesOrderDetailService implements GetHqSalesOrderDetailUseCase {
    private final LoadSalesOrderPort loadSalesOrderPort;
    private final LoadSalesOrderStatusHistoryPort loadHistoryPort;

    /**
     * 본사 기준 발주 상세를 조회한다.
     *
     * 흐름:
     * 1) 역할 검증 — ADMIN·HQ_MANAGER·HQ_STAFF만 허용
     * 2) SO 조회 (local DB)
     * 3) 창고명·요청자·승인자는 모두 발주 확정 시점에 박제된 스냅샷에서 읽는다.
     *    HQ는 REQUESTED 이후만 조회하므로 스냅샷이 항상 채워져 외부 서비스 호출이 없다.
     *
     * 트랜잭션: 읽기 전용. 외부 서비스 호출 없음(스냅샷 사용).
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     */
    @Override
    @Transactional(readOnly = true)
    public SalesOrderDetail get(GetHqSalesOrderDetailQuery query) {
        if (!query.role().isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        SalesOrder order = loadSalesOrderPort.load(query.soCode());

        ActorRef requester = order.getRequest() != null ? order.getRequest().requestedBy() : null;

        return new SalesOrderDetail(
                order,
                order.getFrom().nameSnapshot(),
                order.getTo().nameSnapshot(),
                requester,
                findApprovedActor(query.soCode()),
                order.getLines()
        );
    }

    // 승인자는 상태 변경 이력의 APPROVED 행 actor 스냅샷에서 가져온다(미승인이면 null).
    private ActorRef findApprovedActor(String soCode) {
        return loadHistoryPort.findLatestBySoCodeAndStatus(soCode, SalesOrderStatus.APPROVED)
                .map(SalesOrderStatusHistory::actor)
                .orElse(null);
    }
}
