package com.finex.auth.service;

import java.util.List;

public interface ExpenseDocumentReadAccess {
    void requireDocument(Long userId, String documentCode, List<String> grantedPermissions);
}
