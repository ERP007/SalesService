package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetBranchSalesOrderHistoryQuery;
import com.fallguys.salesservice.application.port.inbound.usecase.GetBranchSalesOrderHistoryUseCase;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderHistoryEntry;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GetBranchSalesOrderHistoryService implements GetBranchSalesOrderHistoryUseCase {
    private final LoadSalesOrderPort loadSalesOrderPort;
    private final LoadSalesOrderStatusHistoryPort loadHistoryPort;

    /**
     * 지점 담당자 기준 발주 변경 이력을 조회한다.
     *
     * 흐름:
     * 1) 역할 검증 — BRANCH_MANAGER·BRANCH_STAFF만 허용
     * 2) SO 조회 (존재 검증 + 소속 창고 확인용)
     * 3) 소속 창고 검증 — fromWarehouse가 요청자 창고와 일치해야 함
     * 4) 이력 테이블 조회(created_at DESC) — DRAFT(생성) 행 포함
     * 5) 담당자 이름·직급은 각 이력 행에 박제된 actor 스냅샷에서 읽는다(외부 호출 없음).
     *    행위자는 DRAFT(생성) 행부터 박제되므로 이름이 채워져 있다.
     *
     * 트랜잭션: 읽기 전용. 외부 서비스 호출 없음(스냅샷 사용).
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     * - 소속 창고 불일치: ForbiddenException (SO-013, 403)
     */
    @Override
    @Transactional(readOnly = true)
    public List<SalesOrderHistoryEntry> get(GetBranchSalesOrderHistoryQuery query) {
        if (!query.role().isBranchUser()) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        SalesOrder order = loadSalesOrderPort.load(query.soCode());

        if (!Objects.equals(query.warehouseCode(), order.getFrom().code())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        return loadHistoryPort.loadBySoCode(query.soCode()).stream()
                .map(h -> new SalesOrderHistoryEntry(h.status(), h.actor(), h.createdAt(), h.payload()))
                .toList();
    }
}
