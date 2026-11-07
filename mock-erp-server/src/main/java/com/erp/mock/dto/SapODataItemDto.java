package com.erp.mock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class SapODataItemDto {

    @JsonProperty("SalesOrderItem")
    private String salesOrderItem;

    @NotBlank(message = "Material cannot be blank")
    @JsonProperty("Material")
    private String material;

    @NotNull(message = "RequestedQuantity cannot be null")
    @JsonProperty("RequestedQuantity")
    private BigDecimal requestedQuantity;

    @JsonProperty("RequestedQuantityUnit")
    private String requestedQuantityUnit = "EA";

    @JsonProperty("NetAmount")
    private BigDecimal netAmount;

    @JsonProperty("TransactionCurrency")
    private String transactionCurrency;

    public String getSalesOrderItem() {
        return salesOrderItem;
    }

    public void setSalesOrderItem(String salesOrderItem) {
        this.salesOrderItem = salesOrderItem;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(BigDecimal requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public String getRequestedQuantityUnit() {
        return requestedQuantityUnit;
    }

    public void setRequestedQuantityUnit(String requestedQuantityUnit) {
        this.requestedQuantityUnit = requestedQuantityUnit;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public String getTransactionCurrency() {
        return transactionCurrency;
    }

    public void setTransactionCurrency(String transactionCurrency) {
        this.transactionCurrency = transactionCurrency;
    }
}
