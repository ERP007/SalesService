package com.fallguys.salesservice.adapter.outbound.client;

import com.fallguys.salesservice.adapter.outbound.client.dto.UserBatchRequest;
import com.fallguys.salesservice.adapter.outbound.client.dto.UserBatchResponse;
import com.fallguys.salesservice.application.port.outbound.LoadUserInfoPort;
import com.fallguys.salesservice.application.port.outbound.UserInfo;
import com.fallguys.salesservice.domain.exception.ExternalServiceException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserClientAdapter implements LoadUserInfoPort {

    private static final String BATCH_USER_LIST_PATH = "/internal/users/batch-userList";

    private final RestClient userRestClient;

    /**
     * 사번 목록으로 사용자 정보를 일괄 조회한다.
     *
     * 흐름:
     * 1) POST /internal/users/batch-userList 를 호출한다.
     * 2) 찾은 사용자만 사번을 키로 하는 UserInfo 맵으로 반환한다.
     *    notFoundEmployeeNumbers는 무시 — 조회 enrichment 목적이므로 부분 결과 허용.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖. 실패 시 호출자(서비스)가 롤백.
     *
     * 예외:
     * - HTTP 오류·연결 실패: ExternalServiceException (SO-07-10, 502) — 클라이언트에는 고정 메시지 노출
     */
    @Override
    public Map<String, UserInfo> loadByUserCodes(List<String> userCodes) {
        UserBatchRequest request = new UserBatchRequest(userCodes);

        UserBatchResponse response;
        try {
            response = userRestClient.post()
                    .uri(BATCH_USER_LIST_PATH)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .body(request)
                    .retrieve()
                    .body(UserBatchResponse.class);
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    SalesErrorCode.USER_SERVICE_ERROR.getCode(),
                    SalesErrorCode.USER_SERVICE_ERROR.getDefaultMessage(),
                    e);
        }

        if (response == null || response.users() == null) {
            return Collections.emptyMap();
        }

        return response.users().stream()
                .collect(Collectors.toMap(
                        UserBatchResponse.UserData::employeeNumber,
                        user -> new UserInfo(user.employeeNumber(), user.name(), user.position())
                ));
    }
}
