package com.fallguys.salesservice.adapter.outbound.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface SoNumberSequenceJpaDao extends JpaRepository<SoNumberSequenceEntity, LocalDate> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SoNumberSequenceEntity s WHERE s.seqDate = :seqDate")
    Optional<SoNumberSequenceEntity> findByIdWithLock(@Param("seqDate") LocalDate seqDate);
}
