package com.finex.auth.service;

import com.finex.auth.dto.FinanceContextMetaVO;

public interface FinanceContextService {

    FinanceContextMetaVO getMeta(Long currentUserId);
}
