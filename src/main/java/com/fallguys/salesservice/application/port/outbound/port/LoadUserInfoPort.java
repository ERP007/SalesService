package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.application.port.outbound.model.UserInfo;

import java.util.List;
import java.util.Map;

public interface LoadUserInfoPort {
    Map<String, UserInfo> loadByUserCodes(List<String> userCodes);
}
