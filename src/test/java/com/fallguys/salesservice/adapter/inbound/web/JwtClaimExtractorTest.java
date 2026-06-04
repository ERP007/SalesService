package com.fallguys.salesservice.adapter.inbound.web;

import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JwtClaimExtractorTest {

    @Test
    void extractUserCode_success() {
        Jwt jwt = jwt().claim("preferred_username", "branch001").build();

        assertThat(JwtClaimExtractor.extractUserCode(jwt)).isEqualTo("branch001");
    }

    @Test
    void extractUserCode_missingClaim_throwsForbiddenException() {
        Jwt jwt = jwt().build();

        assertThatThrownBy(() -> JwtClaimExtractor.extractUserCode(jwt))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void extractRole_success() {
        Jwt jwt = jwtWithRole("BRANCH_MANAGER");

        assertThat(JwtClaimExtractor.extractRole(jwt)).isEqualTo(UserRole.BRANCH_MANAGER);
    }

    @Test
    void extractRole_allRoles_parsedCorrectly() {
        for (UserRole role : UserRole.values()) {
            Jwt jwt = jwtWithRole(role.name());
            assertThat(JwtClaimExtractor.extractRole(jwt)).isEqualTo(role);
        }
    }

    @Test
    void extractRole_invalidRoleName_throwsForbiddenException() {
        Jwt jwt = jwtWithRole("UNKNOWN_ROLE");

        assertThatThrownBy(() -> JwtClaimExtractor.extractRole(jwt))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void extractRole_missingResourceAccess_throwsForbiddenException() {
        Jwt jwt = jwt().build();

        assertThatThrownBy(() -> JwtClaimExtractor.extractRole(jwt))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void extractRole_missingClientEntry_throwsForbiddenException() {
        Jwt jwt = jwt()
                .claim("resource_access", Map.of("other-client", Map.of("roles", List.of("BRANCH_MANAGER"))))
                .build();

        assertThatThrownBy(() -> JwtClaimExtractor.extractRole(jwt))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireAnyOf_allowedRole_noException() {
        Jwt jwt = jwtWithRole("BRANCH_MANAGER");

        assertThatNoException().isThrownBy(() ->
                JwtClaimExtractor.requireAnyOf(jwt, UserRole.BRANCH_MANAGER, UserRole.BRANCH_STAFF));
    }

    @Test
    void requireAnyOf_notAllowedRole_throwsForbiddenException() {
        Jwt jwt = jwtWithRole("HQ_MANAGER");

        assertThatThrownBy(() ->
                JwtClaimExtractor.requireAnyOf(jwt, UserRole.BRANCH_MANAGER, UserRole.BRANCH_STAFF))
                .isInstanceOf(ForbiddenException.class);
    }

    private Jwt.Builder jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }

    private Jwt jwtWithRole(String role) {
        return jwt()
                .claim("resource_access", Map.of("erp-client", Map.of("roles", List.of(role))))
                .build();
    }
}
