package com.fallguys.salesservice.application.service;

import com.fallguys.salesservice.application.port.outbound.model.UserActivity;
import com.fallguys.salesservice.application.port.outbound.model.UserActivityAction;
import com.fallguys.salesservice.application.port.outbound.port.PublishUserActivityPort;
import com.fallguys.salesservice.domain.model.ActorRef;
import com.fallguys.salesservice.domain.model.WarehouseRef;
import com.fallguys.salesservice.domain.model.salesorder.SagaStatus;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderCreation;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderline.Priority;
import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserActivityRecorderTest {

    @Mock PublishUserActivityPort publishUserActivityPort;

    @InjectMocks
    UserActivityRecorder recorder;

    private static final String SO_CODE = "SO-2026-06-0001";
    private static final ActorRef ACTOR = ActorRef.of("branch001", "정유진", "지점 담당");
    private static final Instant NOW = Instant.parse("2026-06-24T10:15:30Z");

    private SalesOrder order(SalesOrderStatus status, int lineCount) {
        List<SalesOrderLine> lines = lineCount == 0 ? List.of()
                : List.of(new SalesOrderLine(1L, SO_CODE, "ITEM-01", "엔진오일", "EA", 3, Priority.NORMAL));
        return new SalesOrder(
                SO_CODE, WarehouseRef.of("WH-BRANCH-01", "지점"), WarehouseRef.of("WH-HQ-01", "본사"),
                status, SagaStatus.NONE, null,
                new SalesOrderCreation(ACTOR, NOW), new SalesOrderRequest(ACTOR, NOW), lines, null);
    }

    private UserActivity capture() {
        ArgumentCaptor<UserActivity> captor = ArgumentCaptor.forClass(UserActivity.class);
        then(publishUserActivityPort).should().publish(captor.capture());
        return captor.getValue();
    }

    @Test
    void created_REQUESTED_매핑() {
        recorder.created(order(SalesOrderStatus.REQUESTED, 1), "branch001", NOW);

        UserActivity a = capture();
        assertThat(a.employeeNo()).isEqualTo("branch001");
        assertThat(a.action()).isEqualTo(UserActivityAction.SALES_ORDER_CREATED);
        assertThat(a.occurredAt()).isEqualTo(NOW);
        assertThat(a.title()).isEqualTo(SO_CODE);
        assertThat(a.content()).isEqualTo("발주 라인 1건");
        assertThat(a.status()).isEqualTo("출고대기");
    }

    @Test
    void created_DRAFT_상태라벨() {
        recorder.created(order(SalesOrderStatus.DRAFT, 0), "branch001", NOW);
        assertThat(capture().status()).isEqualTo("임시저장");
    }

    @Test
    void updated_매핑() {
        recorder.updated(order(SalesOrderStatus.DRAFT, 2), "branch001", NOW);

        UserActivity a = capture();
        assertThat(a.action()).isEqualTo(UserActivityAction.SALES_ORDER_UPDATED);
        assertThat(a.status()).isNull();
    }

    @Test
    void statusChanged_매핑() {
        recorder.statusChanged(SO_CODE, SalesOrderStatus.APPROVED, "hq001", NOW);

        UserActivity a = capture();
        assertThat(a.employeeNo()).isEqualTo("hq001");
        assertThat(a.action()).isEqualTo(UserActivityAction.SALES_ORDER_STATUS_CHANGED);
        assertThat(a.title()).isEqualTo(SO_CODE);
        assertThat(a.content()).isNull();
        assertThat(a.status()).isEqualTo("출고");
    }
}
