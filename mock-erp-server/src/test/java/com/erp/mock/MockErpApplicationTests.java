package com.erp.mock;

import com.erp.mock.dto.SapODataItemDto;
import com.erp.mock.dto.SapODataSalesOrderDto;
import com.erp.mock.model.ErpSimulationScenario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class MockErpApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetState() throws Exception {
        mockMvc.perform(post("/api/mock/reset"))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfHandshakeAndSalesOrderReplicationSuccess() throws Exception {
        // 1. Fetch CSRF Token
        MvcResult csrfResult = mockMvc.perform(get("/sap/opu/odata/sap/API_SALES_ORDER_SRV/")
                        .header("x-csrf-token", "Fetch"))
                .andExpect(status().isOk())
                .andExpect(header().exists("x-csrf-token"))
                .andReturn();

        String csrfToken = csrfResult.getResponse().getHeader("x-csrf-token");

        // 2. Build Sales Order Payload
        SapODataSalesOrderDto orderDto = new SapODataSalesOrderDto();
        orderDto.setSalesOrderType("OR");
        orderDto.setSalesOrganization("1000");
        orderDto.setDistributionChannel("10");
        orderDto.setOrganizationDivision("00");
        orderDto.setSoldToParty("CUST-9901");
        orderDto.setPurchaseOrderByCustomer("00001001");
        orderDto.setTransactionCurrency("USD");
        orderDto.setTotalNetAmount(new BigDecimal("149.99"));

        SapODataItemDto item = new SapODataItemDto();
        item.setSalesOrderItem("10");
        item.setMaterial("SKU-CAM-001");
        item.setRequestedQuantity(new BigDecimal("1"));
        item.setRequestedQuantityUnit("EA");
        item.setNetAmount(new BigDecimal("149.99"));
        orderDto.setToItem(List.of(item));

        // 3. Post to A_SalesOrder with CSRF token
        mockMvc.perform(post("/sap/opu/odata/sap/API_SALES_ORDER_SRV/A_SalesOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-csrf-token", csrfToken)
                        .header("X-Idempotency-Key", "00001001-IDEM-KEY")
                        .header("X-Correlation-ID", "corr-test-123")
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.d.SalesOrder").isNotEmpty())
                .andExpect(jsonPath("$.d.PurchaseOrderByCustomer").value("00001001"))
                .andExpect(jsonPath("$.d.OverallSDProcessStatus").value("Open"));
    }

    @Test
    void testIdempotencyReturnsExistingOrder() throws Exception {
        // Fetch CSRF
        MvcResult csrfResult = mockMvc.perform(get("/sap/opu/odata/sap/API_SALES_ORDER_SRV/")
                        .header("x-csrf-token", "Fetch"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfToken = csrfResult.getResponse().getHeader("x-csrf-token");

        SapODataSalesOrderDto orderDto = new SapODataSalesOrderDto();
        orderDto.setSalesOrganization("1000");
        orderDto.setDistributionChannel("10");
        orderDto.setOrganizationDivision("00");
        orderDto.setSoldToParty("CUST-9901");
        orderDto.setPurchaseOrderByCustomer("00001002");

        // 1st Post
        MvcResult firstResult = mockMvc.perform(post("/sap/opu/odata/sap/API_SALES_ORDER_SRV/A_SalesOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-csrf-token", csrfToken)
                        .header("X-Idempotency-Key", "IDEM-KEY-DUPLICATE")
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String response1 = firstResult.getResponse().getContentAsString();
        String salesOrder1 = objectMapper.readTree(response1).path("d").path("SalesOrder").asText();

        // 2nd Post with same idempotency key
        mockMvc.perform(post("/sap/opu/odata/sap/API_SALES_ORDER_SRV/A_SalesOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-csrf-token", csrfToken)
                        .header("X-Idempotency-Key", "IDEM-KEY-DUPLICATE")
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.d.SalesOrder").value(salesOrder1));
    }

    @Test
    void testSimulatedBusinessRejection() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/sap/opu/odata/sap/API_SALES_ORDER_SRV/")
                        .header("x-csrf-token", "Fetch"))
                .andExpect(status().isOk())
                .andReturn();
        String csrfToken = csrfResult.getResponse().getHeader("x-csrf-token");

        SapODataSalesOrderDto orderDto = new SapODataSalesOrderDto();
        orderDto.setSalesOrganization("1000");
        orderDto.setDistributionChannel("10");
        orderDto.setOrganizationDivision("00");
        orderDto.setSoldToParty("CUST-BLOCKED");
        orderDto.setPurchaseOrderByCustomer("00001003");

        mockMvc.perform(post("/sap/opu/odata/sap/API_SALES_ORDER_SRV/A_SalesOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-csrf-token", csrfToken)
                        .header("X-Simulate-Scenario", "BUSINESS_REJECT")
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("V1/382"));
    }
}
