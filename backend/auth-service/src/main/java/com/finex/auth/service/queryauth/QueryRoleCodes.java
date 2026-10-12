package com.finex.auth.service.queryauth;

import java.util.List;

/** Role codes are stored in uppercase in the existing settings domain. */
final class QueryRoleCodes {
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private QueryRoleCodes() { }

    static boolean isSuperAdmin(Object roleCode) {
        return roleCode instanceof String code && SUPER_ADMIN.equalsIgnoreCase(code);
    }

    static boolean hasSuperAdmin(List<String> roleCodes) {
        return roleCodes != null && roleCodes.stream().anyMatch(QueryRoleCodes::isSuperAdmin);
    }
}
