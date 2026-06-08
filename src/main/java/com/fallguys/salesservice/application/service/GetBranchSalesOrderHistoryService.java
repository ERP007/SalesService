package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderHistoryQuery;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderHistoryUseCase;
import com.fallguys.salesservice.application.port.inbound.SalesOrderHistoryEntry;
import com.fallguys.salesservice.application.port.outbound.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.LoadUserInfoPort;
import com.fallguys.salesservice.application.port.outbound.UserInfo;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class GetBranchSalesOrderHistoryService implements GetBranchSalesOrderHistoryUseCase {

    private static final Set<UserRole> ALLOWED_ROLES = EnumSet.of(
            UserRole.BRANCH_MANAGER, UserRole.BRANCH_STAFF
    );

    private final LoadSalesOrderPort loadSalesOrderPort;
    private final LoadUserInfoPort loadUserInfoPort;

    /**
     * 지점 담당자 기준 발주 변경 이력을 조회한다.
     *
     * 흐름:
     * 1) 역할 검증 — BRANCH_MANAGER·BRANCH_STAFF만 허용
     * 2) SO 조회
     * 3) 소속 창고 검증 — fromWarehouseCode가 요청자 창고와 일치해야 함
     * 4) DRAFT 포함 전 상태 이력 구성
     * 5) User 서비스 batch 호출 → 담당자 이름·직급 조회
     *
     * 트랜잭션: 읽기 전용.
     *
     * 예외:
     * - 미허용 역할: ForbiddenException (SO-05-03, 403)
     * - SO 미존재: ResourceNotFoundException (SO-06-01, 404)
     * - 소속 창고 불일치: ForbiddenException (SO-06-02, 403)
     */
    @Override
    @Transactional(readOnly = true)
    public List<SalesOrderHistoryEntry> get(GetBranchSalesOrderHistoryQuery query) {
        if (!ALLOWED_ROLES.contains(query.role())) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }

        SalesOrder order = loadSalesOrderPort.load(query.soCode());

        if (!query.warehouseCode().equals(order.getFromWarehouseCode())) {
            throw new ForbiddenException(SalesErrorCode.SO_FORBIDDEN);
        }

        List<String> actorCodes = collectActorCodes(order);
        Map<String, UserInfo> userInfoMap = actorCodes.isEmpty()
                ? Map.of()
                : loadUserInfoPort.loadByUserCodes(actorCodes);

        return buildEntries(order, userInfoMap);
    }

    private List<String> collectActorCodes(SalesOrder order) {
        Stream.Builder<String> builder = Stream.builder();
        if (order.getCreation() != null) builder.accept(order.getCreation().createdBy());
        if (order.getRequest() != null) builder.accept(order.getRequest().requestedBy());
        if (order.getApproval() != null) builder.accept(order.getApproval().approvedBy());
        if (order.getRejection() != null) builder.accept(order.getRejection().rejectedBy());
        if (order.getDelivery() != null) builder.accept(order.getDelivery().deliveredBy());
        if (order.getCancellation() != null) builder.accept(order.getCancellation().canceledBy());
        return builder.build().filter(Objects::nonNull).distinct().toList();
    }

    private List<SalesOrderHistoryEntry> buildEntries(SalesOrder order, Map<String, UserInfo> userInfoMap) {
        List<SalesOrderHistoryEntry> entries = new ArrayList<>();

        if (order.getCreation() != null) {
            entries.add(new SalesOrderHistoryEntry(
                    SalesOrderStatus.DRAFT,
                    userInfoMap.get(order.getCreation().createdBy()),
                    order.getCreation().createdAt()
            ));
        }
        if (order.getRequest() != null) {
            entries.add(new SalesOrderHistoryEntry(
                    SalesOrderStatus.REQUESTED,
                    userInfoMap.get(order.getRequest().requestedBy()),
                    order.getRequest().requestedAt()
            ));
        }
        if (order.getApproval() != null) {
            entries.add(new SalesOrderHistoryEntry(
                    SalesOrderStatus.APPROVED,
                    userInfoMap.get(order.getApproval().approvedBy()),
                    order.getApproval().approvedAt()
            ));
        }
        if (order.getRejection() != null) {
            entries.add(new SalesOrderHistoryEntry(
                    SalesOrderStatus.REJECTED,
                    userInfoMap.get(order.getRejection().rejectedBy()),
                    order.getRejection().rejectedAt()
            ));
        }
        if (order.getDelivery() != null) {
            entries.add(new SalesOrderHistoryEntry(
                    SalesOrderStatus.DELIVERED,
                    userInfoMap.get(order.getDelivery().deliveredBy()),
                    order.getDelivery().deliveredAt()
            ));
        }
        if (order.getCancellation() != null) {
            entries.add(new SalesOrderHistoryEntry(
                    SalesOrderStatus.CANCELED,
                    userInfoMap.get(order.getCancellation().canceledBy()),
                    order.getCancellation().canceledAt()
            ));
        }

        entries.sort(Comparator.comparing(SalesOrderHistoryEntry::changedAt).reversed());
        return entries;
    }
}
