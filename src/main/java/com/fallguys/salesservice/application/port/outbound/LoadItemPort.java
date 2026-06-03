package com.fallguys.salesservice.application.port.outbound;

import java.util.List;
import java.util.Map;

// TODO: Item 서비스 또는 Inventory 서비스 호출 방식 확정 후 구현
// 미존재 코드 포함 시 ResourceNotFoundException(SO-05-05) 발생
public interface LoadItemPort {
    Map<String, ItemInfo> loadAll(List<String> itemCodes);
}
