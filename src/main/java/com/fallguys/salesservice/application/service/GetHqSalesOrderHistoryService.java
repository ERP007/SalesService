package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrderHistoryQuery;
import com.fallguys.salesservice.application.port.inbound.usecase.GetHqSalesOrderHistoryUseCase;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderHistoryEntry;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetHqSalesOrderHistoryService implements GetHqSalesOrderHistoryUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(
            UserRole.ADMIN, UserRole.HQ_MANAGER, UserRole.HQ_STAFF
    );

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final LoadSalesOrderStatusHistoryPort loadHistoryPort;

    /**
     * 본사 기준 발주 변경 이력을 조회한다.
     *
     * 흐름:
     * 1) 역할 검증 — ADMIN·HQ_MANAGER·HQ_STAFF만 허용
     * 2) SO 존재 검증
     * 3) 이력 조회 후 DRAFT 행 제외(HQ는 확정 이후만 본다)
     * 4) 담당자 이름·직급은 각 행에 박제된 actor 스냅샷에서 읽는다(외부 호출 없음).
     *
     * 트랜잭션: 읽기 전용. 외부 서비스 호출 없음(스냅샷 사용).
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-014, 404)
     */
    @Override
    @Transactional(readOnly = true)
    public List<SalesOrderHistoryEntry> get(GetHqSalesOrderHistoryQuery query) {
        if (!ALLOWED_ROLES.contains(query.role())) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        loadSalesOrderPort.load(query.soCode());

        return loadHistoryPort.loadBySoCode(query.soCode()).stream()
                .filter(h -> h.status() != SalesOrderStatus.DRAFT)
                .map(h -> new SalesOrderHistoryEntry(h.status(), h.actor(), h.createdAt()))
                .toList();
    }
}
