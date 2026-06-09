package com.fallguys.salesservice.application.port.outbound;

import java.util.List;
import java.util.Map;

public interface LoadItemPort {
    Map<String, ItemInfo> loadAll(List<String> itemCodes);
}
