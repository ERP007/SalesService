package com.fallguys.salesservice.adapter.inbound.web.dto;

import com.fallguys.salesservice.application.port.inbound.model.SalesOrderProgressView;

/**
 * 진행 페이지 폴링 응답. pending이 true인 동안 클라이언트가 폴링을 계속하고,
 * false가 되면 outcome(SUCCESS/FAILED)으로 결과를 표시한다. progress는 UI 라벨 매핑용.
 */
public record SalesOrderProgressResponse(
        String code,
        String progress,
        boolean pending,
        String outcome,
        String failureReason
) {
    public static SalesOrderProgressResponse from(SalesOrderProgressView view) {
        return new SalesOrderProgressResponse(
                view.code(),
                view.progress().name(),
                view.pending(),
                view.outcome().name(),
                view.failureReason()
        );
    }
}
