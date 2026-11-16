package org.training.fulfilmentprocess.actions.order;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractSimpleDecisionAction.Transition;
import org.junit.Before;
import org.junit.Test;
import org.training.core.erp.service.ErpOrderReplicationService;
import org.training.core.erp.service.ErpReplicationResult;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UnitTest
public class SendOrderToErpActionTest {

    private SendOrderToErpAction action;
    private ErpOrderReplicationService erpOrderReplicationService;

    @Before
    public void setUp() {
        action = new SendOrderToErpAction();
        erpOrderReplicationService = mock(ErpOrderReplicationService.class);
        action.setErpOrderReplicationService(erpOrderReplicationService);
    }

    @Test
    public void testExecuteActionSuccess() {
        OrderProcessModel process = mock(OrderProcessModel.class);
        OrderModel order = mock(OrderModel.class);

        when(process.getOrder()).thenReturn(order);
        when(order.getCode()).thenReturn("ORD-1001");
        when(erpOrderReplicationService.replicateOrder(order))
                .thenReturn(ErpReplicationResult.success("0010004501", "IDEM-1001"));

        Transition transition = action.executeAction(process);
        assertEquals(Transition.OK, transition);
    }

    @Test
    public void testExecuteActionFailure() {
        OrderProcessModel process = mock(OrderProcessModel.class);
        OrderModel order = mock(OrderModel.class);

        when(process.getOrder()).thenReturn(order);
        when(order.getCode()).thenReturn("ORD-1002");
        when(erpOrderReplicationService.replicateOrder(order))
                .thenReturn(ErpReplicationResult.fatal("Customer locked in ERP", 400, "IDEM-1002"));

        Transition transition = action.executeAction(process);
        assertEquals(Transition.NOK, transition);
    }

    @Test
    public void testExecuteActionMissingOrder() {
        OrderProcessModel process = mock(OrderProcessModel.class);
        when(process.getOrder()).thenReturn(null);

        Transition transition = action.executeAction(process);
        assertEquals(Transition.NOK, transition);
    }
}
