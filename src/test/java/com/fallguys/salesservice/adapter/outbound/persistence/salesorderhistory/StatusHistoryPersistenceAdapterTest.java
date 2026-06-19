package com.fallguys.salesservice.adapter.outbound.persistence.salesorderhistory;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.ApprovalPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.CancellationPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.CarrierType;
import com.fallguys.salesservice.domain.model.salesorderhistory.DeliveryPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.RejectReasonCategory;
import com.fallguys.salesservice.domain.model.salesorderhistory.RejectionPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import com.fallguys.salesservice.domain.model.salesorderhistory.StatusChangePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatusHistoryPersistenceAdapterTest {

    @Mock
    SalesOrderStatusHistoryJpaDao jpaDao;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private StatusHistoryPersistenceAdapter adapter() {
        return new StatusHistoryPersistenceAdapter(jpaDao, objectMapper);
    }

    // append → load 라운드트립으로 status별 payload 직렬화/역직렬화를 검증한다.
    private void assertRoundTrip(SalesOrderStatus status, StatusChangePayload payload) {
        SalesOrderStatusHistory original = new SalesOrderStatusHistory(
                "SO-2026-06-0001", status, "EMP-1", payload, Instant.parse("2026-06-19T00:00:00Z"));

        ArgumentCaptor<SalesOrderStatusHistoryEntity> captor =
                ArgumentCaptor.forClass(SalesOrderStatusHistoryEntity.class);
        adapter().append(original);
        verify(jpaDao).save(captor.capture());
        SalesOrderStatusHistoryEntity saved = captor.getValue();

        given(jpaDao.findBySoCodeOrderByCreatedAtDesc("SO-2026-06-0001")).willReturn(List.of(saved));
        List<SalesOrderStatusHistory> loaded = adapter().loadBySoCode("SO-2026-06-0001");

        assertThat(loaded).containsExactly(original);
    }

    @Test
    void approved_payload_roundtrip() {
        assertRoundTrip(SalesOrderStatus.APPROVED,
                new ApprovalPayload(LocalDate.of(2026, 6, 20), CarrierType.VEHICLE, "INV-1"));
    }

    @Test
    void rejected_payload_roundtrip() {
        assertRoundTrip(SalesOrderStatus.REJECTED,
                new RejectionPayload(RejectReasonCategory.OUT_OF_STOCK, "재고 없음"));
    }

    @Test
    void delivered_payload_roundtrip() {
        assertRoundTrip(SalesOrderStatus.DELIVERED,
                new DeliveryPayload(LocalDate.of(2026, 6, 21)));
    }

    @Test
    void canceled_payload_roundtrip() {
        assertRoundTrip(SalesOrderStatus.CANCELED, new CancellationPayload("단순 변심"));
    }

    @Test
    void requested_has_no_payload() {
        assertRoundTrip(SalesOrderStatus.REQUESTED, null);
    }

    @Test
    void draft_has_no_payload() {
        assertRoundTrip(SalesOrderStatus.DRAFT, null);
    }

    @Test
    void 최신_상태_이력을_상태별로_단건_조회한다() {
        SalesOrderStatusHistory original = new SalesOrderStatusHistory(
                "SO-2026-06-0001", SalesOrderStatus.APPROVED, "EMP-1",
                new ApprovalPayload(LocalDate.of(2026, 6, 20), CarrierType.VEHICLE, "INV-1"),
                Instant.parse("2026-06-19T00:00:00Z"));
        SalesOrderStatusHistoryEntity entity = SalesOrderStatusHistoryEntity.from(
                original, objectMapper.writeValueAsString(original.payload()));
        given(jpaDao.findFirstBySoCodeAndStatusOrderByCreatedAtDesc(original.soCode(), original.status()))
                .willReturn(Optional.of(entity));

        Optional<SalesOrderStatusHistory> result = adapter()
                .findLatestBySoCodeAndStatus(original.soCode(), original.status());

        assertThat(result).contains(original);
    }
}
