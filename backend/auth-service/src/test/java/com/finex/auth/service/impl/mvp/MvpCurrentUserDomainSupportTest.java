package com.finex.auth.service.impl.mvp;

import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MvpCurrentUserDomainSupportTest {

    @Mock
    private UserService userService;
    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private ExpenseDocumentService expenseDocumentService;

    private MvpCurrentUserDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new MvpCurrentUserDomainSupport(userService, asyncTaskRecordMapper, expenseDocumentService);
    }

    @Test
    void getCurrentUserBuildsProfileFromAuthOwner() {
        User user = new User();
        user.setId(3L);
        user.setUsername("alice");
        user.setName("Alice");
        user.setPhone("13800138000");
        user.setEmail("alice@example.com");
        user.setPosition(null);
        user.setLaborRelationBelong(null);
        user.setCompanyId("C1");

        when(userService.getById(3L)).thenReturn(user);
        when(userService.getRoleCodes(3L)).thenReturn(List.of("EMPLOYEE"));
        when(userService.getPermissionCodes(3L)).thenReturn(List.of("dashboard:view"));

        UserProfileVO result = support.getCurrentUser(3L);

        assertEquals(3L, result.getUserId());
        assertEquals("alice", result.getUsername());
        assertEquals("Alice", result.getName());
        assertEquals("Employee", result.getPosition());
        assertEquals("Headquarters", result.getLaborRelationBelong());
        assertEquals(List.of("EMPLOYEE"), result.getRoles());
        assertEquals(List.of("dashboard:view"), result.getPermissionCodes());
    }
}
