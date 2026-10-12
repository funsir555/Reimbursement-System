package com.finex.auth.service.queryauth;

import java.util.List;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Immutable, server-generated authorization for one query capability. */
public record QueryAuthorization(
        Long requesterUserId,
        Long responsibleUserId,
        String permissionCode,
        List<Grant> requesterGrants,
        List<Grant> responsibleGrants,
        Set<String> serviceCompanyLimit
) {
    public record Grant(String scopeType, Set<String> companyIds, Set<Long> subjectUserIds) {
        public Grant {
            companyIds = Set.copyOf(companyIds);
            subjectUserIds = subjectUserIds == null ? null : Set.copyOf(subjectUserIds);
        }

        boolean allows(String companyId, Long subjectUserId) {
            return companyIds.contains(companyId)
                    && (subjectUserIds == null || subjectUserIds.contains(subjectUserId));
        }
    }

    public QueryAuthorization {
        requesterGrants = List.copyOf(requesterGrants);
        responsibleGrants = List.copyOf(responsibleGrants);
        serviceCompanyLimit = serviceCompanyLimit == null ? null : Set.copyOf(serviceCompanyLimit);
    }

    public boolean allows(String companyId, Long subjectUserId) {
        return (serviceCompanyLimit == null || serviceCompanyLimit.contains(companyId))
                && requesterGrants.stream().anyMatch(grant -> grant.allows(companyId, subjectUserId))
                && responsibleGrants.stream().anyMatch(grant -> grant.allows(companyId, subjectUserId));
    }

    public Set<String> companyIds() {
        return intersectingCompanyIds().stream().filter(id -> {
            Set<Long> subjects = subjectUserIdsForCompany(id);
            return subjects == null || !subjects.isEmpty();
        }).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private Set<String> intersectingCompanyIds() {
        Set<String> responsible = responsibleGrants.stream()
                .flatMap(grant -> grant.companyIds().stream()).collect(java.util.stream.Collectors.toSet());
        return requesterGrants.stream().flatMap(grant -> grant.companyIds().stream())
                .filter(responsible::contains)
                .filter(id -> serviceCompanyLimit == null || serviceCompanyLimit.contains(id))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public boolean isEmpty() {
        return companyIds().isEmpty();
    }

    /** Null means every active subject in this company; empty means none. */
    public Set<Long> subjectUserIdsForCompany(String companyId) {
        if (!intersectingCompanyIds().contains(companyId)) return Set.of();
        Set<Long> requester = unionSubjects(requesterGrants, companyId);
        Set<Long> responsible = unionSubjects(responsibleGrants, companyId);
        if (requester == null) return responsible;
        if (responsible == null) return requester;
        return requester.stream().filter(responsible::contains)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public boolean allowsAssignedTask(String companyId) {
        return companyIds().contains(companyId)
                && requesterGrants.stream().anyMatch(grant -> grant.allows(companyId, requesterUserId))
                && responsibleGrants.stream().anyMatch(grant -> grant.allows(companyId, requesterUserId));
    }

    public Set<Long> assignedSubjectUserIdsForCompany(String companyId) {
        boolean selfTaskGrant = requesterGrants.stream()
                .anyMatch(grant -> "SELF".equals(grant.scopeType()) && grant.companyIds().contains(companyId));
        boolean responsibleAcceptsTask = responsibleGrants.stream()
                .anyMatch(grant -> grant.allows(companyId, requesterUserId));
        if (selfTaskGrant && responsibleAcceptsTask) return null;
        return subjectUserIdsForCompany(companyId);
    }

    private Set<Long> unionSubjects(List<Grant> grants, String companyId) {
        Set<Long> result = new java.util.HashSet<>();
        for (Grant grant : grants) {
            if (!grant.companyIds().contains(companyId)) continue;
            if (grant.subjectUserIds() == null) return null;
            result.addAll(grant.subjectUserIds());
        }
        return Set.copyOf(result);
    }

    public String digest() {
        String material = permissionCode + "|" + requesterUserId + "|" + responsibleUserId
                + "|" + canonical(requesterGrants) + "|" + canonical(responsibleGrants)
                + "|" + (serviceCompanyLimit == null ? "*" : serviceCompanyLimit.stream().sorted().toList());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private String canonical(List<Grant> grants) {
        return grants.stream().map(grant -> grant.scopeType() + ":"
                        + grant.companyIds().stream().sorted().toList() + ":"
                        + (grant.subjectUserIds() == null ? "*" : grant.subjectUserIds().stream().sorted().toList()))
                .sorted().toList().toString();
    }
}
