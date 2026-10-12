package com.finex.auth.service.impl.agentquery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTask;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.service.port.ExpenseAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpenseAgentQueryServiceTest {

    private final ProcessDocumentInstanceMapper documentMapper = mock(ProcessDocumentInstanceMapper.class);
    private final ProcessDocumentTaskMapper taskMapper = mock(ProcessDocumentTaskMapper.class);
    private final ExpenseAgentQueryService service = new ExpenseAgentQueryService(documentMapper, taskMapper);


    @Test
    void assignedScopeUsesTasksAndAlwaysBindsCompany() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), ProcessDocumentInstance.class);
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setDocumentCode("DOC-001");
        when(taskMapper.selectList(any())).thenReturn(List.of(task));

        ProcessDocumentInstance document = new ProcessDocumentInstance();
        document.setId(1L);
        document.setDocumentCode("DOC-001");
        document.setTotalAmount(new BigDecimal("12.34"));
        when(documentMapper.selectList(any())).thenReturn(List.of(document));

        List<ExpenseAgentQueryPort.DocumentView> result = service.queryAuthorizedDocuments(query(
                ExpenseAgentQueryPort.Scope.ASSIGNED_TO_USER
        ), authorization());

        assertThat(result).singleElement().extracting(ExpenseAgentQueryPort.DocumentView::documentCode)
                .isEqualTo("DOC-001");
        verify(taskMapper).selectList(any());

        ArgumentCaptor<LambdaQueryWrapper<ProcessDocumentInstance>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(documentMapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapper<ProcessDocumentInstance> wrapper = wrapperCaptor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("pm_document_instance.company_id", "document_code", "deleted");
        assertThat(wrapper.getParamNameValuePairs()).containsValue("COMPANY_A");
    }

    @Test
    void assignedScopeWithNoTasksDoesNotQueryDocuments() {
        when(taskMapper.selectList(any())).thenReturn(List.of());

        List<ExpenseAgentQueryPort.DocumentView> result = service.queryAuthorizedDocuments(query(
                ExpenseAgentQueryPort.Scope.ASSIGNED_TO_USER
        ), authorization());

        assertThat(result).isEmpty();
        verify(documentMapper, never()).selectList(any());
    }

    @Test
    void submittedScopeDoesNotReadTaskMapper() {
        when(documentMapper.selectList(any())).thenReturn(List.of());

        service.queryAuthorizedDocuments(query(ExpenseAgentQueryPort.Scope.SUBMITTED_BY_USER), authorization());

        verify(taskMapper, never()).selectList(any());
        verify(documentMapper).selectList(any());
    }

    @Test
    void submittedSelfScopeCanReachAnotherPaymentCompanyOnlyWithinResponsibleLimit() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), ProcessDocumentInstance.class);
        var requester = new QueryAuthorization.Grant("SELF", Set.of("COMPANY_A", "COMPANY_B"), Set.of(9L));
        var responsible = new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("COMPANY_B"), null);
        var authorization = new QueryAuthorization(9L, 1L, "expense:agent:query",
                List.of(requester), List.of(responsible), null);
        var query = new ExpenseAgentQueryPort.Query(9L, null,
                ExpenseAgentQueryPort.Scope.SUBMITTED_BY_USER, "report", true,
                null, null, null, null, null, null, 10);
        when(documentMapper.selectList(any())).thenReturn(List.of());

        service.queryAuthorizedDocuments(query, authorization);

        ArgumentCaptor<LambdaQueryWrapper<ProcessDocumentInstance>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(documentMapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("submitter_user_id");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue("COMPANY_B")
                .doesNotContainValue("COMPANY_A");
    }

    @Test
    void assignedSelfScopeCanReadOwnCrossCompanyTaskRegardlessOfSubmitter() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), ProcessDocumentInstance.class);
        var requester = new QueryAuthorization.Grant("SELF", Set.of("COMPANY_A", "COMPANY_B"), Set.of(9L));
        var responsible = new QueryAuthorization.Grant("SELECTED_COMPANIES", Set.of("COMPANY_B"), null);
        var authorization = new QueryAuthorization(9L, 1L, "expense:agent:query",
                List.of(requester), List.of(responsible), null);
        var query = new ExpenseAgentQueryPort.Query(9L, null,
                ExpenseAgentQueryPort.Scope.ASSIGNED_TO_USER, "report", true,
                null, null, null, null, null, null, 10);
        ProcessDocumentTask task = new ProcessDocumentTask();
        task.setDocumentCode("DOC-B");
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(documentMapper.selectList(any())).thenReturn(List.of());

        service.queryAuthorizedDocuments(query, authorization);

        ArgumentCaptor<LambdaQueryWrapper<ProcessDocumentInstance>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(documentMapper).selectList(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("company_id", "document_code")
                .doesNotContain("submitter_user_id");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue("COMPANY_B")
                .doesNotContainValue("COMPANY_A");
    }

    @Test
    void aggregateUsesFullAuthorizedSetWithoutDisplayLimit() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), ProcessDocumentInstance.class);
        when(documentMapper.aggregateAuthorized(any())).thenReturn(Map.of(
                "total_count", 12L, "total_amount", new BigDecimal("1234.56")));

        var aggregate = service.aggregateAuthorizedDocuments(query(
                ExpenseAgentQueryPort.Scope.SUBMITTED_BY_USER), authorization());

        assertThat(aggregate.count()).isEqualTo(12L);
        assertThat(aggregate.totalAmount()).isEqualByComparingTo("1234.56");
        ArgumentCaptor<LambdaQueryWrapper<ProcessDocumentInstance>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(documentMapper).aggregateAuthorized(wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("company_id", "submitter_user_id")
                .doesNotContainIgnoringCase("limit");
    }


    private ExpenseAgentQueryPort.Query query(ExpenseAgentQueryPort.Scope scope) {
        return new ExpenseAgentQueryPort.Query(
                9L,
                "COMPANY_A",
                scope,
                "report",
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                10
        );
    }

    private QueryAuthorization authorization() {
        var grant = new QueryAuthorization.Grant("ALL_COMPANIES", Set.of("COMPANY_A"), null);
        return new QueryAuthorization(9L, 9L, "expense:agent:query",
                List.of(grant), List.of(grant), null);
    }
}
