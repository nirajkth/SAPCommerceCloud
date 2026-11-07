package com.erp.mock.controller;

import com.erp.mock.config.MockScenarioConfig;
import com.erp.mock.model.ErpOrderRecord;
import com.erp.mock.model.ErpSimulationScenario;
import com.erp.mock.service.MockErpOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mock")
public class MockDashboardController {

    private final MockErpOrderService orderService;
    private final MockScenarioConfig scenarioConfig;

    public MockDashboardController(MockErpOrderService orderService, MockScenarioConfig scenarioConfig) {
        this.orderService = orderService;
        this.scenarioConfig = scenarioConfig;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "SAP S/4HANA OData Mock ERP",
                "activeScenario", scenarioConfig.getDefaultScenario(),
                "csrfEnabled", scenarioConfig.isCsrfEnabled()
        ));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<ErpOrderRecord>> listOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetState() {
        orderService.reset();
        return ResponseEntity.ok(Map.of("message", "Mock ERP state cleared successfully."));
    }

    @PostMapping("/scenario")
    public ResponseEntity<Map<String, Object>> setScenario(@RequestParam("type") ErpSimulationScenario scenario) {
        scenarioConfig.setDefaultScenario(scenario);
        return ResponseEntity.ok(Map.of(
                "message", "Default scenario updated",
                "activeScenario", scenarioConfig.getDefaultScenario()
        ));
    }

    @PostMapping("/csrf")
    public ResponseEntity<Map<String, Object>> toggleCsrf(@RequestParam("enabled") boolean enabled) {
        scenarioConfig.setCsrfEnabled(enabled);
        return ResponseEntity.ok(Map.of(
                "message", "CSRF enforcement updated",
                "csrfEnabled", scenarioConfig.isCsrfEnabled()
        ));
    }
}
