package com.fallguys.salesservice.adapter.outbound.client;

import com.fallguys.salesservice.adapter.outbound.client.dto.ExternalProblemDetail;
import com.fallguys.salesservice.adapter.outbound.client.dto.InventoryInboundLineRequest;
import com.fallguys.salesservice.adapter.outbound.client.dto.InventoryInboundRequest;
import com.fallguys.salesservice.application.port.outbound.InboundStockPort;
import com.fallguys.salesservice.domain.exception.ExternalServiceException;
import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.SalesOrder;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClientAdapter implements InboundStockPort {

    private static final String SOURCE_TYPE = "SO_ARRIVAL";
    private static final String INBOUND_PATH = "/internal/inventory/stocks/inbound";

    // inventory 서비스 errorCode 상수 — 변경 시 이 블록만 수정
    private static final String INV_INVALID_PARAMETER = "INVALID_PARAMETER";
    private static final String INV_WAREHOUSE_NOT_FOUND = "WAREHOUSE_NOT_FOUND";
    private static final String INV_ITEM_NOT_FOUND = "ITEM_NOT_FOUND";
    private static final String INV_ALREADY_PROCESSED = "ALREADY_PROCESSED";
    private static final String INV_WAREHOUSE_INACTIVE = "WAREHOUSE_INACTIVE";

    private final RestClient inventoryRestClient;
    private final ObjectMapper objectMapper;

    /**
     * 발주 도착 시 재고 서비스에 입고 이력을 기록한다.
     *
     * 흐름:
     * 1) SalesOrder에서 soCode·toWarehouseCode·라인(itemCode·id·approvedQuantity)을 추출해 요청을 구성한다.
     * 2) POST /internal/inventory/stocks/inbound 를 호출한다.
     * 3) 오류 응답 body의 errorCode를 파싱해 도메인 예외로 번역한다.
     *    파싱 실패 시 HTTP status로 fallback.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖. 실패 시 호출자(서비스)가 롤백.
     *
     * 예외:
     * - INVALID_PARAMETER(400): SalesOrderException (SO-07-01, 400)
     * - WAREHOUSE_NOT_FOUND(404): ResourceNotFoundException (SO-05-04, 404)
     * - ITEM_NOT_FOUND(404): ResourceNotFoundException (SO-05-05, 404)
     * - ALREADY_PROCESSED(409): InvalidStatusTransitionException (SO-07-02, 409)
     * - WAREHOUSE_INACTIVE(422): SalesOrderException (SO-07-03, 400)
     * - 5xx·연결 실패: ExternalServiceException (SO-07-04, 502)
     */
    @Override
    public void inbound(SalesOrder order) {
        InventoryInboundRequest request = buildRequest(order);
        try {
            inventoryRestClient.post()
                    .uri(INBOUND_PATH)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            throw translateHttpError(e);
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    SalesErrorCode.INVENTORY_SERVICE_ERROR.getCode(),
                    SalesErrorCode.INVENTORY_SERVICE_ERROR.getDefaultMessage(),
                    e);
        }
    }

    private RuntimeException translateHttpError(HttpStatusCodeException e) {
        String errorCode = parseErrorCode(e);
        if (errorCode != null) {
            return translateByErrorCode(errorCode, e);
        }
        return translateByStatus(e);
    }

    private RuntimeException translateByErrorCode(String errorCode, HttpStatusCodeException cause) {
        return switch (errorCode) {
            case INV_INVALID_PARAMETER -> new SalesOrderException(SalesErrorCode.INVENTORY_INBOUND_FAILED);
            case INV_WAREHOUSE_NOT_FOUND -> new ResourceNotFoundException(SalesErrorCode.WAREHOUSE_NOT_FOUND);
            case INV_ITEM_NOT_FOUND -> new ResourceNotFoundException(SalesErrorCode.ITEM_NOT_FOUND);
            case INV_ALREADY_PROCESSED -> new InvalidStatusTransitionException(
                    SalesErrorCode.INVENTORY_ALREADY_PROCESSED,
                    SalesErrorCode.INVENTORY_ALREADY_PROCESSED.getDefaultMessage());
            case INV_WAREHOUSE_INACTIVE -> new SalesOrderException(SalesErrorCode.INVENTORY_WAREHOUSE_INACTIVE);
            default -> translateByStatus(cause);
        };
    }

    private RuntimeException translateByStatus(HttpStatusCodeException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null || status.is5xxServerError()) {
            return new ExternalServiceException(
                    SalesErrorCode.INVENTORY_SERVICE_ERROR.getCode(),
                    SalesErrorCode.INVENTORY_SERVICE_ERROR.getDefaultMessage(),
                    e);
        }
        return new SalesOrderException(SalesErrorCode.INVENTORY_INBOUND_FAILED);
    }

    private String parseErrorCode(HttpStatusCodeException e) {
        try {
            ExternalProblemDetail pd = objectMapper.readValue(
                    e.getResponseBodyAsString(), ExternalProblemDetail.class);
            return pd.errorCode();
        } catch (Exception ex) {
            log.warn("inventory 에러 응답 파싱 실패: {}", ex.getMessage());
            return null;
        }
    }

    private InventoryInboundRequest buildRequest(SalesOrder order) {
        List<InventoryInboundLineRequest> lines = order.getLines().stream()
                .map(line -> new InventoryInboundLineRequest(
                        line.getItemCode(),
                        line.getApprovedQuantity(),
                        line.getId()))
                .toList();
        return new InventoryInboundRequest(SOURCE_TYPE, order.getCode(), order.getToWarehouseCode(), lines);
    }
}
