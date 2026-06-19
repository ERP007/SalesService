package com.fallguys.salesservice.application.port.outbound.port;

import com.fallguys.salesservice.application.port.outbound.model.ItemInfo;

import java.util.List;
import java.util.Map;

public interface LoadItemPort {
    Map<String, ItemInfo> loadAll(List<String> itemCodes);
}
