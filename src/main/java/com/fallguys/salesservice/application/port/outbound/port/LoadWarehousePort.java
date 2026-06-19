package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.application.port.outbound.model.WarehouseInfo;

// 창고 코드로 창고 정보를 조회한다.
// 미존재 시 ResourceNotFoundException(SO-015) 발생
public interface LoadWarehousePort {
    WarehouseInfo load(String warehouseCode);
}
