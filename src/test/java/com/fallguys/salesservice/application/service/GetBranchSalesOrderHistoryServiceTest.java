package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetBranchSalesOrderHistoryQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderHistoryEntry;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.UserRole;
import com.fallguys.salesservice.domain.model.WarehouseRef;
import com.fallguys.salesservice.domain.model.salesorder.SagaStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderCreation;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.ApprovalPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetBranchSalesOrderHistoryServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock LoadSalesOrderStatusHistoryPort loadHistoryPort;

    @InjectMocks
    GetBranchSalesOrderHistoryService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";

    private static final Instant T1 = Instant.parse("2026-06-01T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-06-02T09:00:00Z");
    private static final Instant T3 = Instant.parse("2026-06-03T09:00:00Z");

    private static final ActorRef BRANCH_ACTOR = ActorRef.of("BR-001", "정유진", "서비스 매니저");
    private static final ActorRef HQ_ACTOR = ActorRef.of("HQ-001", "강지석", "본사 매니저");

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(order());
        // 이력 테이블은 created_at DESC로 반환한다(DRAFT 포함).
        given(loadHistoryPort.loadBySoCode(SO_CODE)).willReturn(List.of(requestedRow(), draftRow()));
    }

    @Test
    void BRANCH_MANAGER_이력_조회_성공() {
        assertThat(service.get(query(UserRole.BRANCH_MANAGER))).isNotEmpty();
    }

    @Test
    void BRANCH_STAFF_이력_조회_성공() {
        assertThat(service.get(query(UserRole.BRANCH_STAFF))).isNotEmpty();
    }

    @Test
    void HQ_역할_조회_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.get(query(UserRole.HQ_MANAGER)))
                .isInstanceOf(ForbiddenException.class);
        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.get(query(UserRole.BRANCH_MANAGER)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 소속_창고_불일치시_ForbiddenException() {
        GetBranchSalesOrderHistoryQuery query =
                new GetBranchSalesOrderHistoryQuery(SO_CODE, UserRole.BRANCH_MANAGER, "WH-BRANCH-99");

        assertThatThrownBy(() -> service.get(query))
                .isInstanceOf(ForbiddenException.class);
        then(loadHistoryPort).shouldHaveNoInteractions();
    }

    @Test
    void DRAFT_포함된_이력_반환() {
        assertThat(service.get(query(UserRole.BRANCH_MANAGER)))
                .anyMatch(e -> e.status() == SalesOrderStatus.DRAFT);
    }

    @Test
    void 이력_created_at_역순_유지() {
        given(loadHistoryPort.loadBySoCode(SO_CODE))
                .willReturn(List.of(approvedRow(), requestedRow(), draftRow()));

        assertThat(service.get(query(UserRole.BRANCH_MANAGER)))
                .extracting(SalesOrderHistoryEntry::changedAt)
                .isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    void changedBy는_actor_스냅샷에서_매핑됨() {
        SalesOrderHistoryEntry requested = service.get(query(UserRole.BRANCH_MANAGER)).stream()
                .filter(e -> e.status() == SalesOrderStatus.REQUESTED)
                .findFirst().orElseThrow();

        assertThat(requested.changedBy().nameSnapshot()).isEqualTo("정유진");
        assertThat(requested.changedBy().positionSnapshot()).isEqualTo("서비스 매니저");
    }

    @Test
    void 이력이_DRAFT만_있으면_1건_반환() {
        given(loadHistoryPort.loadBySoCode(SO_CODE)).willReturn(List.of(draftRow()));

        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.BRANCH_MANAGER));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo(SalesOrderStatus.DRAFT);
    }

    private GetBranchSalesOrderHistoryQuery query(UserRole role) {
        return new GetBranchSalesOrderHistoryQuery(SO_CODE, role, FROM_WAREHOUSE);
    }

    private SalesOrder order() {
        return new SalesOrder(
                SO_CODE, WarehouseRef.of(FROM_WAREHOUSE, "강남 1지점"), WarehouseRef.of("WH-HQ-01", "본사"),
                SalesOrderStatus.REQUESTED, SagaStatus.NONE, null,
                new SalesOrderCreation(BRANCH_ACTOR, T1),
                new SalesOrderRequest(BRANCH_ACTOR, T2),
                List.of(),
                null
        );
    }

    private SalesOrderStatusHistory draftRow() {
        return SalesOrderStatusHistory.of(SO_CODE, SalesOrderStatus.DRAFT, BRANCH_ACTOR, T1);
    }

    private SalesOrderStatusHistory requestedRow() {
        return SalesOrderStatusHistory.of(SO_CODE, SalesOrderStatus.REQUESTED, BRANCH_ACTOR, T2);
    }

    private SalesOrderStatusHistory approvedRow() {
        return SalesOrderStatusHistory.of(SO_CODE, SalesOrderStatus.APPROVED, HQ_ACTOR,
                new ApprovalPayload(T3.atZone(ZoneOffset.UTC).toLocalDate(), CarrierType.VEHICLE, "INV-001"), T3);
    }
}
