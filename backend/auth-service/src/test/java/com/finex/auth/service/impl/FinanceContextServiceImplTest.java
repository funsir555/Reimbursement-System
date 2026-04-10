package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceContextMetaVO;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceContextServiceImplTest {

    @Mock
    private SystemCompanyMapper systemCompanyMapper;

    @Mock
    private FinanceAccountSetMapper financeAccountSetMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private FinanceContextServiceImpl service;

    @Test
    void getMetaPrefersCompanyWithActiveAccountSetAsDefault() {
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(
                buildCompany("COMPANY_A", "COMP_A", "广州公司"),
                buildCompany("COMPANY_B", "COMP_B", "深圳公司")
        ));
        when(financeAccountSetMapper.selectList(any())).thenReturn(List.of(
                buildAccountSet("COMPANY_B", "ACTIVE")
        ));
        when(userService.getById(7L)).thenReturn(buildUser(7L, "COMPANY_A"));

        FinanceContextMetaVO meta = service.getMeta(7L);

        assertEquals("COMPANY_A", meta.getCurrentUserCompanyId());
        assertEquals("COMPANY_B", meta.getDefaultCompanyId());
        assertFalse(meta.getCompanyOptions().get(0).isHasActiveAccountSet());
        assertTrue(meta.getCompanyOptions().get(1).isHasActiveAccountSet());
        assertEquals("COMP_B - 深圳公司", meta.getCompanyOptions().get(1).getLabel());
    }

    private SystemCompany buildCompany(String companyId, String companyCode, String companyName) {
        SystemCompany company = new SystemCompany();
        company.setCompanyId(companyId);
        company.setCompanyCode(companyCode);
        company.setCompanyName(companyName);
        company.setStatus(1);
        return company;
    }

    private FinanceAccountSet buildAccountSet(String companyId, String status) {
        FinanceAccountSet accountSet = new FinanceAccountSet();
        accountSet.setCompanyId(companyId);
        accountSet.setStatus(status);
        return accountSet;
    }

    private User buildUser(Long id, String companyId) {
        User user = new User();
        user.setId(id);
        user.setCompanyId(companyId);
        return user;
    }
}
