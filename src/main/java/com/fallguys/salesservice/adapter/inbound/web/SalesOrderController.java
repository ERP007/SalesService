package com.fallguys.salesservice.adapter.inbound.web;

import com.fallguys.salesservice.adapter.inbound.web.dto.BranchSalesOrderDetailResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.BranchSalesOrderPageResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.BranchSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.CancelSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.CancelSalesOrderResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.CreateDraftSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.CreateSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.CreateSalesOrderResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.DeliverSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.DeliverSalesOrderResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.BranchSalesOrderKpiResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.HqSalesOrderDetailResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.HqSalesOrderKpiResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.HqSalesOrderPageResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.HqSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.HqSalesOrderSummaryResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.SubmitSalesOrderRequest;
import com.fallguys.salesservice.application.port.inbound.CancelSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.CancelSalesOrderUseCase;
import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderUseCase;
import com.fallguys.salesservice.application.port.inbound.DeliverSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.DeliverSalesOrderUseCase;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderDetailUseCase;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrdersUseCase;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderKpiUseCase;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderDetailUseCase;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderKpiUseCase;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrdersUseCase;
import com.fallguys.salesservice.application.port.inbound.HqSalesOrderDetail;
import com.fallguys.salesservice.application.port.inbound.SalesOrderDetail;
import com.fallguys.salesservice.application.port.inbound.SubmitSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.BranchSalesOrderKpi;
import com.fallguys.salesservice.application.port.outbound.HqSalesOrderKpi;
import com.fallguys.salesservice.application.port.outbound.HqSalesOrderSummaryPage;
import com.fallguys.salesservice.application.port.outbound.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.UserRole;
import jakarta.validation.Valid;
import java.util.List;
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
    private final CancelSalesOrderUseCase cancelSalesOrderUseCase;
    private final DeliverSalesOrderUseCase deliverSalesOrderUseCase;
    private final GetBranchSalesOrderDetailUseCase getBranchSalesOrderDetailUseCase;
    private final GetBranchSalesOrderKpiUseCase getBranchSalesOrderKpiUseCase;
    private final GetHqSalesOrderKpiUseCase getHqSalesOrderKpiUseCase;
    private final GetBranchSalesOrdersUseCase getBranchSalesOrdersUseCase;
    private final GetHqSalesOrdersUseCase getHqSalesOrdersUseCase;
    private final GetHqSalesOrderDetailUseCase getHqSalesOrderDetailUseCase;

    @PostMapping
    public ResponseEntity<CreateSalesOrderResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateSalesOrderRequest request
    ) {
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
        SalesOrder salesOrder = createSalesOrderUseCase.create(request.toCommand(userCode, role, warehouseCode));
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateSalesOrderResponse.from(salesOrder));
    }

    @PostMapping("/drafts")
    public ResponseEntity<CreateSalesOrderResponse> createDraft(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDraftSalesOrderRequest request
    ) {
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
        SalesOrder salesOrder = createSalesOrderUseCase.create(request.toCommand(userCode, role, warehouseCode));
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateSalesOrderResponse.from(salesOrder));
    }

    @PutMapping("/{code}")
    public ResponseEntity<CreateSalesOrderResponse> submit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code,
            @Valid @RequestBody SubmitSalesOrderRequest request
    ) {
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
        SalesOrder salesOrder = submitSalesOrderUseCase.submit(request.toCommand(code, userCode, role, warehouseCode));
        return ResponseEntity.ok(CreateSalesOrderResponse.from(salesOrder));
    }

    @PatchMapping("/{code}/cancel")
    public ResponseEntity<CancelSalesOrderResponse> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code,
            @Valid @RequestBody CancelSalesOrderRequest request
    ) {
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
        SalesOrder salesOrder = cancelSalesOrderUseCase.cancel(
                new CancelSalesOrderCommand(code, userCode, role, warehouseCode, request.reason())
        );
        return ResponseEntity.ok(CancelSalesOrderResponse.from(salesOrder));
    }

    @PatchMapping("/{code}/deliver")
    public ResponseEntity<DeliverSalesOrderResponse> deliver(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code,
            @Valid @RequestBody DeliverSalesOrderRequest request
    ) {
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
        SalesOrder salesOrder = deliverSalesOrderUseCase.deliver(
                new DeliverSalesOrderCommand(code, warehouseCode, userCode, role, request.deliveredDate())
        );
        return ResponseEntity.ok(DeliverSalesOrderResponse.from(salesOrder));
    }

    @GetMapping("/branch/{code}")
    public ResponseEntity<BranchSalesOrderDetailResponse> getBranchOrderDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code
    ) {
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
        SalesOrderDetail detail = getBranchSalesOrderDetailUseCase.get(
                new GetBranchSalesOrderDetailQuery(code, userCode, role, warehouseCode)
        );
        return ResponseEntity.ok(BranchSalesOrderDetailResponse.from(detail));
    }

    @GetMapping("/kpi/branch")
    public ResponseEntity<BranchSalesOrderKpiResponse> getBranchKpi(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
        BranchSalesOrderKpi kpi = getBranchSalesOrderKpiUseCase.getKpi(warehouseCode, role);
        return ResponseEntity.ok(BranchSalesOrderKpiResponse.from(kpi));
    }

    @GetMapping("/kpi/hq")
    public ResponseEntity<HqSalesOrderKpiResponse> getHqKpi(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        HqSalesOrderKpi kpi = getHqSalesOrderKpiUseCase.getKpi(role);
        return ResponseEntity.ok(HqSalesOrderKpiResponse.from(kpi));
    }

    @GetMapping("/hq/{code}")
    public ResponseEntity<HqSalesOrderDetailResponse> getHqOrderDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String code
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        HqSalesOrderDetail detail = getHqSalesOrderDetailUseCase.get(new GetHqSalesOrderDetailQuery(code, role));
        return ResponseEntity.ok(HqSalesOrderDetailResponse.from(detail));
    }

    @GetMapping("/hq")
    public ResponseEntity<HqSalesOrderPageResponse> getHqOrders(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute HqSalesOrderRequest request
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        HqSalesOrderSummaryPage summaryPage = getHqSalesOrdersUseCase.getOrders(request.toQuery(role));
        List<HqSalesOrderSummaryResponse> content = summaryPage.content().stream()
                .map(HqSalesOrderSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(HqSalesOrderPageResponse.from(summaryPage, content));
    }

    @GetMapping("/branch")
    public ResponseEntity<BranchSalesOrderPageResponse> getBranchOrders(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute BranchSalesOrderRequest request
    ) {
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
        SalesOrderSummaryPage summaryPage = getBranchSalesOrdersUseCase.getBranchOrders(request.toQuery(userCode, role, warehouseCode));
        BranchSalesOrderPageResponse response = BranchSalesOrderPageResponse.from(summaryPage);
        return ResponseEntity.ok(response);
    }
}
