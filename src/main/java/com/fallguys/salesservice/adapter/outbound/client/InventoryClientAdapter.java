package com.fallguys.salesservice.adapter.outbound.client;

import com.fallguys.salesservice.adapter.outbound.client.dto.ExternalProblemDetail;
import com.fallguys.salesservice.adapter.outbound.client.dto.InventoryInboundLineRequest;
import com.fallguys.salesservice.adapter.outbound.client.dto.InventoryInboundRequest;
import com.fallguys.salesservice.adapter.outbound.client.dto.InventoryOutboundLineRequest;
import com.fallguys.salesservice.adapter.outbound.client.dto.InventoryOutboundRequest;
import com.fallguys.salesservice.adapter.outbound.client.dto.WarehouseResponse;
import com.fallguys.salesservice.application.port.outbound.InboundStockPort;
import com.fallguys.salesservice.application.port.outbound.LoadWarehousePort;
import com.fallguys.salesservice.application.port.outbound.OutboundStockPort;
import com.fallguys.salesservice.application.port.outbound.VerifyWarehousePort;
import com.fallguys.salesservice.application.port.outbound.WarehouseInfo;
import com.fallguys.salesservice.domain.exception.ExternalServiceException;
import com.fallguys.salesservice.domain.exception.InvalidStatusTransitionException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
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
public class InventoryClientAdapter implements InboundStockPort, OutboundStockPort, VerifyWarehousePort, LoadWarehousePort {

    private static final String INBOUND_SOURCE_TYPE = "SO_ARRIVAL";
    private static final String OUTBOUND_SOURCE_TYPE = "SO";
    private static final String INBOUND_PATH = "/internal/inventory/stocks/inbound";
    private static final String OUTBOUND_PATH = "/internal/inventory/stocks/outbound";
    private static final String WAREHOUSE_PATH = "/internal/inventory/warehouses/{code}";

    // inventory 서비스 errorCode 상수 — 변경 시 이 블록만 수정
    private static final String INV_INVALID_PARAMETER = "INVALID_PARAMETER";
    private static final String INV_WAREHOUSE_NOT_FOUND = "WAREHOUSE_NOT_FOUND";
    private static final String INV_ITEM_NOT_FOUND = "ITEM_NOT_FOUND";
    private static final String INV_ALREADY_PROCESSED = "ALREADY_PROCESSED";
    private static final String INV_WAREHOUSE_INACTIVE = "WAREHOUSE_INACTIVE";
    private static final String INV_STOCK_NOT_FOUND = "STOCK_NOT_FOUND";
    private static final String INV_INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    private static final String INV_LOCK_TIMEOUT = "LOCK_TIMEOUT";

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
     * - INVALID_PARAMETER(400): SalesOrderException (SO-011, 400)
     * - WAREHOUSE_NOT_FOUND(404): ResourceNotFoundException (SO-019, 404)
     * - ITEM_NOT_FOUND(404): ResourceNotFoundException (SO-020, 404)
     * - ALREADY_PROCESSED(409): InvalidStatusTransitionException (SO-012, 409)
     * - WAREHOUSE_INACTIVE(422): SalesOrderException (SO-014, 400)
     * - 5xx·연결 실패: ExternalServiceException (ER-502, 502)
     */
    @Override
    public void inbound(SalesOrder order) {
        InventoryInboundRequest request = buildInboundRequest(order);
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
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getDefaultMessage(),
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
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getDefaultMessage(),
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

    /**
     * 발주 승인 시 재고 서비스에 출고 이력을 기록한다.
     *
     * 흐름:
     * 1) SalesOrder에서 soCode·fromWarehouseCode·라인(itemCode·id·requestedQuantity)을 추출해 요청을 구성한다.
     * 2) POST /internal/inventory/stocks/outbound 를 호출한다.
     * 3) 오류 응답 body의 errorCode를 파싱해 도메인 예외로 번역한다.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖. 실패 시 호출자(서비스)가 롤백.
     *
     * 예외:
     * - INVALID_PARAMETER(400): SalesOrderException (SO-013, 400)
     * - WAREHOUSE_NOT_FOUND(404): ResourceNotFoundException (SO-019, 404)
     * - STOCK_NOT_FOUND(404): ResourceNotFoundException (SO-022, 404)
     * - INSUFFICIENT_STOCK(409): SalesOrderException (SO-015, 400)
     * - LOCK_TIMEOUT(409): SalesOrderException (SO-016, 400)
     * - ALREADY_PROCESSED(409): InvalidStatusTransitionException (SO-012, 409)
     * - WAREHOUSE_INACTIVE(422): SalesOrderException (SO-014, 400)
     * - 5xx·연결 실패: ExternalServiceException (ER-502, 502)
     */
    @Override
    public void outbound(SalesOrder order) {
        InventoryOutboundRequest request = buildOutboundRequest(order);
        try {
            inventoryRestClient.post()
                    .uri(OUTBOUND_PATH)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            throw translateOutboundHttpError(e);
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getDefaultMessage(),
                    e);
        }
    }

