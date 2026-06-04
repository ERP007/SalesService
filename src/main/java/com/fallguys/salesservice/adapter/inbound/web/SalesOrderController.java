package com.fallguys.salesservice.adapter.inbound.web;

import com.fallguys.salesservice.adapter.inbound.web.dto.CreateDraftSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.CreateSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.CreateSalesOrderResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.SalesOrderKpiResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.SubmitSalesOrderRequest;
import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderUseCase;
import com.fallguys.salesservice.application.port.inbound.GetSalesOrderKpiUseCase;
import com.fallguys.salesservice.application.port.inbound.SubmitSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.SalesOrderKpi;
import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final CreateSalesOrderUseCase createSalesOrderUseCase;
    private final SubmitSalesOrderUseCase submitSalesOrderUseCase;
    private final GetSalesOrderKpiUseCase getSalesOrderKpiUseCase;

    @PostMapping
    public ResponseEntity<CreateSalesOrderResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateSalesOrderRequest request
    ) {
        JwtClaimExtractor.requireAnyOf(jwt, UserRole.BRANCH_MANAGER, UserRole.BRANCH_STAFF);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        SalesOrder salesOrder = createSalesOrderUseCase.create(request.toCommand(userCode));
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateSalesOrderResponse.from(salesOrder));
    }

    @PostMapping("/drafts")
    public ResponseEntity<CreateSalesOrderResponse> createDraft(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDraftSalesOrderRequest request
    ) {
        JwtClaimExtractor.requireAnyOf(jwt, UserRole.BRANCH_MANAGER, UserRole.BRANCH_STAFF);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        SalesOrder salesOrder = createSalesOrderUseCase.create(request.toCommand(userCode));
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateSalesOrderResponse.from(salesOrder));
    }

    @PutMapping("/{code}")
    public ResponseEntity<CreateSalesOrderResponse> submit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code,
            @Valid @RequestBody SubmitSalesOrderRequest request
    ) {
        JwtClaimExtractor.requireAnyOf(jwt, UserRole.BRANCH_MANAGER, UserRole.BRANCH_STAFF);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        SalesOrder salesOrder = submitSalesOrderUseCase.submit(request.toCommand(code, userCode));
        return ResponseEntity.ok(CreateSalesOrderResponse.from(salesOrder));
    }

    @GetMapping("/kpi")
    public ResponseEntity<SalesOrderKpiResponse> getKpi(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String branchCode
    ) {
        JwtClaimExtractor.requireAnyOf(jwt, UserRole.BRANCH_MANAGER, UserRole.BRANCH_STAFF);
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        SalesOrderKpi kpi = getSalesOrderKpiUseCase.getKpi(branchCode, userCode);
        SalesOrderKpiResponse response = SalesOrderKpiResponse.from(kpi);
        return ResponseEntity.ok(response);
    }
}
