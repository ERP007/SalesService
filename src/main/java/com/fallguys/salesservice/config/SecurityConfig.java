package com.fallguys.salesservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String[] PUBLIC_PATHS = {
            "/sales/health",
            "/sales-orders/health",
            "/actuator/health",
            "/sales/swagger-ui/**",
            "/sales-orders/swagger-ui/**",
            "/sales/swagger-ui.html",
            "/sales-orders/swagger-ui.html",
            "/sales/v3/api-docs/**",
            "/sales-orders/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * Keycloak 공개키(JWK)로 JWT 서명을 검증하는 디코더(운영 기본).
     * withJwkSetUri는 지연 로딩이라 기동 시 네트워크 호출이 없고, 첫 토큰 검증 시 JWKS를 가져온다.
     * (자동 구성에 의존하지 않고 명시적으로 등록해 컨텍스트 구성을 결정적으로 만든다.)
     */
    @Bean
    @Profile("!local")
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/erp/protocol/openid-connect/certs}")
            String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
