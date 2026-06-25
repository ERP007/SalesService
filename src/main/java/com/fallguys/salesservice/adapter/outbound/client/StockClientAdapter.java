package com.fallguys.salesservice.adapter.outbound.client;

import com.fallguys.salesservice.adapter.outbound.client.dto.StockMovementRequest;
import com.fallguys.salesservice.application.port.outbound.port.SyncInboundStockPort;
import com.fallguys.salesservice.application.port.outbound.port.SyncOutboundStockPort;
import com.fallguys.salesservice.domain.exception.CommonErrorCode;
import com.fallguys.salesservice.domain.exception.ExternalServiceException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import com.fallguys.salesservice.domain.model.salesorder.SalesOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 재고 입고/출고를 동기 REST로 호출하는 어댑터(부하 테스트용 sync 경로).
 * outbox 기반 async 어댑터({@code StockEventMessagingAdapter})와 달리 호출 결과를 즉시 받으며,
 * 실패 시 예외로 호출자(approve/deliver) 트랜잭션을 롤백시킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockClientAdapter implements SyncOutboundStockPort, SyncInboundStockPort {

    private static final String OUTBOUND_PATH = "/internal/inventory/stocks/outbound";
    private static final String INBOUND_PATH = "/internal/inventory/stocks/inbound";

    private final RestClient inventoryRestClient;

    @Override
    public void outbound(SalesOrder order) {
        post(OUTBOUND_PATH, StockMovementRequest.forOutbound(order),
                SalesErrorCode.INVENTORY_OUTBOUND_FAILED);
    }

    @Override
    public void inbound(SalesOrder order) {
        post(INBOUND_PATH, StockMovementRequest.forInbound(order),
                SalesErrorCode.INVENTORY_INBOUND_FAILED);
    }

    /**
     * 재고 서비스에 동기 POST를 보낸다.
     *
     * 예외 번역(CLAUDE.md §10):
     * - 4xx(재고 부족 등 비즈니스 거부): SalesOrderException(SO-011/SO-012, 400)
     * - 5xx·타임아웃·연결 실패(기술적 실패): ExternalServiceException(ER-502, 502)
     * 원본 원인은 cause로 보존하고 클라이언트엔 비노출.
     */
    private void post(String path, StockMovementRequest request, SalesErrorCode businessError) {
        try {
            inventoryRestClient.post()
                    .uri(path)
                    .header("Authorization", "Bearer " + ClientTokenExtractor.extractToken())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new SalesOrderException(businessError, e);
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
