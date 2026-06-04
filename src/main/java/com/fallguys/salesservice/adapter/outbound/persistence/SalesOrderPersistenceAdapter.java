package com.fallguys.salesservice.adapter.outbound.persistence;

import com.fallguys.salesservice.application.port.outbound.GenerateSoCodePort;
import com.fallguys.salesservice.application.port.outbound.SaveSalesOrderPort;
import com.fallguys.salesservice.domain.model.SalesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SalesOrderPersistenceAdapter implements SaveSalesOrderPort, GenerateSoCodePort {

    private final SalesOrderJpaDao salesOrderJpaDao;
    private final SoNumberSequenceJpaDao soNumberSequenceJpaDao;

    @Override
    public SalesOrder save(SalesOrder salesOrder) {
        SalesOrderEntity entity = salesOrderJpaDao.findById(salesOrder.getCode())
                .map(existing -> existing.update(salesOrder))
                .orElseGet(() -> SalesOrderEntity.from(salesOrder));
        return salesOrderJpaDao.save(entity).toDomain();
    }

    /**
     * SO 코드를 채번한다.
     *
     * 흐름:
     * 1) 당월 첫날 키로 시퀀스 행을 비관적 락으로 조회한다.
     * 2) 행이 있으면 lastSeq를 증가, 없으면 1로 신규 생성한다.
     * 3) 저장 후 SO-YYYY-MM-NNNN 형식으로 반환한다.
     *
     * 트랜잭션: 호출 측(서비스)의 쓰기 트랜잭션에 참여한다.
     * 비관적 락으로 동시 채번 시 중복 코드를 방지한다.
     */
    @Override
    public String generate() {
        LocalDate today = LocalDate.now();
        LocalDate monthKey = today.withDayOfMonth(1);

        SoNumberSequenceEntity seq = soNumberSequenceJpaDao.findByIdWithLock(monthKey)
                .map(SoNumberSequenceEntity::increment)
                .orElseGet(() -> SoNumberSequenceEntity.createFirst(monthKey));
        soNumberSequenceJpaDao.save(seq);

        return String.format("SO-%d-%02d-%04d", today.getYear(), today.getMonthValue(), seq.getLastSeq());
    }
}
