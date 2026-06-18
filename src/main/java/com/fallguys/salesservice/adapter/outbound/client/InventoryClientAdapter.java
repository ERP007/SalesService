package com.fallguys.salesservice.adapter.outbound.client;

import com.fallguys.salesservice.adapter.outbound.client.dto.InventoryInboundRequest;
import com.fallguys.salesservice.adapter.outbound.client.dto.InventoryOutboundRequest;
import com.fallguys.salesservice.adapter.outbound.client.dto.WarehouseResponse;
import com.fallguys.salesservice.application.port.outbound.InboundStockPort;
import com.fallguys.salesservice.application.port.outbound.LoadWarehousePort;
import com.fallguys.salesservice.application.port.outbound.OutboundStockPort;
import com.fallguys.salesservice.application.port.outbound.VerifyWarehousePort;
import com.fallguys.salesservice.application.port.outbound.WarehouseInfo;
import com.fallguys.salesservice.domain.exception.ExternalServiceException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.SalesOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClientAdapter implements InboundStockPort, OutboundStockPort, VerifyWarehousePort, LoadWarehousePort {

    private static final String INBOUND_PATH = "/internal/inventory/stocks/inbound";
    private static final String OUTBOUND_PATH = "/internal/inventory/stocks/outbound";
    private static final String WAREHOUSE_PATH = "/internal/inventory/warehouses/{code}";

    private final RestClient inventoryRestClient;

    /**
     * 발주 도착 시 재고 서비스에 입고 이력을 기록한다.
     *
     * 흐름:
     * 1) SalesOrder에서 soCode·toWarehouseCode·라인(itemCode·id·approvedQuantity)을 추출해 요청을 구성한다.
     * 2) POST /internal/inventory/stocks/inbound 를 호출한다.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖. 실패 시 호출자(서비스)가 롤백.
     *
     * 예외:
     * - 4xx: SalesOrderException (SO-011, 400)
     * - 5xx·연결 실패: ExternalServiceException (ER-502, 502)
     */
    @Override
    public void inbound(SalesOrder order) {
        InventoryInboundRequest request = InventoryInboundRequest.from(order);
        try {
            inventoryRestClient.post()
                    .uri(INBOUND_PATH)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                throw new SalesOrderException(SalesErrorCode.INVENTORY_INBOUND_FAILED, e);
            }
            throw externalServiceException(e);
        } catch (RestClientException e) {
            throw externalServiceException(e);
        }
    }

    /**
     * 발주 승인 시 재고 서비스에 출고 이력을 기록한다.
     *
     * 흐름:
     * 1) SalesOrder에서 soCode·fromWarehouseCode·라인(itemCode·id·requestedQuantity)을 추출해 요청을 구성한다.
     * 2) POST /internal/inventory/stocks/outbound 를 호출한다.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖. 실패 시 호출자(서비스)가 롤백.
     *
     * 예외:
     * - 4xx: SalesOrderException (SO-012, 400)
     * - 5xx·연결 실패: ExternalServiceException (ER-502, 502)
     */
    @Override
    public void outbound(SalesOrder order) {
        InventoryOutboundRequest request = InventoryOutboundRequest.from(order);
        try {
            inventoryRestClient.post()
                    .uri(OUTBOUND_PATH)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                throw new SalesOrderException(SalesErrorCode.INVENTORY_OUTBOUND_FAILED, e);
            }
            throw externalServiceException(e);
        } catch (RestClientException e) {
            throw externalServiceException(e);
        }
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
     * - 창고 미존재 (404): ResourceNotFoundException (SO-015, 404)
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
     * - 창고 미존재 (404): ResourceNotFoundException (SO-015, 404)
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
                throw externalServiceException(null);
            }
            return response;
        } catch (HttpStatusCodeException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException(SalesErrorCode.WAREHOUSE_NOT_FOUND);
            }
            throw externalServiceException(e);
        } catch (RestClientException e) {
            throw externalServiceException(e);
        }
    }

    private ExternalServiceException externalServiceException(Throwable cause) {
        return new ExternalServiceException(
                CommonErrorCode.EXTERNAL_SERVICE_ERROR.getCode(),
                CommonErrorCode.EXTERNAL_SERVICE_ERROR.getDefaultMessage(),
                cause);
    }
}
