package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.inbound.model.SalesOrderHistoryEntry;

import java.time.Instant;
import java.util.List;

public record SalesOrderHistoryResponse(
        String status,
        PersonInfo changedBy,
        Instant changedAt,
        StatusChangeMeta meta
) {
    public static SalesOrderHistoryResponse from(SalesOrderHistoryEntry entry) {
        return new SalesOrderHistoryResponse(
                entry.status().name(),
                PersonInfo.from(entry.changedBy()),
                entry.changedAt(),
                StatusChangeMeta.from(entry.payload())
        );
    }

    public static List<SalesOrderHistoryResponse> listFrom(List<SalesOrderHistoryEntry> entries) {
        return entries.stream().map(SalesOrderHistoryResponse::from).toList();
    }
}