    private RuntimeException translateOutboundHttpError(HttpStatusCodeException e) {
        String errorCode = parseErrorCode(e);
        if (errorCode != null) {
            return translateOutboundByErrorCode(errorCode, e);
        }
        return translateOutboundByStatus(e);
    }

    private RuntimeException translateOutboundByErrorCode(String errorCode, HttpStatusCodeException cause) {
        return switch (errorCode) {
            case INV_INVALID_PARAMETER -> new SalesOrderException(SalesErrorCode.INVENTORY_OUTBOUND_FAILED);
            case INV_WAREHOUSE_NOT_FOUND -> new ResourceNotFoundException(SalesErrorCode.WAREHOUSE_NOT_FOUND);
            case INV_STOCK_NOT_FOUND -> new ResourceNotFoundException(SalesErrorCode.STOCK_NOT_FOUND);
            case INV_INSUFFICIENT_STOCK -> new SalesOrderException(SalesErrorCode.INSUFFICIENT_STOCK);
            case INV_LOCK_TIMEOUT -> new SalesOrderException(SalesErrorCode.INVENTORY_LOCK_TIMEOUT);
            case INV_ALREADY_PROCESSED -> new InvalidStatusTransitionException(
                    SalesErrorCode.INVENTORY_ALREADY_PROCESSED,
                    SalesErrorCode.INVENTORY_ALREADY_PROCESSED.getDefaultMessage());
            case INV_WAREHOUSE_INACTIVE -> new SalesOrderException(SalesErrorCode.INVENTORY_WAREHOUSE_INACTIVE);
            default -> translateOutboundByStatus(cause);
        };
    }

    private RuntimeException translateOutboundByStatus(HttpStatusCodeException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null || status.is5xxServerError()) {
            return new ExternalServiceException(
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getDefaultMessage(),
                    e);
        }
        return new SalesOrderException(SalesErrorCode.INVENTORY_OUTBOUND_FAILED);
    }

    /**
     * 창고 코드로 활성 여부를 확인한다.
     *
     * 흐름:
     * 1) GET /internal/inventory/warehouses/{code} 를 호출한다.
     * 2) 응답의 active 필드가 false면 SalesOrderException을 던진다.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖. 실패 시 호출자(서비스)가 롤백.
     *
     * 예외:
     * - 창고 미존재 (404): ResourceNotFoundException (SO-019, 404)
     * - active=false: SalesOrderException (SO-004, 400)
     * - 5xx·연결 실패: ExternalServiceException (ER-502, 502)
     */
    @Override
    public void verify(String warehouseCode) {
        WarehouseResponse response = fetchWarehouse(warehouseCode);
        if (!response.active()) {
            throw new SalesOrderException(SalesErrorCode.WAREHOUSE_INACTIVE);
        }
    }

    /**
     * 창고 코드로 창고 정보를 조회한다.
     *
     * 흐름:
     * 1) GET /internal/inventory/warehouses/{code} 를 호출한다.
     * 2) WarehouseInfo(code, name)로 변환해 반환한다.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖.
     *
     * 예외:
     * - 창고 미존재 (404): ResourceNotFoundException (SO-019, 404)
     * - 5xx·연결 실패: ExternalServiceException (ER-502, 502)
     */
    @Override
    public WarehouseInfo load(String warehouseCode) {
        WarehouseResponse response = fetchWarehouse(warehouseCode);
        return new WarehouseInfo(response.code(), response.name());
    }

    private WarehouseResponse fetchWarehouse(String warehouseCode) {
        try {
            WarehouseResponse response = inventoryRestClient.get()
                    .uri(WAREHOUSE_PATH, warehouseCode)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .retrieve()
                    .body(WarehouseResponse.class);
            if (response == null) {
                throw new ExternalServiceException(
                        CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                        CommonErrorCode.EXTERNAL_SERVICE_ERROR.getDefaultMessage(),
                        null);
            }
            return response;
        } catch (HttpStatusCodeException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException(SalesErrorCode.WAREHOUSE_NOT_FOUND);
            }
            throw new ExternalServiceException(
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getDefaultMessage(),
                    e);
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                    CommonErrorCode.EXTERNAL_SERVICE_ERROR.getDefaultMessage(),
                    e);
        }
    }

    private InventoryInboundRequest buildInboundRequest(SalesOrder order) {
        List<InventoryInboundLineRequest> lines = order.getLines().stream()
                .map(line -> new InventoryInboundLineRequest(
                        line.getItemCode(),
                        line.getApprovedQuantity(),
                        line.getId()))
                .toList();
        return new InventoryInboundRequest(INBOUND_SOURCE_TYPE, order.getCode(), order.getToWarehouseCode(), lines);
    }

    private InventoryOutboundRequest buildOutboundRequest(SalesOrder order) {
        List<InventoryOutboundLineRequest> lines = order.getLines().stream()
                .map(line -> new InventoryOutboundLineRequest(
                        line.getItemCode(),
                        line.getRequestedQuantity(),
                        line.getId()))
                .toList();
        return new InventoryOutboundRequest(OUTBOUND_SOURCE_TYPE, order.getCode(), order.getFromWarehouseCode(), lines);
    }
}
