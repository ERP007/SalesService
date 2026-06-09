package com.fallguys.salesservice.adapter.inbound.web;

import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.UserRole;
import org.springframework.security.oauth2.jwt.Jwt;


public class JwtClaimExtractor {
    private JwtClaimExtractor() {}

    public static String extractUserCode(Jwt jwt) {
        String userCode = jwt.getClaimAsString("employee_no");
        if (userCode == null || userCode.isBlank()) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }
        return userCode;
    }

    public static UserRole extractRole(Jwt jwt) {
        try {
            String role = jwt.getClaimAsString("user_role");
            return UserRole.valueOf(role);
        } catch (Exception e) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }
    }

    public static String extractWarehouseCode(Jwt jwt) {
        String warehouseCode = jwt.getClaimAsString("tenancy_code");
        if (warehouseCode == null || warehouseCode.isBlank()) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }
        return warehouseCode;
    }

}
