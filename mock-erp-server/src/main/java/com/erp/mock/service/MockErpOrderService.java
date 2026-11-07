package com.erp.mock.service;

import com.erp.mock.config.MockScenarioConfig;
import com.erp.mock.dto.SapODataSalesOrderDto;
import com.erp.mock.model.ErpOrderRecord;
import com.erp.mock.model.ErpSimulationScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MockErpOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(MockErpOrderService.class);

    private final MockScenarioConfig scenarioConfig;
    private final AtomicLong salesOrderSequence = new AtomicLong(10004500L);
    private final Map<String, ErpOrderRecord> ordersBySalesOrder = new ConcurrentHashMap<>();
    private final Map<String, String> orderNumberByCustomerPo = new ConcurrentHashMap<>();
    private final Map<String, String> orderNumberByIdempotencyKey = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> customerPoAttemptCounters = new ConcurrentHashMap<>();
    private final Set<String> validCsrfTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public MockErpOrderService(MockScenarioConfig scenarioConfig) {
        this.scenarioConfig = scenarioConfig;
    }

    public String generateCsrfToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        validCsrfTokens.add(token);
        LOG.info("Generated new CSRF token: {}", token);
        return token;
    }

    public boolean isValidCsrfToken(String token) {
        if (!scenarioConfig.isCsrfEnabled()) {
            return true;
        }
        return token != null && validCsrfTokens.contains(token);
    }

    public Optional<SapODataSalesOrderDto> findExistingOrderByKey(String customerPo, String idempotencyKey) {
        if (idempotencyKey != null && orderNumberByIdempotencyKey.containsKey(idempotencyKey)) {
            String existingOrderNo = orderNumberByIdempotencyKey.get(idempotencyKey);
            ErpOrderRecord record = ordersBySalesOrder.get(existingOrderNo);
            if (record != null) {
                return Optional.of(record.getPayload());
            }
        }

        if (customerPo != null && orderNumberByCustomerPo.containsKey(customerPo)) {
            String existingOrderNo = orderNumberByCustomerPo.get(customerPo);
            ErpOrderRecord record = ordersBySalesOrder.get(existingOrderNo);
            if (record != null) {
                return Optional.of(record.getPayload());
            }
        }

        return Optional.empty();
    }

    public int incrementAndGetAttemptCount(String customerPo) {
        return customerPoAttemptCounters.computeIfAbsent(customerPo, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public SapODataSalesOrderDto createSalesOrder(SapODataSalesOrderDto input, String idempotencyKey, String correlationId) {
        String customerPo = input.getPurchaseOrderByCustomer();
        int attempts = incrementAndGetAttemptCount(customerPo);

        String salesOrderNo = String.format("%010d", salesOrderSequence.incrementAndGet());
        input.setSalesOrder(salesOrderNo);
        input.setOverallDeliveryStatus("Not Delivered");
        input.setOverallSDProcessStatus("Open");
        input.setMetadata(Map.of(
                "id", "https://localhost:8089/sap/opu/odata/sap/API_SALES_ORDER_SRV/A_SalesOrder('" + salesOrderNo + "')",
                "uri", "https://localhost:8089/sap/opu/odata/sap/API_SALES_ORDER_SRV/A_SalesOrder('" + salesOrderNo + "')",
                "type", "API_SALES_ORDER_SRV.A_SalesOrderType"
        ));

        ErpOrderRecord record = new ErpOrderRecord(
                salesOrderNo,
                customerPo,
                idempotencyKey,
                correlationId,
                Instant.now(),
                attempts,
                input
        );

        ordersBySalesOrder.put(salesOrderNo, record);
        if (customerPo != null) {
            orderNumberByCustomerPo.put(customerPo, salesOrderNo);
        }
        if (idempotencyKey != null) {
            orderNumberByIdempotencyKey.put(idempotencyKey, salesOrderNo);
        }

        LOG.info("Successfully created SAP Sales Order [{}] for Commerce Order [{}] (Attempt: {})",
                salesOrderNo, customerPo, attempts);

        return input;
    }

    public Optional<SapODataSalesOrderDto> getSalesOrder(String salesOrder) {
        ErpOrderRecord record = ordersBySalesOrder.get(salesOrder);
        return record != null ? Optional.of(record.getPayload()) : Optional.empty();
    }

    public List<ErpOrderRecord> getAllOrders() {
        return new ArrayList<>(ordersBySalesOrder.values());
    }

    public void reset() {
        ordersBySalesOrder.clear();
        orderNumberByCustomerPo.clear();
        orderNumberByIdempotencyKey.clear();
        customerPoAttemptCounters.clear();
        validCsrfTokens.clear();
        salesOrderSequence.set(10004500L);
        LOG.info("Mock ERP service state has been reset.");
    }
}
