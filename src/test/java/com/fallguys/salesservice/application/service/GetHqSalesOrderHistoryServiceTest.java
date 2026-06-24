package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrderHistoryQuery;
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
import com.fallguys.salesservice.domain.model.salesorderhistory.CancellationPayload;
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
class GetHqSalesOrderHistoryServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock LoadSalesOrderStatusHistoryPort loadHistoryPort;

    @InjectMocks
    GetHqSalesOrderHistoryService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";

    private static final Instant T1 = Instant.parse("2026-06-01T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-06-02T09:00:00Z");
    private static final Instant T3 = Instant.parse("2026-06-03T09:00:00Z");

    private static final ActorRef BRANCH_ACTOR = ActorRef.of("BR-001", "정유진", "서비스 매니저");
    private static final ActorRef HQ_ACTOR = ActorRef.of("HQ-001", "강지석", "본사 매니저");

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(order(SalesOrderStatus.REQUESTED));
        // 이력 테이블은 created_at DESC로 반환한다.
        given(loadHistoryPort.loadBySoCode(SO_CODE)).willReturn(List.of(requestedRow(), draftRow()));
    }

    @Test
    void HQ_MANAGER_이력_조회_성공() {
        assertThat(service.get(query(UserRole.HQ_MANAGER))).isNotEmpty();
    }

    @Test
    void HQ_STAFF_이력_조회_성공() {
        assertThat(service.get(query(UserRole.HQ_STAFF))).isNotEmpty();
    }

    @Test
    void ADMIN_이력_조회_성공() {
        assertThat(service.get(query(UserRole.ADMIN))).isNotEmpty();
    }

    @Test
    void BRANCH_MANAGER_조회_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.get(query(UserRole.BRANCH_MANAGER)))
                .isInstanceOf(ForbiddenException.class);
        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void BRANCH_STAFF_조회_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.get(query(UserRole.BRANCH_STAFF)))
                .isInstanceOf(ForbiddenException.class);
        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.get(query(UserRole.HQ_MANAGER)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void DRAFT_이력_미포함() {
        assertThat(service.get(query(UserRole.HQ_MANAGER)))
                .noneMatch(e -> e.status() == SalesOrderStatus.DRAFT);
    }

    @Test
    void 이력_created_at_역순_유지() {
        given(loadHistoryPort.loadBySoCode(SO_CODE))
                .willReturn(List.of(approvedRow(), requestedRow(), draftRow()));

        assertThat(service.get(query(UserRole.HQ_MANAGER)))
                .extracting(SalesOrderHistoryEntry::changedAt)
                .isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    void changedBy는_actor_스냅샷에서_매핑됨() {
        SalesOrderHistoryEntry requested = service.get(query(UserRole.HQ_MANAGER)).stream()
                .filter(e -> e.status() == SalesOrderStatus.REQUESTED)
                .findFirst().orElseThrow();

        assertThat(requested.changedBy().nameSnapshot()).isEqualTo("정유진");
        assertThat(requested.changedBy().positionSnapshot()).isEqualTo("서비스 매니저");
    }

    @Test
    void 승인_이력_payload_포함() {
        given(loadHistoryPort.loadBySoCode(SO_CODE))
                .willReturn(List.of(approvedRow(), requestedRow(), draftRow()));

        SalesOrderHistoryEntry approved = service.get(query(UserRole.HQ_MANAGER)).stream()
                .filter(e -> e.status() == SalesOrderStatus.APPROVED)
                .findFirst().orElseThrow();

        assertThat(approved.payload()).isInstanceOf(ApprovalPayload.class);
        assertThat(approved.changedBy().nameSnapshot()).isEqualTo("강지석");
    }

    @Test
    void 취소된_발주_CANCELED_이력_포함() {
        given(loadHistoryPort.loadBySoCode(SO_CODE))
                .willReturn(List.of(canceledRow(), requestedRow(), draftRow()));

        assertThat(service.get(query(UserRole.HQ_MANAGER)))
                .anyMatch(e -> e.status() == SalesOrderStatus.CANCELED);
    }

    private GetHqSalesOrderHistoryQuery query(UserRole role) {
        return new GetHqSalesOrderHistoryQuery(SO_CODE, role);
    }

    private SalesOrder order(SalesOrderStatus status) {
        return new SalesOrder(
                SO_CODE, WarehouseRef.of(FROM_WAREHOUSE, "강남 1지점"), WarehouseRef.of("WH-HQ-01", "본사"),
                status, SagaStatus.NONE, null,
                new SalesOrderCreation(BRANCH_ACTOR, T1),
                new SalesOrderRequest(BRANCH_ACTOR, T2),
                List.of()
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

    private SalesOrderStatusHistory canceledRow() {
        return SalesOrderStatusHistory.of(SO_CODE, SalesOrderStatus.CANCELED, BRANCH_ACTOR,
                new CancellationPayload("재고 확인 후 취소"), T3);
    }
}
