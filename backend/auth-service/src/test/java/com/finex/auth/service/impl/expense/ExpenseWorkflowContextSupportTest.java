package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseWorkflowContextSupportTest {

    @Mock
    private AbstractExpenseWorkflowSupport support;

    @Test
    void buildRuntimeFlowContextDelegatesToSharedSupport() {
        User user = new User();
        ProcessDocumentTemplate template = new ProcessDocumentTemplate();
        ProcessFormDesign formDesign = new ProcessFormDesign();
        ProcessExpenseDetailDesign detailDesign = new ProcessExpenseDetailDesign();
        Map<String, Object> formData = Map.of("amount", 12);
        List<ExpenseDetailInstanceDTO> expenseDetails = List.of(new ExpenseDetailInstanceDTO());
        Map<String, Object> expected = Map.of("amount", 12);
        ExpenseWorkflowContextSupport contextSupport = new ExpenseWorkflowContextSupport(support);
        when(support.buildRuntimeFlowContext(user, template, formDesign, formData, detailDesign, expenseDetails)).thenReturn(expected);

        Map<String, Object> actual = contextSupport.buildRuntimeFlowContext(
                user,
                template,
                formDesign,
                formData,
                detailDesign,
                expenseDetails
        );

        assertSame(expected, actual);
        verify(support).buildRuntimeFlowContext(user, template, formDesign, formData, detailDesign, expenseDetails);
    }

    @Test
    void runtimeContextAndSnapshotInspectionDelegateToSharedSupport() {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        Map<String, Object> expectedContext = Map.of("documentCode", "DOC-1");
        RawFlowSnapshotSignature expectedSignature = new RawFlowSnapshotSignature(true, false, true);
        ExpenseWorkflowContextSupport contextSupport = new ExpenseWorkflowContextSupport(support);
        when(support.buildRuntimeContextForInstance(instance)).thenReturn(expectedContext);
        when(support.inspectRawFlowSnapshot("{}")).thenReturn(expectedSignature);

        Map<String, Object> actualContext = contextSupport.buildRuntimeContextForInstance(instance);
        RawFlowSnapshotSignature actualSignature = contextSupport.inspectRawFlowSnapshot("{}");
        contextSupport.validateFlowSnapshot("{}");

        assertSame(expectedContext, actualContext);
        assertSame(expectedSignature, actualSignature);
        verify(support).buildRuntimeContextForInstance(instance);
        verify(support).inspectRawFlowSnapshot("{}");
        verify(support).validateFlowSnapshot("{}");
    }

    @Test
    void previewDelegatesToSharedSupport() {
        ExpenseWorkflowContextSupport contextSupport = new ExpenseWorkflowContextSupport(support);
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        ProcessFlowNodeDTO node = new ProcessFlowNodeDTO();
        ProcessFlowRouteDTO route = new ProcessFlowRouteDTO();
        User user = new User();
        Map<String, Object> context = Map.of("amount", 12);
        List<User> users = List.of(user);

        when(support.previewMatchedRoute(List.of(route), context)).thenReturn(route);
        when(support.previewResolvedApprovers(node, context)).thenReturn(users);
        when(support.previewResolvedCcRecipients(instance, node, context)).thenReturn(users);
        when(support.previewResolvedPaymentExecutors(node, context)).thenReturn(users);

        assertSame(route, contextSupport.previewMatchedRoute(List.of(route), context));
        assertSame(users, contextSupport.previewResolvedApprovers(node, context));
        assertSame(users, contextSupport.previewResolvedCcRecipients(instance, node, context));
        assertSame(users, contextSupport.previewResolvedPaymentExecutors(node, context));

        verify(support).previewMatchedRoute(List.of(route), context);
        verify(support).previewResolvedApprovers(node, context);
        verify(support).previewResolvedCcRecipients(instance, node, context);
        verify(support).previewResolvedPaymentExecutors(node, context);
    }
}
