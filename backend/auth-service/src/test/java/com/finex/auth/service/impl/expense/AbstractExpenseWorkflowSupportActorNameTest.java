package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTaskMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractExpenseWorkflowSupportActorNameTest {

    @Mock
    private ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    @Mock
    private ProcessDocumentTaskMapper processDocumentTaskMapper;
    @Mock
    private ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    @Mock
    private SystemPermissionMapper systemPermissionMapper;
    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;
    @Mock
    private SystemRolePermissionMapper systemRolePermissionMapper;
    @Mock
    private SystemUserRoleMapper systemUserRoleMapper;
    @Mock
    private UserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolveActorDisplayNamePrefersRealName() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        user.setName("Real Name");
        user.setUsername("legacy-user");
        when(userMapper.selectById(1L)).thenReturn(user);

        assertEquals("Real Name", invokeResolveActorDisplayName(1L, "legacy-user"));
    }

    @Test
    void resolveActorDisplayNameFallsBackToUsername() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setStatus(1);
        user.setUsername("legacy-user");
        when(userMapper.selectById(2L)).thenReturn(user);

        assertEquals("legacy-user", invokeResolveActorDisplayName(2L, "legacy-user"));
    }

    private String invokeResolveActorDisplayName(Long userId, String username) throws Exception {
        AbstractExpenseWorkflowSupport support = new AbstractExpenseWorkflowSupport(
                processDocumentInstanceMapper,
                processDocumentTaskMapper,
                processDocumentActionLogMapper,
                processDocumentExpenseDetailMapper,
                systemPermissionMapper,
                systemDepartmentMapper,
                systemRolePermissionMapper,
                systemUserRoleMapper,
                userMapper,
                objectMapper
        );
        Method method = AbstractExpenseWorkflowSupport.class.getDeclaredMethod("resolveActorDisplayName", Long.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(support, userId, username);
    }
}
