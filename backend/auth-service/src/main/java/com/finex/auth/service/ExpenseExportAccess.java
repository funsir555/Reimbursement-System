package com.finex.auth.service;

import com.finex.auth.dto.ExpenseExportSubmitDTO;

public interface ExpenseExportAccess {
    void requireScene(Long userId, ExpenseExportSubmitDTO payload);
}
