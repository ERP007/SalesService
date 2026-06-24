package com.fallguys.salesservice.adapter.outbound.persistence.salesorder;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:sales-repository;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class SalesOrderJpaDaoTest {

    @Autowired
    SalesOrderJpaDao salesOrderJpaDao;

    @Test
    void declared_queries_are_valid_against_current_entity_mapping() {
        Instant start = Instant.parse("2026-06-01T00:00:00Z");
        Instant end = Instant.parse("2026-06-30T23:59:59Z");
        List<SalesOrderStatus> statuses = List.of(
                SalesOrderStatus.DRAFT,
                SalesOrderStatus.REQUESTED,
                SalesOrderStatus.APPROVED,
                SalesOrderStatus.DELIVERED
        );

        assertThat(salesOrderJpaDao.countGroupByStatus("WH-BRANCH-01")).isEmpty();
        assertThat(salesOrderJpaDao.countAllGroupByStatus()).isEmpty();
        assertThat(salesOrderJpaDao.findBranchOrders(
                "WH-BRANCH-01", null, statuses, start, end, PageRequest.of(0, 10))).isEmpty();
        assertThat(salesOrderJpaDao.findHqOrders(
                null, null, statuses, start, end, PageRequest.of(0, 10))).isEmpty();
    }
}
