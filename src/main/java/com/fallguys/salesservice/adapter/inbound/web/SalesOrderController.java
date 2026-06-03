package com.fallguys.salesservice.adapter.inbound.web;

import com.fallguys.salesservice.adapter.inbound.web.dto.CreateDraftSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.CreateSalesOrderRequest;
import com.fallguys.salesservice.adapter.inbound.web.dto.CreateSalesOrderResponse;
import com.fallguys.salesservice.application.port.inbound.CreateSalesOrderUseCase;
import com.fallguys.salesservice.domain.model.SalesOrder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final CreateSalesOrderUseCase createSalesOrderUseCase;

    @PostMapping
    public ResponseEntity<CreateSalesOrderResponse> create(
            // TODO: Gateway가 주입하는 사용자 식별 헤더 키 확정 필요
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateSalesOrderRequest request
    ) {
        SalesOrder salesOrder = createSalesOrderUseCase.create(request.toCommand(userId));
        CreateSalesOrderResponse response = CreateSalesOrderResponse.from(salesOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/drafts")
    public ResponseEntity<CreateSalesOrderResponse> createDraft(
            // TODO: Gateway가 주입하는 사용자 식별 헤더 키 확정 필요
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateDraftSalesOrderRequest request
    ) {
        SalesOrder salesOrder = createSalesOrderUseCase.create(request.toCommand(userId));
        CreateSalesOrderResponse response = CreateSalesOrderResponse.from(salesOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
