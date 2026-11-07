package com.erp.mock.controller;

import com.erp.mock.config.MockScenarioConfig;
import com.erp.mock.dto.SapODataErrorResponse;
import com.erp.mock.dto.SapODataResponseWrapper;
import com.erp.mock.dto.SapODataSalesOrderDto;
import com.erp.mock.model.ErpSimulationScenario;
import com.erp.mock.service.MockErpOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/sap/opu/odata/sap/API_SALES_ORDER_SRV")
public class SapODataSalesOrderController {

    private static final Logger LOG = LoggerFactory.getLogger(SapODataSalesOrderController.class);

    private final MockErpOrderService orderService;
    private final MockScenarioConfig scenarioConfig;

    public SapODataSalesOrderController(MockErpOrderService orderService, MockScenarioConfig scenarioConfig) {
        this.orderService = orderService;
        this.scenarioConfig = scenarioConfig;
    }

    /**
     * SAP OData Service Document & CSRF Handshake endpoint.
     * Clients send header 'x-csrf-token: Fetch' to obtain a valid CSRF token.
     */
    @GetMapping(value = {"", "/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getServiceDocument(
            @RequestHeader(value = "x-csrf-token", required = false) String csrfHeader,
            HttpServletResponse response) {

        LOG.info("Received SAP OData root service request. CSRF header: {}", csrfHeader);

        HttpHeaders headers = new HttpHeaders();
        if ("Fetch".equalsIgnoreCase(csrfHeader)) {
            String newToken = orderService.generateCsrfToken();
            headers.set("x-csrf-token", newToken);
            headers.set(HttpHeaders.SET_COOKIE, "SAP_SESSIONID_MOCK=erp-sess-" + newToken + "; Path=/; HttpOnly");
        }

        Map<String, Object> serviceDocument = Map.of(
                "d", Map.of(
                        "EntitySets", List.of("A_SalesOrder", "A_SalesOrderItem", "A_SalesOrderScheduleLine")
                )
        );

        return new ResponseEntity<>(serviceDocument, headers, HttpStatus.OK);
    }

    /**
     * SAP EDMX Metadata endpoint.
     */
    @GetMapping(value = "/$metadata", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getMetadata() {
        String edmx = """
                <?xml version="1.0" encoding="utf-8"?>
                <edmx:Edmx Version="1.0" xmlns:edmx="http://schemas.microsoft.com/ado/2007/06/edmx">
                    <edmx:DataServices m:DataServiceVersion="2.0" xmlns:m="http://schemas.microsoft.com/ado/2007/08/dataservices/metadata">
                        <Schema Namespace="API_SALES_ORDER_SRV" xml:lang="en" xmlns="http://schemas.microsoft.com/ado/2008/09/edm">
                            <EntityType Name="A_SalesOrderType">
                                <Key><PropertyRef Name="SalesOrder"/></Key>
                                <Property Name="SalesOrder" Type="Edm.String" Nullable="false" MaxLength="10"/>
                                <Property Name="SalesOrderType" Type="Edm.String" Nullable="false" MaxLength="4"/>
                                <Property Name="SalesOrganization" Type="Edm.String" Nullable="false" MaxLength="4"/>
                                <Property Name="DistributionChannel" Type="Edm.String" Nullable="false" MaxLength="2"/>
                                <Property Name="OrganizationDivision" Type="Edm.String" Nullable="false" MaxLength="2"/>
                                <Property Name="SoldToParty" Type="Edm.String" Nullable="false" MaxLength="10"/>
                                <Property Name="PurchaseOrderByCustomer" Type="Edm.String" MaxLength="35"/>
                                <Property Name="TotalNetAmount" Type="Edm.Decimal" Precision="16" Scale="3"/>
                                <Property Name="TransactionCurrency" Type="Edm.String" MaxLength="5"/>
                                <Property Name="OverallDeliveryStatus" Type="Edm.String" MaxLength="1"/>
                                <Property Name="OverallSDProcessStatus" Type="Edm.String" MaxLength="1"/>
                            </EntityType>
                            <EntityContainer Name="API_SALES_ORDER_SRV_Entities" m:IsDefaultEntityContainer="true">
                                <EntitySet Name="A_SalesOrder" EntityType="API_SALES_ORDER_SRV.A_SalesOrderType"/>
                            </EntityContainer>
                        </Schema>
                    </edmx:DataServices>
                </edmx:Edmx>
                """;
        return ResponseEntity.ok(edmx);
    }

    /**
     * SAP OData Create Sales Order (A_SalesOrder)
     */
    @PostMapping(value = "/A_SalesOrder", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createSalesOrder(
            @Valid @RequestBody SapODataSalesOrderDto requestPayload,
            @RequestHeader(value = "x-csrf-token", required = false) String csrfToken,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @RequestHeader(value = "X-Simulate-Scenario", required = false) String headerScenario,
            HttpServletRequest request) {

        LOG.info("Received Sales Order creation for PO: [{}] | IdempotencyKey: [{}] | CorrelationID: [{}]",
                requestPayload.getPurchaseOrderByCustomer(), idempotencyKey, correlationId);

        // 1. CSRF Token Validation
        if (scenarioConfig.isCsrfEnabled() && !orderService.isValidCsrfToken(csrfToken)) {
            LOG.warn("CSRF token validation failed! Given: {}", csrfToken);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new SapODataErrorResponse("CSRF/403", "CSRF token validation failed. Fetch a new token using 'x-csrf-token: Fetch'"));
        }

        // 2. Determine Simulation Scenario
        ErpSimulationScenario scenario = scenarioConfig.getDefaultScenario();
        if (headerScenario != null && !headerScenario.isBlank()) {
            try {
                scenario = ErpSimulationScenario.valueOf(headerScenario.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                LOG.warn("Unknown X-Simulate-Scenario: {}", headerScenario);
            }
        }

        // 3. Execute Scenario Behaviors
        switch (scenario) {
            case LATENCY_5S:
                LOG.info("Simulating 5-second ERP network latency...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                break;

            case ERROR_500:
                LOG.error("Simulating ERP System Crash (HTTP 500)!");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new SapODataErrorResponse("RFC_SYSTEM_FAILURE", "SAP backend connection error / CX_SY_REF_IS_INITIAL"));

            case BUSINESS_REJECT:
                LOG.warn("Simulating Business Validation Rejection (HTTP 400)!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new SapODataErrorResponse("V1/382", "Customer " + requestPayload.getSoldToParty() + " is locked for sales area " +
                                requestPayload.getSalesOrganization() + "/" + requestPayload.getDistributionChannel() + "/" + requestPayload.getOrganizationDivision()));

            case TRANSIENT_FAIL_THEN_SUCCESS:
                int attempt = orderService.incrementAndGetAttemptCount(requestPayload.getPurchaseOrderByCustomer());
                if (attempt == 1) {
                    LOG.warn("Simulating Transient Failure (HTTP 503) on 1st attempt for Order [{}]",
                            requestPayload.getPurchaseOrderByCustomer());
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .header(HttpHeaders.RETRY_AFTER, "2")
                            .body(new SapODataErrorResponse("HTTP_503", "SAP S/4HANA Work Process temporarily overloaded. Please retry."));
                }
                LOG.info("Attempt #{} for Order [{}] - proceeding with successful replication!",
                        attempt, requestPayload.getPurchaseOrderByCustomer());
                break;

            case INVALID_PAYLOAD:
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(new SapODataErrorResponse("CX_SD_VALIDATION", "Unprocessable ERP Payload entity"));

            case SUCCESS:
            default:
                break;
        }

        // 4. Idempotency Check (Duplicate Order Prevention)
        Optional<SapODataSalesOrderDto> existingOrder = orderService.findExistingOrderByKey(
                requestPayload.getPurchaseOrderByCustomer(), idempotencyKey);

        if (existingOrder.isPresent()) {
            LOG.info("Idempotent hit: Order [{}] already replicated as SAP Order [{}]",
                    requestPayload.getPurchaseOrderByCustomer(), existingOrder.get().getSalesOrder());
            return ResponseEntity.ok(new SapODataResponseWrapper<>(existingOrder.get()));
        }

        // 5. Create SAP Sales Order
        SapODataSalesOrderDto createdOrder = orderService.createSalesOrder(requestPayload, idempotencyKey, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SapODataResponseWrapper<>(createdOrder));
    }

    /**
     * Read Sales Order by ID: /sap/opu/odata/sap/API_SALES_ORDER_SRV/A_SalesOrder('10004501')
     */
    @GetMapping(value = "/A_SalesOrder('{salesOrder}')", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getSalesOrder(@PathVariable("salesOrder") String salesOrder) {
        Optional<SapODataSalesOrderDto> orderOpt = orderService.getSalesOrder(salesOrder);
        if (orderOpt.isPresent()) {
            return ResponseEntity.ok(new SapODataResponseWrapper<>(orderOpt.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new SapODataErrorResponse("00/001", "Sales Order " + salesOrder + " not found"));
    }

    /**
     * List all replicated orders
     */
    @GetMapping(value = "/A_SalesOrder", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllSalesOrders() {
        List<SapODataSalesOrderDto> orders = orderService.getAllOrders().stream()
                .map(record -> record.getPayload())
                .toList();

        return ResponseEntity.ok(Map.of("d", Map.of("results", orders)));
    }
}
