package com.fallguys.salesservice.application.port.outbound.model;

import java.time.Instant;

/**
 * User 서비스로 보낼 사용자 활동 1건.
 *
 * - employeeNo : 활동 수행자 사번. 시스템 작업이면 "SYSTEM".
 * - action     : 활동 종류.
 * - occurredAt : 발생 시각.
 * - title      : 최근 활동 목록 주요 문구(예: 발주번호).
 * - content    : 주요 문구 뒤 상세. 없으면 null.
 * - status     : 활동 목록 배지 문구(예: 발주 요청).
 */
public record UserActivity(
        String employeeNo,
        UserActivityAction action,
        Instant occurredAt,
        String title,
        String content,
        String status
) {}
