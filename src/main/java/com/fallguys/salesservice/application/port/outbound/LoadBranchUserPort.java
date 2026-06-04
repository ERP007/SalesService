package com.fallguys.salesservice.application.port.outbound;

// User 서비스 호출로 사번에 해당하는 지점 창고 코드를 반환한다.
// 사번 미존재 시 ResourceNotFoundException(SO-05-06) 발생
public interface LoadBranchUserPort {
    BranchUserInfo load(String userCode);
}
