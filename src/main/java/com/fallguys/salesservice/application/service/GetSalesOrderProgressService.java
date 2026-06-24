package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.model.SalesOrderProgressView;
import com.fallguys.salesservice.application.port.inbound.query.GetSalesOrderProgressQuery;
import com.fallguys.salesservice.application.port.inbound.usecase.GetSalesOrderProgressUseCase;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetSalesOrderProgressService implements GetSalesOrderProgressUseCase {

    private final LoadSalesOrderPort loadSalesOrderPort;

    /**
     * 진행 상태(폴링)를 조회한다.
     *
     * 흐름:
     * 1) 역할 검증 — 지점은 소속 창고(fromWarehouse) 일치, 본사는 전체 허용.
     * 2) SO 조회 후 진행 뷰 구성(pending·outcome은 saga 상태로 백엔드가 판단).
     *
     * 트랜잭션: 읽기 전용. 외부 호출 없음(local DB만) — 폴링에 적합.
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - 지점 소속 창고 불일치: ForbiddenException (SO-013, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     */
    @Override
    @Transactional(readOnly = true)
    public SalesOrderProgressView get(GetSalesOrderProgressQuery query) {
        SalesOrder order = loadSalesOrderPort.load(query.soCode());

        if (query.role().isBranchUser()) {
            if (!query.warehouseCode().equals(order.getFrom().code())) {
                throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
            }
        } else if (!query.role().isHqUser()) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        return SalesOrderProgressView.from(order);
    }
}
