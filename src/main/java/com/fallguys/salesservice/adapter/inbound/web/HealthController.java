package com.fallguys.salesservice.adapter.inbound.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales")
class HealthController {
    @GetMapping("/health")
    String health() {
        return "sales-service ok";
    }
}
