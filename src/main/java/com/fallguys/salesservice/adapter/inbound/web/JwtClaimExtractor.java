package com.fallguys.salesservice.adapter.inbound.web;

import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.model.UserRole;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class JwtClaimExtractor {
    private static final String CLIENT_NAME = "erp-client";

    private JwtClaimExtractor() {}

    public static String extractUserCode(Jwt jwt) {
        String userCode = jwt.getClaimAsString("preferred_username");
        if (userCode == null) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }
        return userCode;
    }

    public static UserRole extractRole(Jwt jwt) {
        try {
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");

            @SuppressWarnings("unchecked")
            Map<String, Object> erpClient = (Map<String, Object>) resourceAccess.get(CLIENT_NAME);

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) erpClient.get("roles");
            return UserRole.valueOf(roles.getFirst());
        } catch (Exception e) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }
    }

    public static String extractWarehouseCode(Jwt jwt) {
        String warehouseCode = jwt.getClaimAsString("warehouse_code");
        if (warehouseCode == null) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }
        return warehouseCode;
    }

    /**
     * JWT에서 역할을 추출하고 허용된 역할인지 검증한다.
     * 미인가 시 ForbiddenException(SO-05-03) 발생.
     */
    public static void requireAnyOf(Jwt jwt, UserRole... allowedRoles) {
        UserRole role = extractRole(jwt);
        Set<UserRole> allowed = Set.of(allowedRoles);
        if (!allowed.contains(role)) {
            throw new ForbiddenException(SalesErrorCode.UNAUTHORIZED);
        }
    }
}
