package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.query.GetHqSalesOrderHistoryQuery;
import com.fallguys.salesservice.application.port.inbound.model.SalesOrderHistoryEntry;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadUserInfoPort;
import com.fallguys.salesservice.application.port.outbound.model.UserInfo;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.*;
import com.fallguys.salesservice.domain.model.salesorder.*;
import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;
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
class GetHqSalesOrderHistoryServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock LoadUserInfoPort loadUserInfoPort;

    @InjectMocks
    GetHqSalesOrderHistoryService service;

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
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder());
        given(loadUserInfoPort.loadByUserCodes(List.of(BRANCH_USER_CODE)))
                .willReturn(Map.of(BRANCH_USER_CODE, BRANCH_USER_INFO));
    }

    @Test
    void HQ_MANAGER_이력_조회_성공() {
        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result).isNotEmpty();
    }

    @Test
    void HQ_STAFF_이력_조회_성공() {
        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.HQ_STAFF));

        assertThat(result).isNotEmpty();
    }

    @Test
    void ADMIN_이력_조회_성공() {
        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.ADMIN));

        assertThat(result).isNotEmpty();
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
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(ex -> assertThat(((ResourceNotFoundException) ex).getCode())
                        .isEqualTo(SalesErrorCode.SO_NOT_FOUND.getCode()));
    }

    @Test
    void DRAFT_이력_미포함() {
        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result).noneMatch(e -> e.status() == SalesOrderStatus.DRAFT);
    }

    @Test
    void 이력_changedAt_역순_정렬() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(approvedOrder());
        given(loadUserInfoPort.loadByUserCodes(argThat(codes ->
                codes.contains(BRANCH_USER_CODE) && codes.contains(HQ_USER_CODE)
        ))).willReturn(Map.of(
                BRANCH_USER_CODE, BRANCH_USER_INFO,
                HQ_USER_CODE, HQ_USER_INFO
        ));

        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result).extracting(SalesOrderHistoryEntry::changedAt)
                .isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    void 이력_changedBy_UserInfo_매핑됨() {
        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.HQ_MANAGER));

        SalesOrderHistoryEntry requested = result.stream()
                .filter(e -> e.status() == SalesOrderStatus.REQUESTED)
                .findFirst().orElseThrow();

        assertThat(requested.changedBy().name()).isEqualTo("정유진");
        assertThat(requested.changedBy().position()).isEqualTo("서비스 매니저");
    }

    @Test
    void 승인_이력_포함시_requester와_approver_batch_조회() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(approvedOrder());
        given(loadUserInfoPort.loadByUserCodes(argThat(codes ->
                codes.contains(BRANCH_USER_CODE) && codes.contains(HQ_USER_CODE) && codes.size() == 2
        ))).willReturn(Map.of(
                BRANCH_USER_CODE, BRANCH_USER_INFO,
                HQ_USER_CODE, HQ_USER_INFO
        ));

        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result).anyMatch(e -> e.status() == SalesOrderStatus.APPROVED);
        then(loadUserInfoPort).should().loadByUserCodes(argThat(codes -> codes.size() == 2));
    }

    @Test
    void 취소된_발주_CANCELED_이력_포함() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(canceledOrder());
        given(loadUserInfoPort.loadByUserCodes(anyList()))
                .willReturn(Map.of(BRANCH_USER_CODE, BRANCH_USER_INFO));

        List<SalesOrderHistoryEntry> result = service.get(query(UserRole.HQ_MANAGER));

        assertThat(result).anyMatch(e -> e.status() == SalesOrderStatus.CANCELED);
    }

    private GetHqSalesOrderHistoryQuery query(UserRole role) {
        return new GetHqSalesOrderHistoryQuery(SO_CODE, role);
    }

    private SalesOrder requestedOrder() {
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, "WH-HQ-01",
                SalesOrderStatus.REQUESTED, LocalDate.now().plusDays(3), null,
                new SalesOrderCreation(BRANCH_USER_CODE, T1),
                new SalesOrderRequest(BRANCH_USER_CODE, T2),
                null, null, null, null, List.of()
        );
    }

    private SalesOrder approvedOrder() {
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, "WH-HQ-01",
                SalesOrderStatus.APPROVED, LocalDate.now().plusDays(3), null,
                new SalesOrderCreation(BRANCH_USER_CODE, T1),
                new SalesOrderRequest(BRANCH_USER_CODE, T2),
                new SalesOrderApproval(HQ_USER_CODE, T3, T3.atZone(java.time.ZoneOffset.UTC).toLocalDate(), CarrierType.VEHICLE, "INV-001"),
                null, null, null, List.of()
        );
    }

    private SalesOrder canceledOrder() {
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, "WH-HQ-01",
                SalesOrderStatus.CANCELED, LocalDate.now().plusDays(3), null,
                new SalesOrderCreation(BRANCH_USER_CODE, T1),
                new SalesOrderRequest(BRANCH_USER_CODE, T2),
                null, null, null,
                new SalesOrderCancellation(BRANCH_USER_CODE, T3, "재고 확인 후 취소"),
                List.of()
        );
    }
}
