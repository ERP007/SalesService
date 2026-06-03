package com.fallguys.salesservice.application.port.outbound;

// TODO: JWT 파싱 또는 User 서비스 호출 방식 확정 후 구현
// BRANCH_MANAGER, BRANCH_STAFF만 허용. 미인가 시 ForbiddenException(SO-05-03) 발생
public interface CheckUserPermissionPort {
    BranchUserInfo verify(String userId);
}
