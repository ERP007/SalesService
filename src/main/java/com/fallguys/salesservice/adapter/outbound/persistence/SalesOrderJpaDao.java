package com.fallguys.salesservice.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderJpaDao extends JpaRepository<SalesOrderEntity, String> {
}
