package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceContextMetaVO;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.service.FinanceContextService;
import com.finex.auth.service.UserService;
import com.finex.auth.service.impl.voucher.VoucherContextSupport;
import org.springframework.stereotype.Service;

@Service
public class FinanceContextServiceImpl implements FinanceContextService {

    private final VoucherContextSupport voucherContextSupport;

    public FinanceContextServiceImpl(
            SystemCompanyMapper systemCompanyMapper,
            FinanceAccountSetMapper financeAccountSetMapper,
            UserService userService
    ) {
        this.voucherContextSupport = new VoucherContextSupport(systemCompanyMapper, financeAccountSetMapper, userService);
    }

    @Override
    public FinanceContextMetaVO getMeta(Long currentUserId) {
        return voucherContextSupport.getMeta(currentUserId);
    }
}
