package com.fallguys.salesservice.application.port.outbound;

import com.fallguys.salesservice.adapter.outbound.client.dto.UserInfoResponse;

import java.util.List;
import java.util.Map;

public interface LoadUserInfoPort {
    Map<String, UserInfoResponse> loadByUserCodes(List<String> userCodes);
}
