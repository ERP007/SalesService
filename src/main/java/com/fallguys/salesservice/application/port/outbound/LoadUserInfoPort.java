package com.fallguys.salesservice.application.port.outbound;

import java.util.List;
import java.util.Map;

public interface LoadUserInfoPort {
    Map<String, UserInfo> loadByUserCodes(List<String> userCodes);
}
