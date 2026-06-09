package com.fallguys.salesservice.adapter.inbound.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/sales", "/sales-orders"})
class HealthController {
    @GetMapping("/health")
    public String health() {
        return "sales-service ok";
    }
}
