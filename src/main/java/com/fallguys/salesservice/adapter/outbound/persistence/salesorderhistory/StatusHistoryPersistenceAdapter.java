package com.fallguys.salesservice.adapter.outbound.persistence.salesorderhistory;

import com.fallguys.salesservice.application.port.outbound.port.AppendSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.application.port.outbound.port.LoadSalesOrderStatusHistoryPort;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.ApprovalPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.CancellationPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.DeliveryPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.RejectionPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.SalesOrderStatusHistory;
import com.fallguys.salesservice.domain.model.salesorderhistory.StatusChangePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StatusHistoryPersistenceAdapter
        implements AppendSalesOrderStatusHistoryPort, LoadSalesOrderStatusHistoryPort {

    private final SalesOrderStatusHistoryJpaDao jpaDao;
    private final ObjectMapper objectMapper;

    @Override
    public void append(SalesOrderStatusHistory history) {
        jpaDao.save(SalesOrderStatusHistoryEntity.from(history, serialize(history.payload())));
    }

    @Override
    public List<SalesOrderStatusHistory> loadBySoCode(String soCode) {
        return jpaDao.findBySoCodeOrderByCreatedAtDesc(soCode).stream()
                .map(e -> new SalesOrderStatusHistory(
                        e.getSoCode(),
                        e.getStatus(),
                        e.getActorCode(),
                        deserialize(e.getStatus(), e.getPayload()),
                        e.getCreatedAt()))
                .toList();
    }

    private String serialize(StatusChangePayload payload) {
        if (payload == null) return null;
        return objectMapper.writeValueAsString(payload);
    }

    // status가 payload 구체 타입을 결정한다(Jackson 다형성 미사용). 부가 데이터 없는 전환은 null.
    private StatusChangePayload deserialize(SalesOrderStatus status, String json) {
        if (json == null) return null;
        return switch (status) {
            case APPROVED -> objectMapper.readValue(json, ApprovalPayload.class);
            case REJECTED -> objectMapper.readValue(json, RejectionPayload.class);
            case DELIVERED -> objectMapper.readValue(json, DeliveryPayload.class);
            case CANCELED -> objectMapper.readValue(json, CancellationPayload.class);
            case DRAFT, REQUESTED -> null;
        };
    }
}
