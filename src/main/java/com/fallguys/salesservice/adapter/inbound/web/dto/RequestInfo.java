package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.domain.model.salesorder.SalesOrderRequest;

import java.time.Instant;

/**
 * 요청자·요청시각을 묶은 응답. 미요청(DRAFT)이면 null. null 처리를 from() 한 곳으로 모은다.
 */
public record RequestInfo(
        PersonInfo requestedBy,
        Instant requestedAt
) {
    public static RequestInfo from(SalesOrderRequest request) {
        if (request == null) return null;
        return new RequestInfo(PersonInfo.from(request.requestedBy()), request.requestedAt());
    }
}
