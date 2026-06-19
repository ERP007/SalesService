package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetBranchSalesOrderHistoryQuery;
import com.fallguys.salesservice.application.port.inbound.usecase.GetBranchSalesOrderHistoryUseCase;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderHistoryEntry;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadUserInfoPort;
import com.fallguys.salesservice.application.port.outbound.model.UserInfo;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetBranchSalesOrderHistoryService implements GetBranchSalesOrderHistoryUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(
            UserRole.BRANCH_MANAGER, UserRole.BRANCH_STAFF
    );

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final LoadSalesOrderStatusHistoryPort loadHistoryPort;
    private final LoadUserInfoPort loadUserInfoPort;

    /**
     * 지점 담당자 기준 발주 변경 이력을 조회한다.
     *
     * 흐름:
     * 1) 역할 검증 — BRANCH_MANAGER·BRANCH_STAFF만 허용
     * 2) SO 조회 (존재 검증 + 소속 창고 확인용)
     * 3) 소속 창고 검증 — fromWarehouseCode가 요청자 창고와 일치해야 함
     * 4) 이력 테이블 조회(created_at DESC) — DRAFT(생성) 행 포함
     * 5) User 서비스 batch 호출 → 담당자 이름·직급 조회
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
    public List<SalesOrderHistoryEntry> get(GetBranchSalesOrderHistoryQuery query) {
        if (!ALLOWED_ROLES.contains(query.role())) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        SalesOrder order = loadSalesOrderPort.load(query.soCode());

        if (!Objects.equals(query.warehouseCode(), order.getFromWarehouseCode())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        List<SalesOrderStatusHistory> histories = loadHistoryPort.loadBySoCode(query.soCode());

        return toEntries(histories);
    }

    private List<SalesOrderHistoryEntry> toEntries(List<SalesOrderStatusHistory> histories) {
        List<String> actorCodes = histories.stream()
                .map(SalesOrderStatusHistory::actorCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, UserInfo> userInfoMap = actorCodes.isEmpty()
                ? Map.of()
                : loadUserInfoPort.loadByUserCodes(actorCodes);

        return histories.stream()
                .map(h -> new SalesOrderHistoryEntry(
                        h.status(),
                        userInfoMap.get(h.actorCode()),
                        h.createdAt()))
                .toList();
    }
}
