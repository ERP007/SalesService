package com.fallguys.salesservice.application.port.outbound.model;

/**
 * User 서비스 활동 이력으로 보내는 sales 활동 종류.
 * 화면 배지·문구는 consumer(user-service)가 action으로 매핑한다.
 */
public enum UserActivityAction {
    SALES_ORDER_CREATED,
    SALES_ORDER_UPDATED,
    SALES_ORDER_STATUS_CHANGED
}
