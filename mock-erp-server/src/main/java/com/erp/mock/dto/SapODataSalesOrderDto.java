package com.erp.mock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SapODataSalesOrderDto {

    @JsonProperty("SalesOrder")
    private String salesOrder;

    @NotBlank(message = "SalesOrderType cannot be blank")
    @JsonProperty("SalesOrderType")
    private String salesOrderType = "OR";

    @NotBlank(message = "SalesOrganization cannot be blank")
    @JsonProperty("SalesOrganization")
    private String salesOrganization;

    @NotBlank(message = "DistributionChannel cannot be blank")
    @JsonProperty("DistributionChannel")
    private String distributionChannel;

    @NotBlank(message = "OrganizationDivision cannot be blank")
    @JsonProperty("OrganizationDivision")
    private String organizationDivision;

    @NotBlank(message = "SoldToParty cannot be blank")
    @JsonProperty("SoldToParty")
    private String soldToParty;

    @NotBlank(message = "PurchaseOrderByCustomer cannot be blank")
    @JsonProperty("PurchaseOrderByCustomer")
    private String purchaseOrderByCustomer;

    @JsonProperty("TransactionCurrency")
    private String transactionCurrency = "USD";

    @JsonProperty("TotalNetAmount")
    private BigDecimal totalNetAmount;

    @JsonProperty("OverallDeliveryStatus")
    private String overallDeliveryStatus = "Not Delivered";

    @JsonProperty("OverallSDProcessStatus")
    private String overallSDProcessStatus = "Open";

    @JsonProperty("__metadata")
    private Map<String, Object> metadata;

    @JsonProperty("to_Item")
    private Object toItem;

    public String getSalesOrder() {
        return salesOrder;
    }

    public void setSalesOrder(String salesOrder) {
        this.salesOrder = salesOrder;
    }

    public String getSalesOrderType() {
        return salesOrderType;
    }

    public void setSalesOrderType(String salesOrderType) {
        this.salesOrderType = salesOrderType;
    }

    public String getSalesOrganization() {
        return salesOrganization;
    }

    public void setSalesOrganization(String salesOrganization) {
        this.salesOrganization = salesOrganization;
    }

    public String getDistributionChannel() {
        return distributionChannel;
    }

    public void setDistributionChannel(String distributionChannel) {
        this.distributionChannel = distributionChannel;
    }

    public String getOrganizationDivision() {
        return organizationDivision;
    }

    public void setOrganizationDivision(String organizationDivision) {
        this.organizationDivision = organizationDivision;
    }

    public String getSoldToParty() {
        return soldToParty;
    }

    public void setSoldToParty(String soldToParty) {
        this.soldToParty = soldToParty;
    }

    public String getPurchaseOrderByCustomer() {
        return purchaseOrderByCustomer;
    }

    public void setPurchaseOrderByCustomer(String purchaseOrderByCustomer) {
        this.purchaseOrderByCustomer = purchaseOrderByCustomer;
    }

    public String getTransactionCurrency() {
        return transactionCurrency;
    }

    public void setTransactionCurrency(String transactionCurrency) {
        this.transactionCurrency = transactionCurrency;
    }

    public BigDecimal getTotalNetAmount() {
        return totalNetAmount;
    }

    public void setTotalNetAmount(BigDecimal totalNetAmount) {
        this.totalNetAmount = totalNetAmount;
    }

    public String getOverallDeliveryStatus() {
        return overallDeliveryStatus;
    }

    public void setOverallDeliveryStatus(String overallDeliveryStatus) {
        this.overallDeliveryStatus = overallDeliveryStatus;
    }

    public String getOverallSDProcessStatus() {
        return overallSDProcessStatus;
    }

    public void setOverallSDProcessStatus(String overallSDProcessStatus) {
        this.overallSDProcessStatus = overallSDProcessStatus;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Object getToItem() {
        return toItem;
    }

    public void setToItem(Object toItem) {
        this.toItem = toItem;
    }
}
