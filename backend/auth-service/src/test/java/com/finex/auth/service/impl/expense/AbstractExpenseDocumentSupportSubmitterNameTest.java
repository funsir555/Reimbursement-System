package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AbstractExpenseDocumentSupportSubmitterNameTest {

    @Mock
    private AbstractExpenseDocumentSupport support;

    @Test
    void resolveUserDisplayNamePrefersNameOverUsername() throws Exception {
        User user = new User();
        user.setUsername("lisi");
        user.setName("李四");

        assertEquals("李四", invokeResolveUserDisplayName(user, "legacy-user"));
    }

    @Test
    void resolveUserDisplayNameFallsBackToNameWhenUsernameIsMissing() throws Exception {
        User user = new User();
        user.setName("李四");

        assertEquals("李四", invokeResolveUserDisplayName(user, "legacy-user"));
    }

    private String invokeResolveUserDisplayName(User user, String username) throws Exception {
        Method method = AbstractExpenseDocumentSupport.class.getDeclaredMethod("resolveUserDisplayName", User.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(support, user, username);
    }
}
