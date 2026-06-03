package com.fallguys.salesservice.application.port.outbound;

// TODO: Inventory 서비스 호출 방식 확정 후 구현
// 창고 미존재 시 ResourceNotFoundException(SO-05-04) 발생
public interface VerifyWarehousePort {
    void verify(String warehouseCode);
}
