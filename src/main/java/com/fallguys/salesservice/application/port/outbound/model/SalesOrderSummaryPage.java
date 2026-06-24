package com.fallguys.salesservice.application.port.outbound.model;

import java.util.List;

/**
 * 발주 요약 페이지. 지점·본사 목록 공통 구조이며 content 타입만 다르다.
 * hasPrevious·hasNext는 page·totalPages로 유도 가능하므로 보관하지 않고 응답 DTO에서 계산한다.
 */
public record SalesOrderSummaryPage<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
