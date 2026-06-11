package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderDetailUseCase;
import com.fallguys.salesservice.application.port.inbound.HqSalesOrderDetail;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.LoadUserInfoPort;
import com.fallguys.salesservice.application.port.outbound.LoadWarehousePort;
import com.fallguys.salesservice.application.port.outbound.UserInfo;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetHqSalesOrderDetailService implements GetHqSalesOrderDetailUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(
            UserRole.ADMIN, UserRole.HQ_MANAGER, UserRole.HQ_STAFF
    );

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final LoadWarehousePort loadWarehousePort;
    private final LoadUserInfoPort loadUserInfoPort;

    /**
     * 본사 기준 발주 상세를 조회한다.
     *
     * 흐름:
     * 1) 역할 검증 — ADMIN·HQ_MANAGER·HQ_STAFF만 허용
     * 2) SO 조회 (local DB)
     * 3) 창고 서비스 호출 → fromWarehouse·toWarehouse 이름 조회
     * 4) User 서비스 호출 → 요청자(requestedBy) 이름·직급 조회
     *    승인 이력이 있으면 승인자(approvedBy)도 함께 batch 조회
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (ER-403, 403)
     * - SO 미존재: ResourceNotFoundException (SO-018, 404)
     */
    @Override
    @Transactional(readOnly = true)
    public HqSalesOrderDetail get(GetHqSalesOrderDetailQuery query) {
        if (!ALLOWED_ROLES.contains(query.role())) {
            throw new ForbiddenException(CommonErrorCode.UNAUTHORIZED);
        }

        SalesOrder order = loadSalesOrderPort.load(query.soCode());

        String fromWarehouseName = loadWarehousePort.load(order.getFromWarehouseCode()).warehouseName();
        String toWarehouseName = order.getToWarehouseCode() != null
                ? loadWarehousePort.load(order.getToWarehouseCode()).warehouseName()
                : null;

        String requestedBy = order.getRequest() != null ? order.getRequest().requestedBy() : null;
        String approvedBy = order.getApproval() != null ? order.getApproval().approvedBy() : null;

        List<String> userCodes = new ArrayList<>();
        if (requestedBy != null) userCodes.add(requestedBy);
        if (approvedBy != null && !approvedBy.equals(requestedBy)) userCodes.add(approvedBy);

        Map<String, UserInfo> userInfoMap = userCodes.isEmpty()
                ? Map.of()
                : loadUserInfoPort.loadByUserCodes(userCodes);

        return new HqSalesOrderDetail(
                order,
                fromWarehouseName,
                toWarehouseName,
                requestedBy != null ? userInfoMap.get(requestedBy) : null,
                approvedBy != null ? userInfoMap.get(approvedBy) : null
        );
    }
}
