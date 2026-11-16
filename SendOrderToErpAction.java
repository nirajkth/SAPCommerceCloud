/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package org.training.fulfilmentprocess.actions.order;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractSimpleDecisionAction;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.training.core.erp.service.ErpOrderReplicationService;
import org.training.core.erp.service.ErpReplicationResult;

/**
 * Production-ready fulfillment process action to replicate Commerce orders to SAP S/4HANA ERP via OData.
 */
public class SendOrderToErpAction extends AbstractSimpleDecisionAction<OrderProcessModel> {

    private static final Logger LOG = Logger.getLogger(SendOrderToErpAction.class);

    private ErpOrderReplicationService erpOrderReplicationService;

    @Override
    public Transition executeAction(final OrderProcessModel process) {
        final OrderModel order = process.getOrder();

        if (order == null) {
            LOG.error("Missing order in OrderProcessModel, terminating ERP replication action.");
            return Transition.NOK;
        }

        LOG.info(String.format("Starting ERP replication action for order [%s]", order.getCode()));

        try {
            final ErpReplicationResult result = getErpOrderReplicationService().replicateOrder(order);

            if (result != null && result.isSuccess()) {
                LOG.info(String.format("Order [%s] successfully replicated to SAP S/4HANA with ERP Sales Document ID [%s]",
                        order.getCode(), result.getErpOrderCode()));
                return Transition.OK;
            } else {
                final String failureReason = result != null ? result.getMessage() : "Unknown ERP replication error";
                LOG.warn(String.format("Order [%s] ERP replication was not successful: %s", order.getCode(), failureReason));
                return Transition.NOK;
            }
        } catch (final Exception ex) {
            LOG.error(String.format("Unhandled exception during ERP order replication for order [%s]", order.getCode()), ex);
            return Transition.NOK;
        }
    }

    protected ErpOrderReplicationService getErpOrderReplicationService() {
        return erpOrderReplicationService;
    }

    @Required
    public void setErpOrderReplicationService(final ErpOrderReplicationService erpOrderReplicationService) {
        this.erpOrderReplicationService = erpOrderReplicationService;
    }
}
