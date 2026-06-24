package com.fallguys.salesservice.adapter.outbound.persistence.salesorderhistory;

import com.fallguys.salesservice.application.port.outbound.port.PendingStatusChangePort;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import com.fallguys.salesservice.domain.model.salesorderhistory.ApprovalPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.DeliveryPayload;
import com.fallguys.salesservice.domain.model.salesorderhistory.PendingStatusChange;
import com.fallguys.salesservice.domain.model.salesorderhistory.StatusChangePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PendingStatusChangePersistenceAdapter implements PendingStatusChangePort {

    private final PendingStatusChangeJpaDao jpaDao;
    private final ObjectMapper objectMapper;

    @Override
    public void save(PendingStatusChange pending) {
        jpaDao.save(PendingStatusChangeEntity.from(pending, serialize(pending.payload())));
    }

    @Override
    public Optional<PendingStatusChange> findBySoCode(String soCode) {
        return jpaDao.findById(soCode)
                .map(e -> e.toDomain(deserialize(e.getStatus(), e.getPayload())));
    }

    @Override
    public void removeBySoCode(String soCode) {
        jpaDao.deleteById(soCode);
    }

    private String serialize(StatusChangePayload payload) {
        if (payload == null) return null;
        return objectMapper.writeValueAsString(payload);
    }

    // saga 대상은 APPROVED·DELIVERED뿐이므로 두 payload만 역직렬화한다.
    private StatusChangePayload deserialize(SalesOrderStatus status, String json) {
        if (json == null) return null;
        return switch (status) {
            case APPROVED -> objectMapper.readValue(json, ApprovalPayload.class);
            case DELIVERED -> objectMapper.readValue(json, DeliveryPayload.class);
            default -> null;
        };
    }
}
