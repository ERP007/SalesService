package com.fallguys.salesservice.domain.model.salesorder;

/**
 * 진행 페이지 폴링 결과 구분. saga 진행/성패를 백엔드가 단일 값으로 판단해 노출한다.
 * PENDING이면 클라이언트는 폴링을 계속하고, SUCCESS·FAILED면 중단한다.
 */
public enum ProgressOutcome {
    PENDING,
    SUCCESS,
    FAILED
}
