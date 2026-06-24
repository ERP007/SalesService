package com.fallguys.salesservice.adapter.outbound.persistence.salesorderhistory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingStatusChangeJpaDao extends JpaRepository<PendingStatusChangeEntity, String> {
}
