package com.fallguys.salesservice.adapter.outbound.client;

import com.fallguys.salesservice.adapter.outbound.client.dto.WarehouseResponse;
import com.fallguys.salesservice.application.port.outbound.port.LoadWarehousePort;
import com.fallguys.salesservice.application.port.outbound.port.VerifyWarehousePort;
import com.fallguys.salesservice.application.port.outbound.model.WarehouseInfo;
import com.fallguys.salesservice.domain.exception.ExternalServiceException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
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
public class InventoryClientAdapter implements VerifyWarehousePort, LoadWarehousePort {

    private static final String WAREHOUSE_PATH = "/internal/inventory/warehouses/{code}";

    private final RestClient inventoryRestClient;

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
