# SAP S/4HANA OData Mock ERP Server (`API_SALES_ORDER_SRV`)

A production-grade, standalone Mock ERP microservice built with Spring Boot 3 & Java 17 for testing SAP Commerce Cloud (Hybris) outbound order replication.

## Key Capabilities

1. **SAP S/4HANA OData v2 Protocol Compliance**:
   - Implements the standard `API_SALES_ORDER_SRV` specification (`A_SalesOrder` entity set).
   - Enforces the SAP **CSRF Handshake** (`x-csrf-token: Fetch` with session cookie management).
   - Generates standard SAP 10-digit Sales Order numbers (e.g., `0010004501`).
2. **Idempotency & Deduplication**:
   - Detects repeated submissions via `X-Idempotency-Key` or `PurchaseOrderByCustomer`.
   - Returns the original ERP sales order document without creating duplicate records.
3. **Dynamic Fault & Scenario Simulation**:
   - `SUCCESS`: Normal `201 Created` with generated SAP Sales Order ID.
   - `LATENCY_5S`: Injects 5-second network delay to test Commerce read timeouts.
   - `ERROR_500`: Simulates ERP backend system failure (`RFC_SYSTEM_FAILURE`).
   - `BUSINESS_REJECT`: Simulates business validation errors (`HTTP 400 Bad Request` with SAP error code `V1/382`).
   - `TRANSIENT_FAIL_THEN_SUCCESS`: Returns `503 Service Unavailable` on attempt 1, then succeeds on retry (attempt 2).
   - `INVALID_PAYLOAD`: Returns `422 Unprocessable Entity`.
4. **Live Web Dashboard & Telemetry**:
   - Open **`http://localhost:8089`** in any web browser to view real-time replicated orders, inspect JSON payloads, switch scenarios dynamically, toggle CSRF validation, or reset test data.

---

## Quick Start

### Run with Java JAR
```bash
java -jar target/mock-erp-server-1.0.0-SNAPSHOT.jar
```

### Run with Maven
```bash
mvn spring-boot:run
```

---

## SAP OData API Reference

| Endpoint | Method | Description |
|---|---|---|
| `/sap/opu/odata/sap/API_SALES_ORDER_SRV/` | `GET` | Service Document & CSRF token fetch (`x-csrf-token: Fetch`) |
| `/sap/opu/odata/sap/API_SALES_ORDER_SRV/$metadata` | `GET` | EDMX metadata specification |
| `/sap/opu/odata/sap/API_SALES_ORDER_SRV/A_SalesOrder` | `POST` | Replicate / Create Sales Order |
| `/sap/opu/odata/sap/API_SALES_ORDER_SRV/A_SalesOrder('{id}')` | `GET` | Fetch single Sales Order by ID |
| `/sap/opu/odata/sap/API_SALES_ORDER_SRV/A_SalesOrder` | `GET` | Query all replicated Sales Orders |

### Request Headers
- `x-csrf-token`: Token acquired from `GET /sap/opu/odata/sap/API_SALES_ORDER_SRV/`
- `X-Idempotency-Key`: Unique client transaction key (e.g. `HYB-ORD-1001-KEY`)
- `X-Correlation-ID`: Traceability UUID across Commerce & ERP
- `X-Simulate-Scenario`: Optional per-request scenario override (`SUCCESS`, `ERROR_500`, `BUSINESS_REJECT`, etc.)

---

## Management API

- `GET /api/mock/health`: Health status & active configuration
- `GET /api/mock/orders`: JSON list of all replicated orders
- `POST /api/mock/reset`: Clears in-memory orders and resets counters
- `POST /api/mock/scenario?type={SCENARIO}`: Updates active default simulation scenario
- `POST /api/mock/csrf?enabled=true|false`: Toggles CSRF enforcement
