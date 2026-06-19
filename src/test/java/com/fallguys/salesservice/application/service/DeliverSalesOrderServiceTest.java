package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.inbound.command.DeliverSalesOrderCommand;
import com.fallguys.salesservice.application.port.outbound.port.InboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.SaveSalesOrderPort;
import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.ExternalServiceException;
import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.*;
import com.fallguys.salesservice.domain.model.salesorder.*;
import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;
import com.fallguys.salesservice.domain.model.salesorderhistory.DeliveryPayload;
import com.fallguys.salesservice.domain.model.salesorderline.Priority;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliverSalesOrderServiceTest {

    @Mock LoadSalesOrderPort loadSalesOrderPort;
    @Mock SaveSalesOrderPort saveSalesOrderPort;
    @Mock AppendSalesOrderStatusHistoryPort appendHistoryPort;
    @Mock InboundStockPort inboundStockPort;

    @InjectMocks
    DeliverSalesOrderService service;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final String USER_CODE = "branch001";
    private static final String FROM_WAREHOUSE = "WH-BRANCH-01";
    private static final String OTHER_WAREHOUSE = "WH-BRANCH-99";
    private static final Instant APPROVED_AT = Instant.parse("2026-06-01T00:00:00Z");
    private static final LocalDate VALID_DELIVERED_DATE = LocalDate.of(2026, 6, 3);
    private static final LocalDate BEFORE_APPROVED_DATE = LocalDate.of(2026, 5, 31);

    @BeforeEach
    void setUp() {
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(approvedOrder());
        given(saveSalesOrderPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(inboundStockPort).inbound(any());
    }

    @Test
    void MANAGER_도착_확인_성공() {
        SalesOrder result = service.deliver(command(UserRole.BRANCH_MANAGER));

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.DELIVERED);
        assertThat(result.getDelivery()).isNotNull();
        assertThat(result.getDelivery().deliveredBy()).isEqualTo(USER_CODE);
        assertThat(result.getDelivery().deliveredDate()).isEqualTo(VALID_DELIVERED_DATE);
        assertThat(result.getDelivery().deliveredAt()).isNotNull();
    }

    @Test
    void STAFF_도착_확인_성공() {
        SalesOrder result = service.deliver(command(UserRole.BRANCH_STAFF));

        assertThat(result.getStatus()).isEqualTo(SalesOrderStatus.DELIVERED);
    }

    @Test
    void 성공시_라인_deliveredQuantity가_approvedQuantity로_확정됨() {
        SalesOrder result = service.deliver(command(UserRole.BRANCH_MANAGER));

        result.getLines().forEach(line ->
                assertThat(line.getDeliveredQuantity()).isEqualTo(line.getApprovedQuantity())
        );
    }

    @Test
    void 성공시_재고_입고_호출됨() {
        service.deliver(command(UserRole.BRANCH_MANAGER));

        then(inboundStockPort).should().inbound(any());
    }

    @Test
    void 성공시_DELIVERED_상태로_저장됨() {
        service.deliver(command(UserRole.BRANCH_MANAGER));

        then(saveSalesOrderPort).should().save(argThat(o ->
                o.getStatus() == SalesOrderStatus.DELIVERED &&
                o.getDelivery() != null
        ));
        then(appendHistoryPort).should().append(argThat(h ->
                h.status() == SalesOrderStatus.DELIVERED &&
                h.payload() instanceof DeliveryPayload
        ));
    }

    @Test
    void HQ_MANAGER_역할_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.deliver(command(UserRole.HQ_MANAGER)))
                .isInstanceOf(ForbiddenException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void ADMIN_역할_시도시_ForbiddenException() {
        assertThatThrownBy(() -> service.deliver(command(UserRole.ADMIN)))
                .isInstanceOf(ForbiddenException.class);

        then(loadSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void SO_미존재시_ResourceNotFoundException() {
        given(loadSalesOrderPort.load(SO_CODE))
                .willThrow(new ResourceNotFoundException(SalesErrorCode.SO_NOT_FOUND));

        assertThatThrownBy(() -> service.deliver(command(UserRole.BRANCH_MANAGER)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 창고_불일치시_ForbiddenException() {
        DeliverSalesOrderCommand command = new DeliverSalesOrderCommand(
                SO_CODE, OTHER_WAREHOUSE, USER_CODE, UserRole.BRANCH_MANAGER, VALID_DELIVERED_DATE);

        assertThatThrownBy(() -> service.deliver(command))
                .isInstanceOf(ForbiddenException.class);

        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void deliveredDate가_출고일_이전이면_SalesOrderException() {
        DeliverSalesOrderCommand command = new DeliverSalesOrderCommand(
                SO_CODE, FROM_WAREHOUSE, USER_CODE, UserRole.BRANCH_MANAGER, BEFORE_APPROVED_DATE);

        assertThatThrownBy(() -> service.deliver(command))
                .isInstanceOf(SalesOrderException.class)
                .extracting(e -> ((SalesOrderException) e).getCode())
                .isEqualTo(SalesErrorCode.INVALID_DELIVERED_DATE.getCode());

        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void APPROVED_아닌_상태_시도시_InvalidStatusTransitionException() {
        SalesOrder requestedOrder = new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, "WH-HQ-01",
                SalesOrderStatus.REQUESTED, LocalDate.now().plusDays(3), null,
                new SalesOrderCreation(USER_CODE, Instant.now()),
                new SalesOrderRequest(USER_CODE, Instant.now()),
                null, null, null, null, List.of()
        );
        given(loadSalesOrderPort.load(SO_CODE)).willReturn(requestedOrder);

        assertThatThrownBy(() -> service.deliver(command(UserRole.BRANCH_MANAGER)))
                .isInstanceOf(InvalidStatusTransitionException.class);

        then(saveSalesOrderPort).shouldHaveNoInteractions();
    }

    @Test
    void 재고_서비스_실패시_예외_전파되고_저장은_실행됨() {
        // save → inbound 순서이므로 save는 호출된다. 실제 DB 롤백은 @Transactional이 보장(단위테스트 범위 밖).
        willThrow(new ExternalServiceException(
                CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(), "재고 서비스 호출 실패", new RuntimeException()))
                .given(inboundStockPort).inbound(any());

        assertThatThrownBy(() -> service.deliver(command(UserRole.BRANCH_MANAGER)))
                .isInstanceOf(ExternalServiceException.class);

        then(saveSalesOrderPort).should().save(any());
    }

    private DeliverSalesOrderCommand command(UserRole role) {
        return new DeliverSalesOrderCommand(SO_CODE, FROM_WAREHOUSE, USER_CODE, role, VALID_DELIVERED_DATE);
    }

    private SalesOrder approvedOrder() {
        List<SalesOrderLine> lines = List.of(
                new SalesOrderLine(1L, SO_CODE, "HMC-EN-00214", "엔진오일", "EA", 100, 100, null, Priority.NORMAL),
                new SalesOrderLine(2L, SO_CODE, "HMC-BR-01102", "브레이크패드", "EA", 40, 40, null, Priority.NORMAL)
        );
        return new SalesOrder(
                SO_CODE, FROM_WAREHOUSE, "WH-HQ-01",
                SalesOrderStatus.APPROVED, LocalDate.of(2026, 6, 5), null,
                new SalesOrderCreation(USER_CODE, Instant.now()),
                new SalesOrderRequest(USER_CODE, Instant.now()),
                new SalesOrderApproval("hq001", APPROVED_AT, LocalDate.of(2026, 6, 1), CarrierType.VEHICLE, "INV-2026-001"),
                null, null, null, lines
        );
    }
}
