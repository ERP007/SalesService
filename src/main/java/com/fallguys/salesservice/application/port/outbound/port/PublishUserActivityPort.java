package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.application.port.outbound.model.UserActivity;

/**
 * 사용자 활동을 User 서비스로 비동기 발행한다(감사 best-effort).
 * 발행 실패는 호출자 트랜잭션을 롤백시키지 않는다.
 */
public interface PublishUserActivityPort {
    void publish(UserActivity activity);
}
