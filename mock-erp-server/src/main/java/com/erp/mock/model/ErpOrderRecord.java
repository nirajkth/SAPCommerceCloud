package com.erp.mock.model;

import com.erp.mock.dto.SapODataSalesOrderDto;
import java.time.Instant;

public class ErpOrderRecord {

    private String salesOrderNumber;
    private String purchaseOrderByCustomer;
    private String idempotencyKey;
    private String correlationId;
    private Instant receivedAt;
    private int attemptCount;
    private SapODataSalesOrderDto payload;

    public ErpOrderRecord() {
    }

    public ErpOrderRecord(String salesOrderNumber, String purchaseOrderByCustomer, String idempotencyKey,
                          String correlationId, Instant receivedAt, int attemptCount, SapODataSalesOrderDto payload) {
        this.salesOrderNumber = salesOrderNumber;
        this.purchaseOrderByCustomer = purchaseOrderByCustomer;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.receivedAt = receivedAt;
        this.attemptCount = attemptCount;
        this.payload = payload;
    }

    public String getSalesOrderNumber() {
        return salesOrderNumber;
    }

    public void setSalesOrderNumber(String salesOrderNumber) {
        this.salesOrderNumber = salesOrderNumber;
    }

    public String getPurchaseOrderByCustomer() {
        return purchaseOrderByCustomer;
    }

    public void setPurchaseOrderByCustomer(String purchaseOrderByCustomer) {
        this.purchaseOrderByCustomer = purchaseOrderByCustomer;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public SapODataSalesOrderDto getPayload() {
        return payload;
    }

    public void setPayload(SapODataSalesOrderDto payload) {
        this.payload = payload;
    }
}
