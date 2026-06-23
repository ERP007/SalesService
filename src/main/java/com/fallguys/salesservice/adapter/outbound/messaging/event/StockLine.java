package com.fallguys.salesservice.adapter.outbound.messaging.event;

import com.fallguys.salesservice.domain.model.salesorderline.SalesOrderLine;

/**
 * 재고 이벤트 라인. sku=itemCode, sourceLineNo=발주 라인 id.
 */
public record StockLine(
        String sku,
        int quantity,
        Long sourceLineNo
) {
    public static StockLine from(SalesOrderLine line) {
        return new StockLine(line.getItemCode(), line.getQuantity(), line.getId());
    }
}
