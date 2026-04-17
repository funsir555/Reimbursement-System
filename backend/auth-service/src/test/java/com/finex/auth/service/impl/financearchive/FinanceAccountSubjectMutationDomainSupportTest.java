package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectCloseDTO;
import com.finex.auth.dto.FinanceAccountSubjectDerivedDefaultsVO;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.dto.FinanceAccountSubjectStatusDTO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceAccountSubjectMutationDomainSupportTest {

    @Mock
    private FinanceAccountSubjectMapper financeAccountSubjectMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private GlAccvouchMapper glAccvouchMapper;
    @Mock
    private FinanceAccountSetMapper financeAccountSetMapper;
    @Mock
    private FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper;

    private FinanceAccountSubjectMutationDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceAccountSubjectMutationDomainSupport(
                financeAccountSubjectMapper,
                systemCompanyMapper,
                glAccvouchMapper,
                new ObjectMapper(),
                financeAccountSetMapper,
                financeAccountSetTemplateSubjectMapper
        );
    }

    @Test
    void createSubjectRejectsDisabledParent() {
        SystemCompany company = enabledCompany("COMPANY_A");
        FinanceAccountSubject parent = subject("COMPANY_A", "1001", null, 1);
        parent.setStatus(0);

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(null);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(parent));

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("100101");
        dto.setSubjectName("\u5e93\u5b58\u73b0\u91d1-\u5b50\u79d1\u76ee");
        dto.setParentSubjectCode("1001");
        dto.setSubjectCategory("ASSET");

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                support.createSubject("COMPANY_A", dto, "tester")
        );
        assertEquals("\u4e0a\u7ea7\u79d1\u76ee\u672a\u542f\u7528\u6216\u5df2\u5c01\u5b58\uff0c\u4e0d\u80fd\u65b0\u589e\u5b50\u79d1\u76ee", error.getMessage());
    }

    @Test
    void createSubjectAutoDerivesControlledFields() {
        SystemCompany company = enabledCompany("COMPANY_A");

        FinanceAccountSubject parent = subject("COMPANY_A", "2202", null, 1);
        parent.setSubjectName("\u5e94\u4ed8\u8d26\u6b3e");
        parent.setBalanceDirection("CREDIT");
        parent.setSubjectCategory("LIABILITY");
        parent.setLeafFlag(1);

        FinanceAccountSubject created = subject("COMPANY_A", "220201", "2202", 2);
        created.setSubjectName("\u5e94\u4ed8\u8d26\u6b3e-\u5b50\u79d1\u76ee");
        created.setBalanceDirection("CREDIT");
        created.setSubjectCategory("LIABILITY");
        created.setLeafFlag(1);
        created.setCclassany("LIABILITY");
        created.setBproperty(0);
        created.setCbookType("GENERAL");

        FinanceAccountSubject inserted = new FinanceAccountSubject();

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(null).thenReturn(created);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(parent)).thenReturn(List.of());
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            FinanceAccountSubject payload = invocation.getArgument(0);
            inserted.setParentSubjectCode(payload.getParentSubjectCode());
            inserted.setSubjectLevel(payload.getSubjectLevel());
            inserted.setSubjectCategory(payload.getSubjectCategory());
            inserted.setBalanceDirection(payload.getBalanceDirection());
            inserted.setLeafFlag(payload.getLeafFlag());
            inserted.setCclassany(payload.getCclassany());
            inserted.setBproperty(payload.getBproperty());
            inserted.setCbookType(payload.getCbookType());
            return 1;
        }).when(financeAccountSubjectMapper).insert(any(FinanceAccountSubject.class));

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("220201");
        dto.setSubjectName("\u5e94\u4ed8\u8d26\u6b3e-\u5b50\u79d1\u76ee");
        dto.setParentSubjectCode("1001");
        dto.setSubjectCategory("ASSET");
        dto.setLeafFlag(0);
        dto.setBproperty(1);
        dto.setCclassany("ASSET");
        dto.setCbookType("CASH");

        FinanceAccountSubjectDetailVO detail = support.createSubject("COMPANY_A", dto, "tester");

        assertEquals("2202", inserted.getParentSubjectCode());
        assertEquals(2, inserted.getSubjectLevel());
        assertEquals("LIABILITY", inserted.getSubjectCategory());
        assertEquals("CREDIT", inserted.getBalanceDirection());
        assertEquals(1, inserted.getLeafFlag());
        assertEquals("LIABILITY", inserted.getCclassany());
        assertEquals(0, inserted.getBproperty());
        assertEquals("GENERAL", inserted.getCbookType());
        assertEquals("LIABILITY", detail.getSubjectCategory());
        assertEquals("CREDIT", detail.getBalanceDirection());
        assertEquals(0, parent.getLeafFlag());
    }

    @Test
    void createSubjectKeepsNormalChineseAndLeavesCodeFieldsUntouched() {
        SystemCompany company = enabledCompany("COMPANY_A");

        FinanceAccountSubject parent = subject("COMPANY_A", "5601", null, 1);
        parent.setSubjectName("\u9500\u552e\u8d39\u7528");
        parent.setSubjectCategory("PROFIT");
        parent.setBalanceDirection("DEBIT");
        parent.setLeafFlag(1);

        FinanceAccountSubject created = subject("COMPANY_A", "560101", "5601", 2);
        created.setSubjectName("\u5e7f\u544a\u5ba3\u4f20\u8d39");
        created.setChelp("\u5e7f\u544a\u5ba3\u4f20\u8d39");
        created.setCmeasure("\u9879");
        created.setCother("\u6b63\u5e38\u4e2d\u6587\u5907\u6ce8");
        created.setCassItem("97");

        FinanceAccountSubject inserted = new FinanceAccountSubject();

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(null).thenReturn(created);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(parent)).thenReturn(List.of());
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            FinanceAccountSubject payload = invocation.getArgument(0);
            inserted.setSubjectName(payload.getSubjectName());
            inserted.setChelp(payload.getChelp());
            inserted.setCmeasure(payload.getCmeasure());
            inserted.setCother(payload.getCother());
            inserted.setCassItem(payload.getCassItem());
            return 1;
        }).when(financeAccountSubjectMapper).insert(any(FinanceAccountSubject.class));

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("560101");
        dto.setSubjectName("\u5e7f\u544a\u5ba3\u4f20\u8d39");
        dto.setChelp("\u5e7f\u544a\u5ba3\u4f20\u8d39");
        dto.setCmeasure("\u9879");
        dto.setCother("\u6b63\u5e38\u4e2d\u6587\u5907\u6ce8");
        dto.setBitem(1);
        dto.setCassItem("97");

        support.createSubject("COMPANY_A", dto, "tester");

        assertEquals("\u5e7f\u544a\u5ba3\u4f20\u8d39", inserted.getSubjectName());
        assertEquals("\u5e7f\u544a\u5ba3\u4f20\u8d39", inserted.getChelp());
        assertEquals("\u9879", inserted.getCmeasure());
        assertEquals("\u6b63\u5e38\u4e2d\u6587\u5907\u6ce8", inserted.getCother());
        assertEquals("97", inserted.getCassItem());
    }

    @Test
    void updateSubjectRepairsUtf8AsGbkMojibakeBeforePersist() {
        FinanceAccountSubject parent = subject("COMPANY_A", "5601", null, 1);
        parent.setSubjectName("\u9500\u552e\u8d39\u7528");
        parent.setSubjectCategory("PROFIT");
        parent.setBalanceDirection("DEBIT");

        FinanceAccountSubject existing = subject("COMPANY_A", "560101", "5601", 2);
        existing.setSubjectName("\u5e7f\u544a\u5ba3\u4f20\u8d39");
        existing.setSubjectCategory("PROFIT");
        existing.setBalanceDirection("DEBIT");
        existing.setCclassany("PROFIT");
        existing.setBproperty(1);
        existing.setCbookType("GENERAL");
        existing.setLeafFlag(1);

        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(existing).thenReturn(parent).thenReturn(existing);
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(0L);

        String garbledSubjectName = "搴撳瓨鐜伴噾";
        String garbledChelp = "搴撳瓨鐜伴噾";
        String untouchedCassItem = "搴撳瓨鐜伴噾";

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("560101");
        dto.setSubjectName(garbledSubjectName);
        dto.setChelp(garbledChelp);
        dto.setCmeasure("\u9879");
        dto.setCother("\u4e2d\u6587\u5907\u6ce8");
        dto.setBitem(1);
        dto.setCassItem(untouchedCassItem);

        FinanceAccountSubjectDetailVO detail = support.updateSubject("COMPANY_A", "560101", dto, "tester");

        assertEquals("\u5e93\u5b58\u73b0\u91d1", existing.getSubjectName());
        assertEquals("\u5e93\u5b58\u73b0\u91d1", existing.getChelp());
        assertEquals("\u9879", existing.getCmeasure());
        assertEquals("\u4e2d\u6587\u5907\u6ce8", existing.getCother());
        assertEquals(untouchedCassItem, existing.getCassItem());
        assertEquals("\u5e93\u5b58\u73b0\u91d1", detail.getSubjectName());
    }

    @Test
    void updateSubjectRejectsControlledFieldChangeWhenVoucherReferenced() {
        FinanceAccountSubject existing = subject("COMPANY_A", "1001", null, 1);
        existing.setSubjectName("\u5e93\u5b58\u73b0\u91d1");
        existing.setBperson(0);

        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(existing).thenReturn(existing);
        when(glAccvouchMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(0L);

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("1001");
        dto.setSubjectName("\u5e93\u5b58\u73b0\u91d1");
        dto.setSubjectCategory("ASSET");
        dto.setBperson(1);

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                support.updateSubject("COMPANY_A", "1001", dto, "tester")
        );
        assertEquals("\u5f53\u524d\u79d1\u76ee\u5df2\u88ab\u603b\u8d26\u5f15\u7528\uff0c\u4e0d\u80fd\u4fee\u6539\u53d7\u63a7\u5c5e\u6027", error.getMessage());
    }

    @Test
    void updateStatusRejectsDisableWhenEnabledChildrenRemain() {
        FinanceAccountSubject subject = subject("COMPANY_A", "1001", null, 1);

        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(subject);
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(1L);

        FinanceAccountSubjectStatusDTO dto = new FinanceAccountSubjectStatusDTO();
        dto.setStatus(0);

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                support.updateStatus("COMPANY_A", "1001", dto, "tester")
        );
        assertEquals("\u8bf7\u5148\u505c\u7528\u4e0b\u7ea7\u79d1\u76ee\uff0c\u518d\u505c\u7528\u5f53\u524d\u79d1\u76ee", error.getMessage());
    }

    @Test
    void updateCloseStatusRejectsCloseWhenOpenChildrenRemain() {
        FinanceAccountSubject subject = subject("COMPANY_A", "1001", null, 1);

        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(subject);
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(1L);

        FinanceAccountSubjectCloseDTO dto = new FinanceAccountSubjectCloseDTO();
        dto.setBclose(1);

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                support.updateCloseStatus("COMPANY_A", "1001", dto, "tester")
        );
        assertEquals("\u8bf7\u5148\u5c01\u5b58\u4e0b\u7ea7\u79d1\u76ee\uff0c\u518d\u5c01\u5b58\u5f53\u524d\u79d1\u76ee", error.getMessage());
    }

    @Test
    void updateSubjectIgnoresClientControlledFieldsAndKeepsAuthorityValues() {
        FinanceAccountSubject parent = subject("COMPANY_A", "6602", null, 1);
        parent.setSubjectName("\u7ba1\u7406\u8d39\u7528");
        parent.setSubjectCategory("PROFIT");
        parent.setBalanceDirection("DEBIT");

        FinanceAccountSubject existing = subject("COMPANY_A", "660201", "6602", 2);
        existing.setSubjectName("\u529e\u516c\u8d39");
        existing.setSubjectCategory("PROFIT");
        existing.setBalanceDirection("DEBIT");
        existing.setCclassany("PROFIT");
        existing.setBproperty(1);
        existing.setCbookType("GENERAL");
        existing.setLeafFlag(1);

        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(existing).thenReturn(parent).thenReturn(existing);
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(0L);

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("660201");
        dto.setSubjectName("\u529e\u516c\u8d39-\u66f4\u65b0");
        dto.setParentSubjectCode("1001");
        dto.setSubjectLevel(99);
        dto.setSubjectCategory("ASSET");
        dto.setLeafFlag(0);
        dto.setBproperty(0);
        dto.setCclassany("ASSET");
        dto.setCbookType("CASH");

        FinanceAccountSubjectDetailVO detail = support.updateSubject("COMPANY_A", "660201", dto, "tester");

        assertEquals("6602", existing.getParentSubjectCode());
        assertEquals(2, existing.getSubjectLevel());
        assertEquals("PROFIT", existing.getSubjectCategory());
        assertEquals("DEBIT", existing.getBalanceDirection());
        assertEquals(1, existing.getLeafFlag());
        assertEquals("PROFIT", existing.getCclassany());
        assertEquals(1, existing.getBproperty());
        assertEquals("GENERAL", existing.getCbookType());
        assertEquals("PROFIT", detail.getSubjectCategory());
        assertEquals("DEBIT", detail.getBalanceDirection());
    }

    @Test
    void getDerivedDefaultsReturnsUnmatchedForChildWithoutParent() {
        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(enabledCompany("COMPANY_A"));
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of());

        FinanceAccountSubjectDerivedDefaultsVO defaults = support.getDerivedDefaults("COMPANY_A", "990001");

        assertEquals("UNMATCHED", defaults.getMatchedBy());
        assertEquals("DEBIT", defaults.getBalanceDirection());
        assertEquals(1, defaults.getLeafFlag());
    }

    private SystemCompany enabledCompany(String companyId) {
        SystemCompany company = new SystemCompany();
        company.setCompanyId(companyId);
        company.setStatus(1);
        return company;
    }

    private FinanceAccountSubject subject(String companyId, String subjectCode, String parentCode, int level) {
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId(companyId);
        subject.setSubjectCode(subjectCode);
        subject.setParentSubjectCode(parentCode);
        subject.setSubjectLevel(level);
        subject.setSubjectName(subjectCode);
        subject.setBalanceDirection("DEBIT");
        subject.setSubjectCategory("ASSET");
        subject.setStatus(1);
        subject.setBclose(0);
        subject.setLeafFlag(parentCode == null ? 0 : 1);
        return subject;
    }
}
