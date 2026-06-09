package com.fallguys.salesservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    /**
     * Swagger "Try it out"이 호출할 외부 베이스 URL.
     * 게이트웨이(/api 접두어) 뒤 배포 환경에서는 OPENAPI_SERVER_URL=https://api.erp007.xyz/api 로 지정한다.
     * 비어 있으면(로컬) springdoc이 요청 기준으로 server url을 자동 생성한다.
     */
    @Value("${OPENAPI_SERVER_URL:}")
    private String serverUrl;

    @Bean
    public OpenAPI salesOpenApi() {
        OpenAPI openApi = new OpenAPI()
                .info(new Info()
                        .title("Sales Service API")
                        .description("지점-본사 간 발주·입고 서비스 API")
                        .version("v0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));

        if (serverUrl != null && !serverUrl.isBlank()) {
            openApi.servers(List.of(new Server().url(serverUrl)));
        }
        return openApi;
    }
}
