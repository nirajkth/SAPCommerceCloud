In your log file (

console-20261014.log
), the following 3 Solr indexing cronjobs are executing every 1 minute on a recurring schedule:

update-electronicsIndex-cronJob
update-powertoolsIndex-cronJob
update-backofficeIndex-CronJob
Each execution triggers index updates, user session switches, and frequent [Slf4jAuditableActionHandler] DB_AUDIT logs. Furthermore, your 

local.properties
 has task.polling.interval.min=0, causing the Task Engine to poll continuously with zero delay.

Here is how you can stop them immediately and permanently:

Option 1: Stop Them Immediately via HAC (No Server Restart Required)
If your server is already running and you want to stop the log spam right away:

A. Via HAC ImpEx Import
Go to HAC (https://localhost:9002) $\rightarrow$ Console $\rightarrow$ ImpEx Import.
Run the following ImpEx to deactivate the triggers for those indexing jobs:
impex
UPDATE Trigger; cronjob(code)[unique=true]; active
; update-electronicsIndex-cronJob ; false
; update-powertoolsIndex-cronJob ; false
; update-backofficeIndex-CronJob ; false
(Tip: If you want to disable all cronjob triggers across your local environment at once, run:)

impex
UPDATE Trigger; active[default=false]; active
; true ; false
B. Via HAC Groovy Script
Go to HAC $\rightarrow$ Console $\rightarrow$ Scripting Languages $\rightarrow$ select Groovy, and run:

groovy
import de.hybris.platform.servicelayer.cronjob.CronJobService
import de.hybris.platform.servicelayer.model.ModelService
def cronJobService = spring.getBean("cronJobService")
def modelService = spring.getBean("modelService")
def jobs = ["update-electronicsIndex-cronJob", "update-powertoolsIndex-cronJob", "update-backofficeIndex-CronJob"]
jobs.each { jobCode ->
    try {
        def cronJob = cronJobService.getCronJob(jobCode)
        cronJob.getTriggers().each { trigger ->
            trigger.setActive(false)
            modelService.save(trigger)
            println "Deactivated trigger for: ${jobCode}"
        }
    } catch (Exception e) {
        println "Error processing ${jobCode}: ${e.message}"
    }
}
Option 2: Stop All CronJobs Automatically on Startup (Recommended for Local Dev)
In a local development environment, running background cronjobs automatically on startup is rarely needed and wastes CPU and RAM.

Open 

i:/COMCL2211/hybris/config/local.properties
.
Add the following property:
properties
# Disable task engine & automatic cronjob trigger executions on startup
task.engine.loadonstartup=false
(Optional but recommended): Check line 51 in 

local.properties
:
properties
task.polling.interval.min=0
Either comment this out or set it back to the default task.polling.interval.min=10. Setting it to 0 causes the task engine to hammer the DB constantly.
NOTE

Setting task.engine.loadonstartup=false will prevent automated triggers from running on startup. You can still trigger any CronJob manually whenever needed via HAC or Backoffice.

Option 3: Via Backoffice (GUI)
Log into Backoffice (https://localhost:9002/backoffice) as an admin.
Navigate to System $\rightarrow$ CronJobs.
Search for:
update-electronicsIndex-cronJob
update-powertoolsIndex-cronJob
update-backofficeIndex-CronJob
For each CronJob, go to the Triggers tab and set Active to False (or delete the trigger).
Option 4: Silence the Audit & Indexer Loggers (If you want to keep them running silently)
If you need the jobs running but want to mute the logs:

In HAC $\rightarrow$ Monitoring $\rightarrow$ Log4J / Loggers:
de.hybris.platform.audit.internal.action.Slf4jAuditableActionHandler $\rightarrow$ set to WARN (silences DB_AUDIT spam)
de.hybris.platform.solrfacetsearch $\rightarrow$ set to WARN
To make it persistent across restarts, add to 

local.properties
:
properties
log4j2.logger.auditaction.name=de.hybris.platform.audit.internal.action.Slf4jAuditableActionHandler
log4j2.logger.auditaction.level=WARN
log4j2.logger.solrindexer.name=de.hybris.platform.solrfacetsearch
log4j2.logger.solrindexer.level=WARN
5:21 PM
Slf4jAuditableActionHandler still info is coming after setting it to warning
8:36 PM
The reason INFO logs are still appearing comes down to two main causes:

Exact Package Name: The full class package is: 👉 de.hybris.platform.audit.actions.impl.Slf4jAuditableActionHandler (Note the actions.impl path. If you configured it without this exact full package, Log4j2 won't match the logger.)

local.properties requires a restart: If you added it to local.properties, Log4j2 does not reload properties on a running server dynamically. It only takes effect after a restart.

Solution 1: Change it Instantly via HAC Groovy (No Restart Needed)
You can force Log4j2 to change the level in memory right now:

Open HAC (https://localhost:9002) $\rightarrow$ Console $\rightarrow$ Scripting Languages.
Select Groovy and run:
groovy
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
def loggerName = "de.hybris.platform.audit.actions.impl.Slf4jAuditableActionHandler"
def pkgName = "de.hybris.platform.audit"
println "Previous Level: " + LogManager.getLogger(loggerName).getLevel()
// Set the specific class and the entire audit package to WARN
Configurator.setLevel(loggerName, Level.WARN)
Configurator.setLevel(pkgName, Level.WARN)
println "New Level for " + loggerName + ": " + LogManager.getLogger(loggerName).getLevel()
println "New Level for " + pkgName + ": " + LogManager.getLogger(pkgName).getLevel()
The output will confirm New Level: WARN, and the DB_AUDIT logs will stop immediately.

Solution 2: Change it via HAC UI
Go to HAC $\rightarrow$ Monitoring $\rightarrow$ Log4J.
Scroll to the Configure log levels section at the bottom.
In the Logger Name field, paste:
text
de.hybris.platform.audit.actions.impl.Slf4jAuditableActionHandler
Set the dropdown to WARN.
Click Change level.
Repeat for the parent package de.hybris.platform.audit to catch any related audit loggers.
Solution 3: Make it Permanent in local.properties
Add the following to 

i:/COMCL2211/hybris/config/local.properties
:

properties
# Mute DB_AUDIT logs from Slf4jAuditableActionHandler
log4j2.logger.auditaction.name=de.hybris.platform.audit.actions.impl.Slf4jAuditableActionHandler
log4j2.logger.auditaction.level=WARN
# Mute all platform audit logs
log4j2.logger.auditall.name=de.hybris.platform.audit
log4j2.logger.auditall.level=WARN
Recommended: Deactivate the Recurring Cronjob Triggers
Even with the audit logs silenced, the 3 Solr indexing cronjobs (update-electronicsIndex-cronJob, update-powertoolsIndex-cronJob, update-backofficeIndex-CronJob) are still actively running in the background every 60 seconds, which will continue to generate other logs and consume CPU.

To stop them from firing in the background entirely, run this in HAC $\rightarrow$ Console $\rightarrow$ ImpEx Import:

impex
UPDATE Trigger; cronjob(code)[unique=true]; active
; update-electronicsIndex-cronJob ; false
; update-powertoolsIndex-cronJob ; false
; update-backofficeIndex-CronJob ; false
8:36 PM


<!-- ERP Lifecycle Status Enum -->
<enumtype generate="true" code="ErpExportStatus" autocreate="true" dynamic="true">
    <value code="NOT_EXPORTED"/>
    <value code="EXPORTED"/>
    <value code="FAILED"/>
    <value code="RETRY"/>
</enumtype>

<!-- Attributes added to Order -->
<itemtype code="Order" autocreate="false" generate="false">
    <attributes>
        <attribute qualifier="erpOrderCode" type="java.lang.String">
            <description>SAP ERP Sales Document Number (e.g. 0010004501)</description>
            <persistence type="property"/>
            <modifiers read="true" write="true" search="true" optional="true"/>
        </attribute>
        <attribute qualifier="erpExportStatus" type="ErpExportStatus">
            <description>Status of ERP Replication</description>
            <persistence type="property"/>
            <modifiers read="true" write="true" search="true" optional="true"/>
        </attribute>
        <attribute qualifier="erpExportDate" type="java.util.Date">
            <description>Date and time when replicated to ERP</description>
            <persistence type="property"/>
            <modifiers read="true" write="true" search="true" optional="true"/>
        </attribute>
        <attribute qualifier="erpExportError" type="java.lang.String">
            <description>Last error message received from ERP</description>
            <persistence type="property"/>
            <modifiers read="true" write="true" search="true" optional="true"/>
        </attribute>
        <attribute qualifier="erpIdempotencyKey" type="java.lang.String">
            <description>Idempotency key sent to ERP</description>
            <persistence type="property"/>
            <modifiers read="true" write="true" search="true" optional="true"/>
        </attribute>
    </attributes>
</itemtype>


    <action id="takePayment" bean="takePaymentAction">
        <transition name="OK" to="sendOrderToErp"/>
        <transition name="NOK" to="sendPaymentFailedNotification"/>
    </action>

    <!-- SAP S/4HANA OData Order Replication Step -->
    <action id="sendOrderToErp" bean="sendOrderToErpAction">
        <transition name="OK" to="splitOrder"/>
        <transition name="NOK" to="error"/>
    </action>

    <action id="splitOrder" bean="splitOrderAction">
        <transition name="OK" to="waitForWarehouseSubprocessEnd"/>
    </action>


    <!-- Populator & Converter -->
<bean id="sapODataOrderPopulator" class="org.training.core.erp.odata.converters.SapODataOrderPopulator"/>

<bean id="sapODataOrderConverter" parent="abstractPopulatingConverter">
    <property name="targetClass" value="org.training.core.erp.odata.dto.SapODataSalesOrderRequest"/>
    <property name="populators">
        <list>
            <ref bean="sapODataOrderPopulator"/>
        </list>
    </property>
</bean>

<!-- Resilient Service -->
<alias name="defaultSapODataErpOrderReplicationService" alias="erpOrderReplicationService"/>
<bean id="defaultSapODataErpOrderReplicationService"
      class="org.training.core.erp.service.impl.DefaultSapODataErpOrderReplicationService">
    <constructor-arg ref="modelService"/>
    <constructor-arg ref="enumerationService"/>
    <constructor-arg ref="sapODataOrderConverter"/>
</bean>


<bean id="sendOrderToErpAction" class="org.training.fulfilmentprocess.actions.order.SendOrderToErpAction" parent="abstractAction">
    <property name="erpOrderReplicationService" ref="erpOrderReplicationService"/>
</bean>

# SAP S/4HANA OData ERP Integration Properties (Configurable for Mock & Production)
erp.odata.base.url=${env.ERP_ODATA_BASE_URL:http://localhost:8089/sap/opu/odata/sap/API_SALES_ORDER_SRV}
erp.odata.username=${env.ERP_ODATA_USERNAME:SAP_COMMERCE_USER}
erp.odata.password=${env.ERP_ODATA_PASSWORD:SecretPassword123}
erp.odata.connect.timeout=5000
erp.odata.read.timeout=10000
erp.odata.retry.maxAttempts=3
erp.odata.csrf.enabled=true
erp.odata.orderType=OR
erp.odata.salesOrg=1000
erp.odata.distChannel=10
erp.odata.division=00


Updated Code
---------------------------------------

<items xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:noNamespaceSchemaLocation="items.xsd">

    <enumtypes>
        <enumtype code="ErpExportStatus" generate="true" autocreate="true" dynamic="true">
            <value code="NOT_EXPORTED"/>
            <value code="EXPORTED"/>
            <value code="FAILED"/>
            <value code="RETRY"/>
        </enumtype>
    </enumtypes>

    <itemtypes>
        <itemtype code="Order" autocreate="false" generate="false">
            <attributes>
                <attribute qualifier="erpOrderCode" type="java.lang.String">
                    <description>SAP S/4HANA Sales Order ID</description>
                    <persistence type="property"/>
                    <modifiers read="true" write="true" search="true" optional="true"/>
                </attribute>
                <attribute qualifier="erpExportStatus" type="ErpExportStatus">
                    <description>ERP replication lifecycle status</description>
                    <persistence type="property"/>
                    <modifiers read="true" write="true" search="true" optional="true"/>
                </attribute>
                <attribute qualifier="erpExportDate" type="java.util.Date">
                    <description>Timestamp of successful ERP replication</description>
                    <persistence type="property"/>
                    <modifiers read="true" write="true" search="true" optional="true"/>
                </attribute>
                <!-- Fixed typo from erpExortError and mapped to LONG_STRING to prevent DB truncation -->
                <attribute qualifier="erpExportError" type="java.lang.String">
                    <description>Detailed error response / payload from ERP</description>
                    <persistence type="property">
                        <columntype>
                            <value>HYBRIS.LONG_STRING</value>
                        </columntype>
                    </persistence>
                    <modifiers read="true" write="true" search="false" optional="true"/>
                </attribute>
                <attribute qualifier="erpIdempotencyKey" type="java.lang.String">
                    <description>Unique key ensuring single delivery to ERP</description>
                    <persistence type="property"/>
                    <modifiers read="true" write="true" search="true" optional="true"/>
                </attribute>
                <attribute qualifier="erpRetryCount" type="java.lang.Integer">
                    <description>Number of replication retry attempts made</description>
                    <persistence type="property"/>
                    <modifiers read="true" write="true" search="false" optional="true"/>
                </attribute>
            </attributes>
        </itemtype>
    </itemtypes>
</items>

package org.training.core.erp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.enumeration.EnumerationService;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.training.core.erp.odata.dto.SapODataErrorResponse;
import org.training.core.erp.odata.dto.SapODataResponseWrapper;
import org.training.core.erp.odata.dto.SapODataSalesOrderRequest;
import org.training.core.erp.odata.dto.SapODataSalesOrderResponse;
import org.training.core.erp.service.ErpOrderReplicationService;
import org.training.core.erp.service.ErpReplicationResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultSapODataErpOrderReplicationService implements ErpOrderReplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSapODataErpOrderReplicationService.class);

    private static final String PROP_BASE_URL = "erp.odata.base.url";
    private static final String PROP_USERNAME = "erp.odata.username";
    private static final String PROP_PASSWORD = "erp.odata.password";
    private static final String PROP_CONNECT_TIMEOUT = "erp.odata.connect.timeout";
    private static final String PROP_READ_TIMEOUT = "erp.odata.read.timeout";
    private static final String PROP_CSRF_ENABLED = "erp.odata.csrf.enabled";

    private final ModelService modelService;
    private final EnumerationService enumerationService;
    private final ConfigurationService configurationService;
    private final Converter<OrderModel, SapODataSalesOrderRequest> erpOrderODataConverter;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Thread-safe CSRF & Session Cache
    private final ReentrantReadWriteLock csrfLock = new ReentrantReadWriteLock();
    private String cachedCsrfToken;
    private List<String> cachedSessionCookies = Collections.emptyList();
    private Instant tokenExpiry = Instant.MIN;
    private static final long CSRF_TTL_SECONDS = 1200; // 20 minutes

    public DefaultSapODataErpOrderReplicationService(final ModelService modelService,
                                                     final EnumerationService enumerationService,
                                                     final ConfigurationService configurationService,
                                                     final Converter<OrderModel, SapODataSalesOrderRequest> erpOrderODataConverter) {
        this.modelService = modelService;
        this.enumerationService = enumerationService;
        this.configurationService = configurationService;
        this.erpOrderODataConverter = erpOrderODataConverter;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = buildPooledRestTemplate();
    }

    @Override
    public ErpReplicationResult replicateOrder(final OrderModel order) {
        if (order == null) {
            return ErpReplicationResult.fatal("Order parameter cannot be null", 400, null);
        }

        final String orderCode = order.getCode();
        LOG.info("Replicating Order [{}] to SAP S/4HANA", orderCode);

        // Ensure Idempotency Key is saved upfront
        String idempotencyKey = (String) getSafeOrderProperty(order, "erpIdempotencyKey");
        if (StringUtils.isBlank(idempotencyKey)) {
            idempotencyKey = "HYB-" + orderCode + "-" + UUID.randomUUID().toString().substring(0, 8);
            setSafeOrderProperty(order, "erpIdempotencyKey", idempotencyKey);
            modelService.save(order);
        }

        final String baseUrl = getConfiguration(PROP_BASE_URL, "http://localhost:8089/sap/opu/odata/sap/API_SALES_ORDER_SRV");
        final boolean csrfEnabled = Boolean.parseBoolean(getConfiguration(PROP_CSRF_ENABLED, "true"));

        // 1. Deduplication reconciliation: Check if order already exists in S/4HANA (prevents duplicate orders)
        final String existingErpOrder = queryExistingErpOrder(baseUrl, orderCode, csrfEnabled);
        if (StringUtils.isNotBlank(existingErpOrder)) {
            LOG.warn("Order [{}] already exists in S/4HANA with ERP ID [{}]. Reconciling and skipping duplicate creation.",
                    orderCode, existingErpOrder);
            updateOrderSuccess(order, existingErpOrder, idempotencyKey);
            return ErpReplicationResult.success(existingErpOrder, idempotencyKey);
        }

        // 2. Prepare Payload
        final SapODataSalesOrderRequest odataPayload = erpOrderODataConverter.convert(order);

        // 3. Execute POST to create Sales Order in S/4HANA
        try {
            ensureValidCsrfToken(baseUrl, csrfEnabled, false);

            final HttpHeaders headers = createHeaders(idempotencyKey, csrfEnabled);
            final HttpEntity<SapODataSalesOrderRequest> requestEntity = new HttpEntity<>(odataPayload, headers);
            final String postUrl = baseUrl.replaceAll("/+$", "") + "/A_SalesOrder";

            final ResponseEntity<String> response = restTemplate.exchange(postUrl, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && StringUtils.isNotBlank(response.getBody())) {
                final SapODataSalesOrderResponse salesOrderResponse = parseSalesOrderResponse(response.getBody());
                final String salesOrderCode = salesOrderResponse != null ? salesOrderResponse.getSalesOrder() : null;

                if (StringUtils.isBlank(salesOrderCode)) {
                    final String err = "ERP returned 201 Created but no SalesOrder ID was found in response payload.";
                    LOG.error("Order [{}] replication failed: {}", orderCode, err);
                    updateOrderFailure(order, ErpReplicationResult.Status.FATAL_ERROR, err, idempotencyKey);
                    return ErpReplicationResult.fatal(err, response.getStatusCode().value(), idempotencyKey);
                }

                updateOrderSuccess(order, salesOrderCode, idempotencyKey);
                LOG.info("Order [{}] replicated successfully. ERP Sales Document: [{}]", orderCode, salesOrderCode);
                return ErpReplicationResult.success(salesOrderCode, idempotencyKey);
            }

            final String unhandledMsg = "Unexpected response from ERP: " + response.getStatusCode();
            updateOrderFailure(order, ErpReplicationResult.Status.RETRY, unhandledMsg, idempotencyKey);
            return ErpReplicationResult.retry(unhandledMsg, response.getStatusCode().value(), idempotencyKey);

        } catch (final HttpStatusCodeException httpEx) {
            final HttpStatus status = HttpStatus.resolve(httpEx.getStatusCode().value());
            final String errorBody = httpEx.getResponseBodyAsString();
            final String parsedError = extractErrorMessage(errorBody);

            LOG.warn("ERP HTTP Error for order [{}]: Status [{}] Body: [{}]", orderCode, status, parsedError);

            // If CSRF token expired on Gateway (403), force refresh and retry once
            if (status == HttpStatus.FORBIDDEN && csrfEnabled) {
                LOG.info("CSRF token expired for order [{}]. Refreshing token and retrying...", orderCode);
                ensureValidCsrfToken(baseUrl, true, true);
                return replicateOrder(order);
            }

            // Transient server errors (500, 502, 503, 504) -> RETRY
            if (status != null && (status.is5xxServerError() || status == HttpStatus.REQUEST_TIMEOUT)) {
                updateOrderFailure(order, ErpReplicationResult.Status.RETRY, parsedError, idempotencyKey);
                return ErpReplicationResult.retry("Transient ERP Error: " + parsedError, status.value(), idempotencyKey);
            }

            // Client/Validation errors (400, 422, etc.) -> FATAL_ERROR (requires business fix)
            updateOrderFailure(order, ErpReplicationResult.Status.FATAL_ERROR, parsedError, idempotencyKey);
            return ErpReplicationResult.fatal("ERP rejected order: " + parsedError, httpEx.getStatusCode().value(), idempotencyKey);

        } catch (final ResourceAccessException connEx) {
            LOG.warn("Network timeout connecting to ERP for order [{}]: {}", orderCode, connEx.getMessage());
            updateOrderFailure(order, ErpReplicationResult.Status.RETRY, connEx.getMessage(), idempotencyKey);
            return ErpReplicationResult.retry("Network timeout: " + connEx.getMessage(), 504, idempotencyKey);

        } catch (final Exception ex) {
            LOG.error("Unhandled error replicating order [{}] to ERP", orderCode, ex);
            updateOrderFailure(order, ErpReplicationResult.Status.FATAL_ERROR, ex.getMessage(), idempotencyKey);
            return ErpReplicationResult.fatal("Internal replication error: " + ex.getMessage(), 500, idempotencyKey);
        }
    }

    /**
     * Reconciles by querying S/4HANA via OData $filter to see if the order was already received.
     */
    protected String queryExistingErpOrder(final String baseUrl, final String orderCode, final boolean csrfEnabled) {
        try {
            final String queryUrl = String.format("%s/A_SalesOrder?$filter=PurchaseOrderByCustomer eq '%s'&$select=SalesOrder&$top=1",
                    baseUrl.replaceAll("/+$", ""), orderCode);

            final HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            addBasicAuth(headers);

            final ResponseEntity<String> response = restTemplate.exchange(queryUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (response.getStatusCode().is2xxSuccessful() && StringUtils.isNotBlank(response.getBody())) {
                final JsonNode root = objectMapper.readTree(response.getBody());
                final JsonNode results = root.path("d").path("results");
                if (results.isArray() && results.size() > 0) {
                    return results.get(0).path("SalesOrder").asText(null);
                }
            }
        } catch (final Exception e) {
            LOG.debug("Could not verify pre-existing ERP order: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Thread-safe CSRF & Session Cookie management.
     */
    private void ensureValidCsrfToken(final String baseUrl, final boolean csrfEnabled, final boolean forceRefresh) {
        if (!csrfEnabled) {
            return;
        }

        csrfLock.readLock().lock();
        try {
            if (!forceRefresh && cachedCsrfToken != null && Instant.now().isBefore(tokenExpiry)) {
                return;
            }
        } finally {
            csrfLock.readLock().unlock();
        }

        csrfLock.writeLock().lock();
        try {
            if (!forceRefresh && cachedCsrfToken != null && Instant.now().isBefore(tokenExpiry)) {
                return;
            }

            final HttpHeaders headers = new HttpHeaders();
            headers.set("x-csrf-token", "Fetch");
            addBasicAuth(headers);

            final String fetchUrl = baseUrl.replaceAll("/+$", "") + "/";
            final ResponseEntity<String> response = restTemplate.exchange(fetchUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            final List<String> tokenHeader = response.getHeaders().get("x-csrf-token");
            if (CollectionUtils.isNotEmpty(tokenHeader)) {
                this.cachedCsrfToken = tokenHeader.get(0);
                this.tokenExpiry = Instant.now().plusSeconds(CSRF_TTL_SECONDS);
            }

            final List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (CollectionUtils.isNotEmpty(setCookies)) {
                this.cachedSessionCookies = new ArrayList<>(setCookies);
            }
        } catch (final Exception ex) {
            LOG.warn("Failed to fetch fresh CSRF token from ERP Gateway: {}", ex.getMessage());
        } finally {
            csrfLock.writeLock().unlock();
        }
    }

    private HttpHeaders createHeaders(final String idempotencyKey, final boolean csrfEnabled) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Idempotency-Key", idempotencyKey);
        headers.set("X-Correlation-ID", UUID.randomUUID().toString());

        addBasicAuth(headers);

        if (csrfEnabled) {
            csrfLock.readLock().lock();
            try {
                if (cachedCsrfToken != null) {
                    headers.set("x-csrf-token", cachedCsrfToken);
                }
                if (CollectionUtils.isNotEmpty(cachedSessionCookies)) {
                    headers.put(HttpHeaders.COOKIE, cachedSessionCookies);
                }
            } finally {
                csrfLock.readLock().unlock();
            }
        }

        return headers;
    }

    private void addBasicAuth(final HttpHeaders headers) {
        final String user = getConfiguration(PROP_USERNAME, "");
        final String pass = getConfiguration(PROP_PASSWORD, "");
        if (StringUtils.isNotBlank(user)) {
            final String auth = user + ":" + pass;
            final byte[] encoded = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + new String(encoded));
        }
    }

    private RestTemplate buildPooledRestTemplate() {
        final int connectTimeout = Integer.parseInt(getConfiguration(PROP_CONNECT_TIMEOUT, "5000"));
        final int readTimeout = Integer.parseInt(getConfiguration(PROP_READ_TIMEOUT, "10000"));

        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(20);

        final CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        return new RestTemplate(factory);
    }

    private void updateOrderSuccess(final OrderModel order, final String salesOrderNumber, final String idempotencyKey) {
        setSafeOrderProperty(order, "erpOrderCode", salesOrderNumber);
        setSafeOrderProperty(order, "erpExportDate", new Date());
        setSafeOrderProperty(order, "erpExportError", null);
        setSafeOrderProperty(order, "erpIdempotencyKey", idempotencyKey);
        setSafeEnumProperty(order, "erpExportStatus", "EXPORTED");
        modelService.save(order);
    }

    private void updateOrderFailure(final OrderModel order, final ErpReplicationResult.Status status,
                                    final String errorMessage, final String idempotencyKey) {
        final String sanitizedError = StringUtils.truncate(errorMessage, 4000);
        setSafeOrderProperty(order, "erpExportError", sanitizedError);
        setSafeOrderProperty(order, "erpIdempotencyKey", idempotencyKey);

        final Integer currentRetries = (Integer) getSafeOrderProperty(order, "erpRetryCount");
        setSafeOrderProperty(order, "erpRetryCount", currentRetries != null ? currentRetries + 1 : 1);

        final String enumVal = status == ErpReplicationResult.Status.RETRY ? "RETRY" : "FAILED";
        setSafeEnumProperty(order, "erpExportStatus", enumVal);
        modelService.save(order);
    }

    private SapODataSalesOrderResponse parseSalesOrderResponse(final String responseJson) {
        try {
            final SapODataResponseWrapper<SapODataSalesOrderResponse> wrapper = objectMapper.readValue(
                    responseJson,
                    objectMapper.getTypeFactory().constructParametricType(SapODataResponseWrapper.class, SapODataSalesOrderResponse.class)
            );
            return wrapper != null ? wrapper.getD() : null;
        } catch (final Exception e) {
            LOG.warn("Failed to parse ERP response: {}", e.getMessage());
            return null;
        }
    }

    private String extractErrorMessage(final String errorJson) {
        if (StringUtils.isBlank(errorJson)) {
            return "No error details returned from ERP";
        }
        try {
            final SapODataErrorResponse err = objectMapper.readValue(errorJson, SapODataErrorResponse.class);
            if (err != null && err.getError() != null && err.getError().getMessage() != null) {
                return err.getError().getMessage().getValue();
            }
        } catch (final Exception ignored) {}
        return StringUtils.truncate(errorJson, 500);
    }

    private String getConfiguration(final String key, final String defaultValue) {
        if (configurationService != null && configurationService.getConfiguration() != null) {
            return configurationService.getConfiguration().getString(key, defaultValue);
        }
        return defaultValue;
    }

    private Object getSafeOrderProperty(final OrderModel order, final String attribute) {
        try {
            return modelService.getAttributeValue(order, attribute);
        } catch (final Exception e) {
            return null;
        }
    }

    private void setSafeOrderProperty(final OrderModel order, final String attribute, final Object value) {
        try {
            modelService.setAttributeValue(order, attribute, value);
        } catch (final Exception e) {
            LOG.debug("Attribute [{}] cannot be set on OrderModel: {}", attribute, e.getMessage());
        }
    }

    private void setSafeEnumProperty(final OrderModel order, final String attribute, final String enumCode) {
        try {
            final Object enumVal = enumerationService.getEnumerationValue("ErpExportStatus", enumCode);
            modelService.setAttributeValue(order, attribute, enumVal);
        } catch (final Exception e) {
            LOG.debug("Could not set enum ErpExportStatus.{}", enumCode);
        }
    }
}

package org.training.core.erp.odata.converters;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import org.apache.commons.lang3.StringUtils;
import org.training.core.erp.odata.dto.SapODataItemRequest;
import org.training.core.erp.odata.dto.SapODataSalesOrderRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class SapODataOrderPopulator implements Populator<OrderModel, SapODataSalesOrderRequest> {

    private final ConfigurationService configurationService;

    public SapODataOrderPopulator(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void populate(final OrderModel source, final SapODataSalesOrderRequest target) {
        if (source == null || target == null) {
            return;
        }

        // Header mapping
        target.setSalesOrderType(getConfig("erp.odata.orderType", "OR"));
        target.setSalesOrganization(getConfig("erp.odata.salesOrg", "1010"));
        target.setDistributionChannel(getConfig("erp.odata.distChannel", "10"));
        target.setOrganizationDivision(getConfig("erp.odata.division", "00"));
        target.setPurchaseOrderByCustomer(source.getCode());

        if (source.getCurrency() != null) {
            target.setTransactionCurrency(source.getCurrency().getIsocode());
        }
        if (source.getTotalPrice() != null) {
            target.setTotalNetAmount(BigDecimal.valueOf(source.getTotalPrice()).setScale(2, RoundingMode.HALF_UP));
        }

        // Customer / Sold-To Party mapping
        target.setSoldToParty(resolveSoldToParty(source));

        // Line Item mapping (Deep Insert via to_Item)
        final List<SapODataItemRequest> items = new ArrayList<>();
        if (source.getEntries() != null) {
            int itemIndex = 10;
            for (final AbstractOrderEntryModel entry : source.getEntries()) {
                final SapODataItemRequest item = new SapODataItemRequest();
                item.setSalesOrderItem(String.valueOf(itemIndex));
                if (entry.getProduct() != null) {
                    item.setMaterial(entry.getProduct().getCode());
                }
                if (entry.getQuantity() != null) {
                    item.setRequestedQuantity(BigDecimal.valueOf(entry.getQuantity()));
                }
                // Map Hybris unit to SAP UoM (e.g., pieces -> PCE / EA)
                item.setRequestedQuantityUnit(mapSapUnit(entry.getUnit() != null ? entry.getUnit().getCode() : "pieces"));

                if (entry.getTotalPrice() != null) {
                    item.setNetAmount(BigDecimal.valueOf(entry.getTotalPrice()).setScale(2, RoundingMode.HALF_UP));
                }
                if (source.getCurrency() != null) {
                    item.setTransactionCurrency(source.getCurrency().getIsocode());
                }

                items.add(item);
                itemIndex += 10;
            }
        }
        target.setToItem(items);
    }

    protected String resolveSoldToParty(final OrderModel order) {
        if (order.getUser() instanceof CustomerModel) {
            final CustomerModel customer = (CustomerModel) order.getUser();
            // Check if customer has an ERP Customer ID attribute
            try {
                final String customerId = (String) customer.getProperty("erpCustomerId");
                if (StringUtils.isNotBlank(customerId)) {
                    return customerId;
                }
            } catch (final Exception ignored) {}
        }
        // Fallback to configured standard B2C generic customer
        return getConfig("erp.odata.defaultSoldToParty", "0001000001");
    }

    protected String mapSapUnit(final String hybrisUnit) {
        if ("pieces".equalsIgnoreCase(hybrisUnit) || "piece".equalsIgnoreCase(hybrisUnit)) {
            return "PCE";
        }
        return "EA";
    }

    private String getConfig(final String key, final String defaultValue) {
        return configurationService != null && configurationService.getConfiguration() != null
                ? configurationService.getConfiguration().getString(key, defaultValue)
                : defaultValue;
    }
}

package org.training.fulfilmentprocess.actions.order;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractAction;
import org.apache.log4j.Logger;
import org.training.core.erp.service.ErpOrderReplicationService;
import org.training.core.erp.service.ErpReplicationResult;

import java.util.HashSet;
import java.util.Set;

public class SendOrderToErpAction extends AbstractAction<OrderProcessModel> {

    private static final Logger LOG = Logger.getLogger(SendOrderToErpAction.class);

    public enum Transition {
        OK, RETRY, NOK;

        public static Set<String> getStringValues() {
            final Set<String> res = new HashSet<>();
            for (final Transition transitions : Transition.values()) {
                res.add(transitions.toString());
            }
            return res;
        }
    }

    private ErpOrderReplicationService erpOrderReplicationService;

    @Override
    public String execute(final OrderProcessModel process) {
        final OrderModel order = process.getOrder();

        if (order == null) {
            LOG.error(String.format("Order is missing in process [%s]. Cannot replicate to ERP.", process.getCode()));
            return Transition.NOK.toString();
        }

        LOG.info(String.format("Executing ERP replication action for order [%s]", order.getCode()));

        try {
            final ErpReplicationResult result = getErpOrderReplicationService().replicateOrder(order);

            if (result != null && result.isSuccess()) {
                LOG.info(String.format("Order [%s] successfully sent to ERP with Sales Order ID [%s]",
                        order.getCode(), result.getErpOrderCode()));
                return Transition.OK.toString();
            }

            if (result != null && result.isRetryable()) {
                LOG.warn(String.format("Order [%s] encountered transient ERP error. Transitioning to RETRY: %s",
                        order.getCode(), result.getMessage()));
                return Transition.RETRY.toString();
            }

            LOG.error(String.format("Order [%s] rejected by ERP with fatal error: %s",
                    order.getCode(), result != null ? result.getMessage() : "Unknown error"));
            return Transition.NOK.toString();

        } catch (final Exception ex) {
            LOG.error(String.format("Unexpected error during ERP order replication for [%s]", order.getCode()), ex);
            return Transition.RETRY.toString();
        }
    }

    @Override
    public Set<String> getTransitions() {
        return Transition.getStringValues();
    }

    public ErpOrderReplicationService getErpOrderReplicationService() {
        return erpOrderReplicationService;
    }

    public void setErpOrderReplicationService(final ErpOrderReplicationService erpOrderReplicationService) {
        this.erpOrderReplicationService = erpOrderReplicationService;
    }
}


<?xml version="1.0" encoding="utf-8"?>
<process xmlns="http://www.hybris.de/xsd/processdefinition"
         start="checkOrder"
         name="order-process"
         processClass="de.hybris.platform.orderprocessing.model.OrderProcessModel">

    <!-- Previous checkout & payment steps -->
    <action id="checkOrder" bean="checkOrderAction">
        <transition name="OK" to="checkAuthorizeOrderPayment"/>
        <transition name="NOK" to="error"/>
    </action>

    <action id="checkAuthorizeOrderPayment" bean="checkAuthorizeOrderPaymentAction">
        <transition name="OK" to="reserveAmount"/>
        <transition name="NOK" to="authorizationFailedNotification"/>
    </action>

    <action id="reserveAmount" bean="reserveOrderAmountAction">
        <transition name="OK" to="checkTransactionReviewStatus"/>
        <transition name="NOK" to="sendPaymentFailedNotification"/>
    </action>

    <action id="checkTransactionReviewStatus" bean="checkTransactionReviewStatusAction">
        <transition name="OK" to="fraudCheck"/>
        <transition name="NOK" to="notifyCustomer"/>
        <transition name="WAIT" to="waitForReviewDecision"/>
    </action>

    <wait id="waitForReviewDecision" then="checkTransactionReviewStatus" prependProcessCode="false">
        <event>${process.order.code}_ReviewDecision</event>
    </wait>

    <action id="fraudCheck" bean="fraudCheckOrderInternalAction">
        <transition name="OK" to="sendOrderPlacedNotification"/>
        <transition name="POTENTIAL" to="manualOrderCheckCSA" />
        <transition name="FRAUD" to="notifyCustomer" />
    </action>

    <action id="manualOrderCheckCSA" bean="prepareOrderForManualCheckAction">
        <transition name="OK" to="waitForManualOrderCheckCSA"/>
    </action>

    <wait id="waitForManualOrderCheckCSA" then="orderManualChecked" prependProcessCode="false">
        <event>${process.code}_CSAOrderVerified</event>
    </wait>

    <action id="orderManualChecked" bean="orderManualCheckedAction">
        <transition name="OK" to="sendOrderPlacedNotification"/>
        <transition name="NOK" to="waitForCleanUp"/>
        <transition name="UNDEFINED" to="waitForManualOrderCheckCSA"/>
    </action>

    <action id="notifyCustomer" bean="notifyCustomerAboutFraudAction">
        <transition name="OK" to="waitForCleanUp"/>
    </action>

    <wait id="waitForCleanUp" then="scheduleForCleanUp" prependProcessCode="false">
        <event>${process.code}_CleanUpEvent</event>
    </wait>

    <action id="scheduleForCleanUp" bean="scheduleForCleanUpAction">
        <transition name="OK" to="cancelOrder"/>
        <transition name="NOK" to="orderManualChecked" />
    </action>

    <action id="sendOrderPlacedNotification" bean="sendOrderPlacedNotificationAction">
        <transition name="OK" to="takePayment"/>
    </action>

    <action id="takePayment" bean="takePaymentAction">
        <transition name="OK" to="sendOrderToErp"/>
        <transition name="NOK" to="sendPaymentFailedNotification"/>
    </action>

    <!-- ============================================================= -->
    <!-- Resilient SAP S/4HANA Order Replication                       -->
    <!-- ============================================================= -->
    <action id="sendOrderToErp" bean="sendOrderToErpAction">
        <transition name="OK" to="splitOrder"/>
        <transition name="RETRY" to="waitForErpRetry"/>
        <transition name="NOK" to="holdOrderForErpReview"/>
    </action>

    <!-- Non-blocking wait state for retry event (triggered by timer or CronJob) -->
    <wait id="waitForErpRetry" then="sendOrderToErp" prependProcessCode="false">
        <event>${process.code}_ErpRetryEvent</event>
    </wait>

    <!-- Fatal business error hold queue (alerts Customer Service instead of killing process) -->
    <action id="holdOrderForErpReview" bean="prepareOrderForManualCheckAction">
        <transition name="OK" to="waitForManualOrderCheckCSA"/>
    </action>

    <!-- Downstream Fulfillment -->
    <action id="splitOrder" bean="splitOrderAction">
        <transition name="OK" to="waitForWarehouseSubprocessEnd"/>
    </action>

    <wait id="waitForWarehouseSubprocessEnd" then="isProcessCompleted" prependProcessCode="false">
        <event>${process.code}_ConsignmentSubprocessEnd</event>
    </wait>

    <action id="isProcessCompleted" bean="subprocessesCompletedAction">
        <transition name="OK" to="setOrderExpirationTime"/>
        <transition name="NOK" to="waitForWarehouseSubprocessEnd"/>
    </action>

    <action id="setOrderExpirationTime" bean="setOrderExpirationTimeAction">
        <transition name="OK" to="sendOrderCompletedNotification"/>
        <transition name="NOK" to="error"/>
    </action>

    <action id="sendOrderCompletedNotification" bean="sendOrderCompletedNotificationAction">
        <transition name="OK" to="success"/>
    </action>

    <action id="cancelOrder" bean="cancelWholeOrderAuthorizationAction">
        <transition name="OK" to="failed"/>
    </action>

    <action id="authorizationFailedNotification" bean="sendAuthorizationFailedNotificationAction">
        <transition name="OK" to="failed"/>
    </action>

    <action id="sendPaymentFailedNotification" bean="sendPaymentFailedNotificationAction">
        <transition name="OK" to="failed"/>
    </action>

    <end id="error" state="ERROR">All went wrong.</end>
    <end id="failed" state="FAILED">Order not placed.</end>
    <end id="success" state="SUCCEEDED">Order placed.</end>

</process>


<!-- Populator & Converter -->
<bean id="sapODataOrderPopulator" class="org.training.core.erp.odata.converters.SapODataOrderPopulator">
    <constructor-arg ref="configurationService"/>
</bean>

<bean id="sapODataOrderConverter" parent="abstractPopulatingConverter">
    <property name="targetClass" value="org.training.core.erp.odata.dto.SapODataSalesOrderRequest"/>
    <property name="populators">
        <list>
            <ref bean="sapODataOrderPopulator"/>
        </list>
    </property>
</bean>

<!-- Resilient Thread-Safe Service -->
<alias name="defaultSapODataErpOrderReplicationService" alias="erpOrderReplicationService"/>
<bean id="defaultSapODataErpOrderReplicationService"
      class="org.training.core.erp.service.impl.DefaultSapODataErpOrderReplicationService">
    <constructor-arg ref="modelService"/>
    <constructor-arg ref="enumerationService"/>
    <constructor-arg ref="configurationService"/>
    <constructor-arg ref="sapODataOrderConverter"/>
</bean>

<!-- Fulfillment Action -->
<bean id="sendOrderToErpAction"
      class="org.training.fulfilmentprocess.actions.order.SendOrderToErpAction"
      parent="abstractAction">
    <property name="erpOrderReplicationService" ref="erpOrderReplicationService"/>
</bean>

package org.training.core.erp.jobs;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.BusinessProcessService;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class ErpReplicationRetryJob extends AbstractJobPerformable<CronJobModel> {

    private static final Logger LOG = LoggerFactory.getLogger(ErpReplicationRetryJob.class);
    private static final int MAX_RETRY_LIMIT = 5;

    private BusinessProcessService businessProcessService;

    @Override
    public PerformResult perform(final CronJobModel cronJob) {
        LOG.info("Starting ERP Replication Retry Job...");

        // Find orders in RETRY status that have not exceeded the max retry limit
        final String queryStr = "SELECT {o.pk} FROM {Order AS o " +
                "JOIN ErpExportStatus AS s ON {o.erpExportStatus}={s.pk}} " +
                "WHERE {s.code} = 'RETRY' " +
                "AND ({o.erpRetryCount} IS NULL OR {o.erpRetryCount} < ?maxRetries)";

        final FlexibleSearchQuery query = new FlexibleSearchQuery(queryStr);
        query.addQueryParameter("maxRetries", MAX_RETRY_LIMIT);

        final List<OrderModel> ordersToRetry = flexibleSearchService.<OrderModel>search(query).getResult();

        if (CollectionUtils.isEmpty(ordersToRetry)) {
            LOG.info("No orders currently pending ERP retry.");
            return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
        }

        LOG.info("Found {} orders pending ERP retry.", ordersToRetry.size());

        for (final OrderModel order : ordersToRetry) {
            triggerOrderProcessRetry(order);
        }

        return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
    }

    private void triggerOrderProcessRetry(final OrderModel order) {
        if (CollectionUtils.isEmpty(order.getOrderProcess())) {
            LOG.warn("No OrderProcess found for order [{}]", order.getCode());
            return;
        }

        for (final OrderProcessModel process : order.getOrderProcess()) {
            final String eventName = process.getCode() + "_ErpRetryEvent";
            LOG.info("Triggering ERP retry event [{}] for order [{}]", eventName, order.getCode());
            try {
                businessProcessService.triggerEvent(eventName);
            } catch (final Exception ex) {
                LOG.error("Failed to trigger event [{}] for order [{}]: {}", eventName, order.getCode(), ex.getMessage());
            }
        }
    }

    public void setBusinessProcessService(final BusinessProcessService businessProcessService) {
        this.businessProcessService = businessProcessService;
    }
}

package org.training.core.erp.odata.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SapODataPartnerRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("PartnerFunction")
    private String partnerFunction; // "AG" (Sold-To), "WE" (Ship-To), "RE" (Bill-To)

    @JsonProperty("Customer")
    private String customer;

    @JsonProperty("FullName")
    private String fullName;

    @JsonProperty("StreetName")
    private String streetName;

    @JsonProperty("PostalCode")
    private String postalCode;

    @JsonProperty("CityName")
    private String cityName;

    @JsonProperty("Country")
    private String country;

    // Getters and Setters...
    public String getPartnerFunction() { return partnerFunction; }
    public void setPartnerFunction(String partnerFunction) { this.partnerFunction = partnerFunction; }
    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getStreetName() { return streetName; }
    public void setStreetName(String streetName) { this.streetName = streetName; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}

Add partner navigation to SapODataSalesOrderRequest:

java
@JsonProperty("to_Partner")
private List<SapODataPartnerRequest> toPartner = new ArrayList<>();
public List<SapODataPartnerRequest> getToPartner() { return toPartner; }
public void setToPartner(List<SapODataPartnerRequest> toPartner) { this.toPartner = toPartner; }

In SapODataOrderPopulator, map shipping and billing addresses:

java
// Map Ship-To Party (WE)
if (source.getDeliveryAddress() != null) {
    final AddressModel addr = source.getDeliveryAddress();
    final SapODataPartnerRequest shipTo = new SapODataPartnerRequest();
    shipTo.setPartnerFunction("WE");
    shipTo.setFullName(addr.getFirstname() + " " + addr.getLastname());
    shipTo.setStreetName(addr.getStreetname() + " " + (addr.getStreetnumber() != null ? addr.getStreetnumber() : ""));
    shipTo.setPostalCode(addr.getPostalcode());
    shipTo.setCityName(addr.getTown());
    if (addr.getCountry() != null) {
        shipTo.setCountry(addr.getCountry().getIsocode());
    }
    target.getToPartner().add(shipTo);
}
3. Pricing Condition Alignment (to_PricingElement)

If your S/4HANA system has a standard pricing procedure (e.g., A10001 with condition PR00), S/4HANA will reject a POST request that does not supply prices matching the pricing schema unless pricing elements are populated:

json
"to_PricingElement": [
    {
        "ConditionType": "PPR0",
        "ConditionRateValue": "100.00",
        "ConditionCurrency": "USD"
    }
]
