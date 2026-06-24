package com.fallguys.salesservice.domain.model;

public enum UserRole {
    BRANCH_MANAGER,
    BRANCH_STAFF,
    HQ_MANAGER,
    HQ_STAFF,
    ADMIN;

    /** 지점 사용자(발주 생성·제출·취소·입고·지점 조회 권한). */
    public boolean isBranchUser() {
        return this == BRANCH_MANAGER || this == BRANCH_STAFF;
    }

    /** 본사 사용자(승인·본사 조회 권한). ADMIN 포함. */
    public boolean isHqUser() {
        return this == ADMIN || this == HQ_MANAGER || this == HQ_STAFF;
    }
}
