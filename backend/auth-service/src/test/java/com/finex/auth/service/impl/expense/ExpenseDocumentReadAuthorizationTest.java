package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpenseDocumentReadAuthorizationTest {
    private final QueryAuthorizationService authorizations = mock(QueryAuthorizationService.class);
    private final ProcessDocumentInstanceMapper documents = mock(ProcessDocumentInstanceMapper.class);
    private final ProcessDocumentTaskMapper tasks = mock(ProcessDocumentTaskMapper.class);
    private final ExpenseDocumentReadAuthorization read = new ExpenseDocumentReadAuthorization(
            authorizations, documents, tasks);

    @BeforeEach
    void initMapping() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "document-read"),
                ProcessDocumentInstance.class);
    }

    @Test
    void submittedDocumentCannotUseCreationPermissionAsDataScope() {
        when(documents.selectOne(any())).thenReturn(document("PENDING_APPROVAL"));
        assertThatThrownBy(() -> read.requireDocument(7L, "DOC-1", List.of("expense:create:view")))
                .isInstanceOf(SecurityException.class)
                .hasMessage(QueryAuthorizationService.NO_PERMISSION);
    }

    @Test
    void ownDocumentUsesSameRoleQueryPermissionAndScope() {
        when(documents.selectOne(any())).thenReturn(document("PENDING_APPROVAL"));
        var grant = new QueryAuthorization.Grant("SELF", Set.of("COMPANY-A"), Set.of(7L));
        when(authorizations.resolveForSelf(7L, ExpenseDocumentReadAuthorization.MY_DOCUMENTS))
                .thenReturn(new QueryAuthorization(7L, 7L,
                        ExpenseDocumentReadAuthorization.MY_DOCUMENTS,
                        List.of(grant), List.of(grant), null));

        assertThatCode(() -> read.requireDocument(7L, "DOC-1",
                List.of(ExpenseDocumentReadAuthorization.MY_DOCUMENTS))).doesNotThrowAnyException();
    }

    private ProcessDocumentInstance document(String status) {
        ProcessDocumentInstance document = new ProcessDocumentInstance();
        document.setDocumentCode("DOC-1");
        document.setCompanyId("COMPANY-A");
        document.setSubmitterUserId(7L);
        document.setStatus(status);
        document.setDeleted(false);
        return document;
    }
}
