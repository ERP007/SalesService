package com.fallguys.salesservice.adapter.inbound.web;

import com.fallguys.salesservice.adapter.inbound.web.dto.ApproveSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.ApproveSalesOrderResponse;
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
import com.fallguys.salesservice.adapter.inbound.web.dto.RejectSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.RejectSalesOrderResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.SalesOrderHistoryResponse;
import com.fallguys.salesservice.adapter.inbound.web.dto.SubmitSalesOrderRequest;
import com.fallguys.salesservice.application.port.inbound.CancelSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.CancelSalesOrderUseCase;
import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderUseCase;
import com.fallguys.salesservice.application.port.inbound.DeliverSalesOrderCommand;
import com.fallguys.salesservice.application.port.inbound.DeliverSalesOrderUseCase;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderDetailUseCase;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderHistoryQuery;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderHistoryUseCase;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrdersUseCase;
import com.fallguys.salesservice.application.port.inbound.GetBranchSalesOrderKpiUseCase;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderDetailQuery;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderDetailUseCase;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderHistoryQuery;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderHistoryUseCase;
import com.fallguys.salesservice.application.port.inbound.ApproveSalesOrderUseCase;
import com.fallguys.salesservice.application.port.inbound.RejectSalesOrderUseCase;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrderKpiUseCase;
import com.fallguys.salesservice.application.port.inbound.GetHqSalesOrdersUseCase;
import com.fallguys.salesservice.application.port.inbound.HqSalesOrderDetail;
import com.fallguys.salesservice.application.port.inbound.SalesOrderDetail;
import com.fallguys.salesservice.application.port.inbound.SalesOrderHistoryEntry;
import com.fallguys.salesservice.application.port.inbound.SubmitSalesOrderUseCase;
import com.fallguys.salesservice.application.port.outbound.BranchSalesOrderKpi;
import com.fallguys.salesservice.application.port.outbound.HqSalesOrderKpi;
import com.fallguys.salesservice.application.port.outbound.HqSalesOrderSummaryPage;
import com.fallguys.salesservice.application.port.outbound.SalesOrderSummaryPage;
import com.fallguys.salesservice.domain.model.SalesOrder;
import com.fallguys.salesservice.domain.model.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "SalesOrder", description = "발주 API")
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
    private final GetBranchSalesOrderHistoryUseCase getBranchSalesOrderHistoryUseCase;
    private final GetHqSalesOrderHistoryUseCase getHqSalesOrderHistoryUseCase;
    private final RejectSalesOrderUseCase rejectSalesOrderUseCase;
    private final ApproveSalesOrderUseCase approveSalesOrderUseCase;

    @Operation(summary = "발주 생성(즉시 제출)", description = "REQUESTED 상태로 발주를 생성한다. BRANCH_MANAGER·BRANCH_STAFF만 허용.")
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

    @Operation(summary = "발주 임시저장", description = "DRAFT 상태로 발주를 생성한다. BRANCH_MANAGER·BRANCH_STAFF만 허용.")
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

    @Operation(summary = "발주 제출", description = "DRAFT 발주를 REQUESTED로 전환한다.")
    @PutMapping("/{code}")
    public ResponseEntity<CreateSalesOrderResponse> submit(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code,
            @Valid @RequestBody SubmitSalesOrderRequest request
    ) {
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
        SalesOrder salesOrder = submitSalesOrderUseCase.submit(request.toCommand(code, userCode, role, warehouseCode));
        return ResponseEntity.ok(CreateSalesOrderResponse.from(salesOrder));
    }

    @Operation(summary = "발주 취소", description = "발주를 CANCELED로 전환한다.")
    @PatchMapping("/{code}/cancel")
    public ResponseEntity<CancelSalesOrderResponse> cancel(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code,
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

    @Operation(summary = "입고 처리", description = "APPROVED 발주를 DELIVERED로 전환하고 재고 입고를 기록한다.")
    @PatchMapping("/{code}/deliver")
    public ResponseEntity<DeliverSalesOrderResponse> deliver(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code,
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

    @Operation(summary = "지점 발주 상세 조회")
    @GetMapping("/branch/{code}")
    public ResponseEntity<BranchSalesOrderDetailResponse> getBranchOrderDetail(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code
    ) {
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
        SalesOrderDetail detail = getBranchSalesOrderDetailUseCase.get(
                new GetBranchSalesOrderDetailQuery(code, userCode, role, warehouseCode)
        );
        return ResponseEntity.ok(BranchSalesOrderDetailResponse.from(detail));
    }

    @Operation(summary = "지점 발주 KPI 조회")
    @GetMapping("/kpi/branch")
    public ResponseEntity<BranchSalesOrderKpiResponse> getBranchKpi(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
        BranchSalesOrderKpi kpi = getBranchSalesOrderKpiUseCase.getKpi(warehouseCode, role);
        return ResponseEntity.ok(BranchSalesOrderKpiResponse.from(kpi));
    }

    @Operation(summary = "본사 발주 KPI 조회")
    @GetMapping("/kpi/hq")
    public ResponseEntity<HqSalesOrderKpiResponse> getHqKpi(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        HqSalesOrderKpi kpi = getHqSalesOrderKpiUseCase.getKpi(role);
        return ResponseEntity.ok(HqSalesOrderKpiResponse.from(kpi));
    }

    @Operation(summary = "본사 발주 상세 조회")
    @GetMapping("/hq/{code}")
    public ResponseEntity<HqSalesOrderDetailResponse> getHqOrderDetail(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        HqSalesOrderDetail detail = getHqSalesOrderDetailUseCase.get(new GetHqSalesOrderDetailQuery(code, role));
        return ResponseEntity.ok(HqSalesOrderDetailResponse.from(detail));
    }

    @Operation(summary = "본사 발주 목록 조회", description = "날짜 범위·상태·창고 필터와 페이지네이션으로 발주 목록을 조회한다.")
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

    @Operation(summary = "발주 승인", description = "REQUESTED 발주를 APPROVED로 전환하고 재고 출고를 기록한다.")
    @PatchMapping("/{code}/approve")
    public ResponseEntity<ApproveSalesOrderResponse> approve(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code,
            @Valid @RequestBody ApproveSalesOrderRequest request
    ) {
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        SalesOrder order = approveSalesOrderUseCase.approve(request.toCommand(code, userCode, role));
        return ResponseEntity.ok(ApproveSalesOrderResponse.from(order));
    }

    @Operation(summary = "발주 반려")
    @PatchMapping("/{code}/reject")
    public ResponseEntity<RejectSalesOrderResponse> reject(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code,
            @Valid @RequestBody RejectSalesOrderRequest request
    ) {
        String userCode = JwtClaimExtractor.extractUserCode(jwt);
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        SalesOrder order = rejectSalesOrderUseCase.reject(request.toCommand(code, userCode, role));
        return ResponseEntity.ok(RejectSalesOrderResponse.from(order));
    }

    @Operation(summary = "발주 이력 조회", description = "발주 상태 변경 이력을 최신순으로 반환한다. 역할에 따라 지점·본사 이력을 조회한다.")
    @GetMapping("/{code}/histories")
    public ResponseEntity<List<SalesOrderHistoryResponse>> getHistories(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "발주 코드") @PathVariable String code
    ) {
        UserRole role = JwtClaimExtractor.extractRole(jwt);
        List<SalesOrderHistoryEntry> entries = switch (role) {
            case BRANCH_MANAGER, BRANCH_STAFF -> {
                String warehouseCode = JwtClaimExtractor.extractWarehouseCode(jwt);
                yield getBranchSalesOrderHistoryUseCase.get(new GetBranchSalesOrderHistoryQuery(code, role, warehouseCode));
            }
            case ADMIN, HQ_MANAGER, HQ_STAFF ->
                    getHqSalesOrderHistoryUseCase.get(new GetHqSalesOrderHistoryQuery(code, role));
        };
        return ResponseEntity.ok(SalesOrderHistoryResponse.listFrom(entries));
    }

    @Operation(summary = "지점 발주 목록 조회", description = "지점 담당자 기준 발주 목록을 필터와 페이지네이션으로 조회한다.")
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
