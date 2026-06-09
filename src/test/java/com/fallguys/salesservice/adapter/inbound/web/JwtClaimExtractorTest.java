package com.fallguys.salesservice.adapter.inbound.web;

import com.fallguys.salesservice.domain.exception.ForbiddenException;
import com.fallguys.salesservice.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class JwtClaimExtractorTest {

    @Test
    void extractUserCode_success() {
        Jwt jwt = jwt().claim("employee_no", "branch001").build();

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
    void extractRole_missingClaim_throwsForbiddenException() {
        Jwt jwt = jwt().build();

        assertThatThrownBy(() -> JwtClaimExtractor.extractRole(jwt))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void extractWarehouseCode_success() {
        Jwt jwt = jwt().claim("tenancy_code", "WH-BRANCH-01").build();

        assertThat(JwtClaimExtractor.extractWarehouseCode(jwt)).isEqualTo("WH-BRANCH-01");
    }

    @Test
    void extractWarehouseCode_missingClaim_throwsForbiddenException() {
        Jwt jwt = jwt().build();

        assertThatThrownBy(() -> JwtClaimExtractor.extractWarehouseCode(jwt))
                .isInstanceOf(ForbiddenException.class);
    }

    private Jwt.Builder jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }

    private Jwt jwtWithRole(String role) {
        return jwt().claim("user_role", role).build();
    }
}
