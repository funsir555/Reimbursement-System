package com.finex.auth.service.queryauth;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QueryAuthorizationTest {
    private static final String CODE = QueryAuthorizationService.EXPENSE_QUERY;

    @Test
    void mergesRangesOnlyInsideOneCapabilityAndIntersectsRequesterWithResponsible() {
        QueryAuthorization access = new QueryAuthorization(10L, 20L, CODE,
                List.of(grant("SELF", Set.of("A"), Set.of(10L)),
                        grant("SELECTED_COMPANIES", Set.of("B"), null)),
                List.of(grant("SELECTED_COMPANIES", Set.of("A", "B", "C"), null)), null);

        assertThat(access.companyIds()).containsExactlyInAnyOrder("A", "B");
        assertThat(access.allows("A", 10L)).isTrue();
        assertThat(access.allows("A", 99L)).isFalse();
        assertThat(access.allows("B", 99L)).isTrue();
        assertThat(access.allows("C", 99L)).isFalse();
    }

    @Test
    void disjointPersonalScopesDoNotBecomeAnAuthorizedZeroResult() {
        QueryAuthorization access = new QueryAuthorization(10L, 20L, CODE,
                List.of(grant("SELF", Set.of("A"), Set.of(10L))),
                List.of(grant("DEPARTMENT_TREE", Set.of("A"), Set.of(20L))), null);

        assertThat(access.companyIds()).isEmpty();
        assertThat(access.isEmpty()).isTrue();
    }

    @Test
    void employeeLimitCanOnlyNarrowAndRevocationChangesDigest() {
        var broad = grant("ALL_COMPANIES", Set.of("A", "B"), null);
        QueryAuthorization before = new QueryAuthorization(10L, 20L, CODE,
                List.of(broad), List.of(broad), Set.of("A"));
        QueryAuthorization after = new QueryAuthorization(10L, 20L, CODE,
                List.of(grant("SELECTED_COMPANIES", Set.of("B"), null)), List.of(broad), Set.of("A"));

        assertThat(before.companyIds()).containsExactly("A");
        assertThat(after.isEmpty()).isTrue();
        assertThat(after.digest()).isNotEqualTo(before.digest());
    }

    @Test
    void selfScopeAllowsAssignedTaskButDoesNotExposeOtherSubmittedDocuments() {
        QueryAuthorization access = new QueryAuthorization(10L, 20L, CODE,
                List.of(grant("SELF", Set.of("A"), Set.of(10L))),
                List.of(grant("ALL_COMPANIES", Set.of("A"), null)), null);

        assertThat(access.allowsAssignedTask("A")).isTrue();
        assertThat(access.assignedSubjectUserIdsForCompany("A")).isNull();
        assertThat(access.subjectUserIdsForCompany("A")).containsExactly(10L);
    }

    private QueryAuthorization.Grant grant(String scope, Set<String> companies, Set<Long> subjects) {
        return new QueryAuthorization.Grant(scope, companies, subjects);
    }
}
