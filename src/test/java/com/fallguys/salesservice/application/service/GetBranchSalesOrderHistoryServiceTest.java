package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetBranchSalesOrderHistoryQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderHistoryEntry;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadUserInfoPort;
import com.fallguys.salesservice.application.port.outbound.model.UserInfo;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.*;
import com.fallguys.salesservice.domain.model.salesorder.*;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetBranchSalesOrderHistoryServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock LoadSalesOrderStatusHistoryPort loadHistoryPort;
    @Mock LoadUserInfoPort loadUserInfoPort;

    @InjectMocks
    GetBranchSalesOrderHistoryService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String BRANCH_USER_CODE = "BR-001";
    private static final String HQ_USER_CODE = "HQ-001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";

    private static final Instant T1 = Instant.parse("2026-06-01T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-06-02T09:00:00Z");
    private static final Instant T3 = Instant.parse("2026-06-03T09:00:00Z");

    private static final UserInfo BRANCH_USER_INFO = new UserInfo(BRANCH_USER_CODE, "정유진", "서비스 매니저");
    private static final UserInfo HQ_USER_INFO = new UserInfo(HQ_USER_CODE, "강지석", "본사 매니저");

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(order());
        // 이력 테이블은 created_at DESC로 반환한다(DRAFT 포함).
        given(loadHistoryPort.loadBySoCode(SO_CODE)).willReturn(List.of(requestedRow(), draftRow()));
        given(loadUserInfoPort.loadByUserCodes(List.of(BRANCH_USER_CODE)))
                .willReturn(Map.of(BRANCH_USER_CODE, BRANCH_USER_INFO));
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
        then(loadUserInfoPort).shouldHaveNoInteractions();
    }

    @Test
    void DRAFT_포함된_이력_반환() {
        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.BRANCH_MANAGER));

        assertThat(result).anyMatch(e -> e.status() == SalesOrderStatus.DRAFT);
    }

    @Test
    void 이력_created_at_역순_유지() {
        given(loadHistoryPort.loadBySoCode(SO_CODE))
                .willReturn(List.of(approvedRow(), requestedRow(), draftRow()));
        given(loadUserInfoPort.loadByUserCodes(argThat(codes ->
                codes.contains(BRANCH_USER_CODE) && codes.contains(HQ_USER_CODE)
        ))).willReturn(Map.of(
                BRANCH_USER_CODE, BRANCH_USER_INFO,
                HQ_USER_CODE, HQ_USER_INFO
        ));

        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.BRANCH_MANAGER));

        assertThat(result).extracting(SalesOrderHistoryEntry::changedAt)
                .isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    void 이력_changedBy_UserInfo_매핑됨() {
        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.BRANCH_MANAGER));

        SalesOrderHistoryEntry requested = result.stream()
                .filter(e -> e.status() == SalesOrderStatus.REQUESTED)
                .findFirst().orElseThrow();

        assertThat(requested.changedBy().name()).isEqualTo("정유진");
        assertThat(requested.changedBy().position()).isEqualTo("서비스 매니저");
    }

    @Test
    void 담당자_코드_중복이면_user_서비스_1회_호출() {
        // requested·draft 모두 동일 actor → 코드 distinct로 1회만 batch 조회
        service.get(query(UserRole.BRANCH_MANAGER));

        then(loadUserInfoPort).should().loadByUserCodes(List.of(BRANCH_USER_CODE));
    }

    @Test
    void 이력이_DRAFT만_있으면_1건_반환() {
        given(loadHistoryPort.loadBySoCode(SO_CODE)).willReturn(List.of(draftRow()));

        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.BRANCH_MANAGER));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(SalesOrderStatus.DRAFT);
    }

    private GetBranchSalesOrderHistoryQuery query(UserRole role) {
        return new GetBranchSalesOrderHistoryQuery(SO_CODE, role, FROM_WAREHOUSE);
    }

    private SalesOrder order() {
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, "WH-HQ-01",
                SalesOrderStatus.REQUESTED, LocalDate.now().plusDays(3), null,
                new SalesOrderCreation(BRANCH_USER_CODE, T1),
                new SalesOrderRequest(BRANCH_USER_CODE, T2),
                List.of()
        );
    }

    private SalesOrderStatusHistory draftRow() {
        return SalesOrderStatusHistory.of(SO_CODE, SalesOrderStatus.DRAFT, BRANCH_USER_CODE, T1);
    }

    private SalesOrderStatusHistory requestedRow() {
        return SalesOrderStatusHistory.of(SO_CODE, SalesOrderStatus.REQUESTED, BRANCH_USER_CODE, T2);
    }

    private SalesOrderStatusHistory approvedRow() {
        return SalesOrderStatusHistory.of(SO_CODE, SalesOrderStatus.APPROVED, HQ_USER_CODE,
                new ApprovalPayload(T3.atZone(java.time.ZoneOffset.UTC).toLocalDate(), CarrierType.VEHICLE, "INV-001"), T3);
    }
}
