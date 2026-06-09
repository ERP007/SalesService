package com.fallguys.salesservice.adapter.outbound.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "so_number_sequences")
@Getter
@NoArgsConstructor
public class SoNumberSequenceEntity {
    @Id
    private LocalDate seqDate;

    @Column(nullable = false)
    private int lastSeq;

    public static SoNumberSequenceEntity createFirst(LocalDate seqDate) {
        SoNumberSequenceEntity entity = new SoNumberSequenceEntity();
        entity.seqDate = seqDate;
        entity.lastSeq = 1;
        return entity;
    }

    public SoNumberSequenceEntity increment() {
        this.lastSeq++;
        return this;
    }
}
