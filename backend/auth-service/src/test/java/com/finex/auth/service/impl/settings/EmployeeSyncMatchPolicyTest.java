package com.finex.auth.service.impl.settings;

import com.finex.auth.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeSyncMatchPolicyTest {

    @Test
    void matchesByPhoneOrEmailWithoutChangingExistingIdentity() {
        User user = user(7L);
        assertThat(EmployeeSyncMatchPolicy.match(Map.of("p", List.of(user)), Map.of(), "p", "").user())
                .isSameAs(user);
        assertThat(EmployeeSyncMatchPolicy.match(Map.of(), Map.of("e", List.of(user)), "", "e").user())
                .isSameAs(user);
        assertThat(EmployeeSyncMatchPolicy.match(Map.of("p", List.of(user)), Map.of("e", List.of(user)), "p", "e")
                .conflictMessage()).isNull();
    }

    @Test
    void rejectsDuplicatePhoneBeforeEmailAndNeverSelectsArbitrarily() {
        var result = EmployeeSyncMatchPolicy.match(Map.of("p", List.of(user(1L), user(2L))),
                Map.of("e", List.of(user(3L))), "p", "e");
        assertThat(result.user()).isNull();
        assertThat(result.conflictMessage()).contains("手机号匹配到多名员工");
    }

    @Test
    void rejectsDuplicateEmailAndConflictingIdentities() {
        var duplicate = EmployeeSyncMatchPolicy.match(Map.of(), Map.of("e", List.of(user(1L), user(2L))), "", "e");
        assertThat(duplicate.conflictMessage()).contains("邮箱匹配到多名员工");

        var conflicting = EmployeeSyncMatchPolicy.match(Map.of("p", List.of(user(1L))),
                Map.of("e", List.of(user(2L))), "p", "e");
        assertThat(conflicting.user()).isNull();
        assertThat(conflicting.conflictMessage()).contains("手机号与邮箱匹配到不同员工");
    }

    @Test
    void emptyKeysDoNotMatchBlankIndexEntries() {
        var result = EmployeeSyncMatchPolicy.match(Map.of("", List.of(user(1L))),
                Map.of("", List.of(user(2L))), "", "");
        assertThat(result.user()).isNull();
        assertThat(result.conflictMessage()).isNull();
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
