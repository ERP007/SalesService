package com.fallguys.salesservice.adapter.outbound.client;

import com.fallguys.salesservice.adapter.outbound.client.dto.ItemBatchRequest;
import com.fallguys.salesservice.adapter.outbound.client.dto.ItemBatchResponse;
import com.fallguys.salesservice.application.port.outbound.ItemInfo;
import com.fallguys.salesservice.application.port.outbound.LoadItemPort;
import com.fallguys.salesservice.domain.exception.ExternalServiceException;
import com.fallguys.salesservice.domain.exception.ResourceNotFoundException;
import com.fallguys.salesservice.domain.exception.SalesErrorCode;
import com.fallguys.salesservice.domain.exception.SalesOrderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class ItemClientAdapter implements LoadItemPort {

    private static final String ITEMS_PATH = "/internal/items/batch";

    private final RestClient itemRestClient;

    /**
     * SKU 목록으로 부품 정보를 일괄 조회한다.
     *
     * 흐름:
     * 1) SKU 목록을 body에 담아 POST /internal/items/batch 호출한다.
     * 2) notFoundSkus가 존재하면 ResourceNotFoundException을 던진다.
     * 3) active=false 부품이 존재하면 SalesOrderException을 던진다.
     * 4) SKU를 키로 하는 ItemInfo 맵을 반환한다.
     *
     * 트랜잭션: 외부 호출이므로 트랜잭션 경계 밖. 실패 시 호출자(서비스)가 롤백.
     *
     * 예외:
     * - 미존재 SKU 포함: ResourceNotFoundException (SO-05-05, 404)
     * - 비활성 부품 포함: SalesOrderException (SO-05-14, 400)
     * - HTTP 오류·연결 실패: ExternalServiceException (SO-07-09, 502) — 클라이언트에는 고정 메시지 노출
     */
    @Override
    public Map<String, ItemInfo> loadAll(List<String> itemCodes) {
        ItemBatchRequest request = new ItemBatchRequest(itemCodes);

        ItemBatchResponse response;
        try {
            response = itemRestClient.post()
                    .uri(ITEMS_PATH)
                    .body(request)
                    .retrieve()
                    .body(ItemBatchResponse.class);
        } catch (RestClientException e) {
            throw new ExternalServiceException(
                    SalesErrorCode.ITEM_SERVICE_ERROR.getCode(),
                    SalesErrorCode.ITEM_SERVICE_ERROR.getDefaultMessage(),
                    e);
        }

        if (response == null) {
            throw new ExternalServiceException(
                    SalesErrorCode.ITEM_SERVICE_ERROR.getCode(),
                    SalesErrorCode.ITEM_SERVICE_ERROR.getDefaultMessage(),
                    null);
        }

        if (response.notFoundSkus() != null && !response.notFoundSkus().isEmpty()) {
            throw new ResourceNotFoundException(SalesErrorCode.ITEM_NOT_FOUND);
        }

        List<String> inactiveSkus = response.items().stream()
                .filter(item -> !item.active())
                .map(ItemBatchResponse.ItemData::sku)
                .toList();
        if (!inactiveSkus.isEmpty()) {
            throw new SalesOrderException(SalesErrorCode.ITEM_INACTIVE);
        }

        return response.items().stream()
                .collect(Collectors.toMap(
                        ItemBatchResponse.ItemData::sku,
                        item -> new ItemInfo(item.sku(), item.name(), item.unit())
                ));
    }
}
