package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.dto.FinancePeriodTransferPreviewDTO;
import com.finex.auth.dto.FinancePeriodTransferPreviewResultVO;
import com.finex.auth.dto.FinancePeriodTransferRuleVO;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSetModuleEnable;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinancePeriodTransferRule;
import com.finex.auth.entity.FinancePeriodTransferRun;
import com.finex.auth.entity.FinancePeriodTransferRunDetail;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetModuleEnableMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePeriodTransferRuleLineMapper;
import com.finex.auth.mapper.FinancePeriodTransferRuleMapper;
import com.finex.auth.mapper.FinancePeriodTransferRunDetailMapper;
import com.finex.auth.mapper.FinancePeriodTransferRunMapper;
import com.finex.auth.mapper.FinancePeriodTransferVoucherLinkMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceVoucherService;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class FinancePeriodTransferServiceImplTest {

    @Mock
    private FinancePeriodTransferRuleMapper ruleMapper;
    @Mock
    private FinancePeriodTransferRuleLineMapper ruleLineMapper;
    @Mock
    private FinancePeriodTransferRunMapper runMapper;
    @Mock
    private FinancePeriodTransferRunDetailMapper runDetailMapper;
    @Mock
    private FinancePeriodTransferVoucherLinkMapper voucherLinkMapper;
    @Mock
    private FinanceAccountSubjectMapper accountSubjectMapper;
    @Mock
    private FinanceProjectClassMapper projectClassMapper;
    @Mock
    private FinanceProjectArchiveMapper projectArchiveMapper;
    @Mock
    private GlAccsumMapper glAccsumMapper;
    @Mock
    private GlAccassMapper glAccassMapper;
    @Mock
    private GlAccvouchMapper glAccvouchMapper;
    @Mock
    private FinanceVoucherService financeVoucherService;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private FinanceAccountSetMapper financeAccountSetMapper;
    @Mock
    private FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper;
    @Mock
    private FinancePeriodCloseMapper financePeriodCloseMapper;

    private FinancePeriodTransferServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FinancePeriodTransferRule.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FinancePeriodTransferRun.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FinancePeriodTransferRunDetail.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FinanceAccountSubject.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FinanceAccountSet.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GlAccsum.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GlAccass.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GlAccvouch.class);

        service = new FinancePeriodTransferServiceImpl(
                ruleMapper,
                ruleLineMapper,
                runMapper,
                runDetailMapper,
                voucherLinkMapper,
                accountSubjectMapper,
                projectClassMapper,
                projectArchiveMapper,
                glAccsumMapper,
                glAccassMapper,
                glAccvouchMapper,
                financeVoucherService,
                systemCompanyMapper,
                userMapper,
                financeAccountSetMapper,
                financeAccountSetModuleEnableMapper,
                financePeriodCloseMapper
        );

        lenient().when(financeAccountSetMapper.selectOne(any())).thenReturn(buildActiveAccountSet());
        lenient().when(financeAccountSetModuleEnableMapper.selectOne(any())).thenReturn(buildGeneralLedgerModule());
        lenient().when(projectClassMapper.selectList(any())).thenReturn(List.of());
        lenient().when(projectArchiveMapper.selectList(any())).thenReturn(List.of());
        lenient().when(glAccsumMapper.selectList(any())).thenReturn(List.of());
        lenient().when(glAccassMapper.selectList(any())).thenReturn(List.of());
        lenient().when(ruleLineMapper.selectList(any())).thenReturn(List.of());
        lenient().when(financePeriodCloseMapper.selectOne(any())).thenReturn(null);
        lenient().when(financeAccountSetMapper.selectCount(any())).thenReturn(1L);
        lenient().when(financePeriodCloseMapper.selectCount(any())).thenReturn(0L);
        lenient().doAnswer(invocation -> {
            FinancePeriodTransferRun run = invocation.getArgument(0);
            run.setId(99L);
            return 1;
        }).when(runMapper).insert(any(FinancePeriodTransferRun.class));
        lenient().doAnswer(invocation -> {
            FinancePeriodTransferRunDetail detail = invocation.getArgument(0);
            detail.setId(detail.getLineNo() == null ? 1L : detail.getLineNo().longValue());
            return 1;
        }).when(runDetailMapper).insert(any(FinancePeriodTransferRunDetail.class));
    }

    @Test
    void listRulesDefaultsProfitSubjectLevelToActiveAccountSetMaxLevel() {
        when(ruleMapper.selectList(any())).thenReturn(List.of());
        when(accountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("4103", "本年利润", "EQUITY", 1, 1),
                buildSubject("5601", "管理费用", "PROFIT", 1, 0),
                buildSubject("560101", "办公费", "PROFIT", 3, 1)
        ));

        List<FinancePeriodTransferRuleVO> rules = service.listRules("COMPANY_A");

        FinancePeriodTransferRuleVO profitRule = rules.stream()
                .filter(item -> "PROFIT".equals(item.getRuleType()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, profitRule.getSubjectLevel());
    }

    @Test
    void previewProfitUsesCurrentPeriodOccurrenceForNonLeafProfitSubject() {
        FinancePeriodTransferRule rule = buildProfitRule();
        rule.setSubjectLevel(null);
        when(ruleMapper.selectById(1L)).thenReturn(rule);
        when(accountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("4103", "本年利润", "EQUITY", 1, 1),
                buildSubject("5601", "管理费用", "PROFIT", 1, 0),
                buildSubject("1001", "库存现金", "ASSET", 1, 1)
        ));
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                buildVoucherRow("5601", new BigDecimal("100.00"), BigDecimal.ZERO, 1, 1),
                buildVoucherRow("1001", BigDecimal.ZERO, new BigDecimal("100.00"), 2, 1)
        ));

        FinancePeriodTransferPreviewResultVO preview = service.preview(buildPreviewDTO(), 1L, "tester");

        assertEquals(1, preview.getGeneratedEntryCount());
        assertEquals(0, preview.getSkippedEntryCount());
        assertEquals(1, preview.getDetails().size());
        assertEquals("5601", preview.getDetails().get(0).getSourceSubjectCode());
        assertEquals("本期净发生额", preview.getDetails().get(0).getMetricTypeLabel());
        assertEquals("贷方", preview.getDetails().get(0).getDirectionLabel());
        assertEquals(new BigDecimal("100.00"), preview.getDetails().get(0).getAmount());
    }

    @Test
    void previewProfitFiltersZeroNetOccurrenceWithoutPlaceholderRows() {
        when(ruleMapper.selectById(1L)).thenReturn(buildProfitRule());
        when(accountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("4103", "本年利润", "EQUITY", 1, 1),
                buildSubject("560101", "办公费", "PROFIT", 2, 1)
        ));
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                buildVoucherRow("560101", new BigDecimal("100.00"), BigDecimal.ZERO, 1, 1),
                buildVoucherRow("560101", BigDecimal.ZERO, new BigDecimal("100.00"), 2, 1)
        ));

        FinancePeriodTransferPreviewResultVO preview = service.preview(buildPreviewDTO(), 1L, "tester");

        assertEquals(0, preview.getGeneratedEntryCount());
        assertEquals(0, preview.getSkippedEntryCount());
        assertTrue(preview.getDetails().isEmpty());
        assertEquals("本期无可结转损益发生额", preview.getMessage());
    }

    private FinancePeriodTransferPreviewDTO buildPreviewDTO() {
        FinancePeriodTransferPreviewDTO dto = new FinancePeriodTransferPreviewDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setIyear(2022);
        dto.setIperiod(12);
        dto.setRuleId(1L);
        return dto;
    }

    private FinancePeriodTransferRule buildProfitRule() {
        FinancePeriodTransferRule rule = new FinancePeriodTransferRule();
        rule.setId(1L);
        rule.setCompanyId("COMPANY_A");
        rule.setRuleType("PROFIT");
        rule.setRuleName("期间损益结转");
        rule.setEnabled(1);
        rule.setVoucherType("转");
        rule.setIncludeUnposted(0);
        rule.setTransferGranularity("SUBJECT");
        rule.setTargetSubjectCode("4103");
        rule.setTargetSubjectName("本年利润");
        return rule;
    }

    private FinanceAccountSet buildActiveAccountSet() {
        FinanceAccountSet accountSet = new FinanceAccountSet();
        accountSet.setCompanyId("COMPANY_A");
        accountSet.setStatus("ACTIVE");
        return accountSet;
    }

    private FinanceAccountSetModuleEnable buildGeneralLedgerModule() {
        FinanceAccountSetModuleEnable module = new FinanceAccountSetModuleEnable();
        module.setId(1L);
        module.setCompanyId("COMPANY_A");
        module.setModuleCode("GENERAL_LEDGER");
        module.setEnabled(1);
        return module;
    }

    private FinanceAccountSubject buildSubject(String code, String name, String category, int subjectLevel, int leafFlag) {
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setId((long) code.hashCode());
        subject.setCompanyId("COMPANY_A");
        subject.setSubjectCode(code);
        subject.setSubjectName(name);
        subject.setSubjectCategory(category);
        subject.setSubjectLevel(subjectLevel);
        subject.setLeafFlag(leafFlag);
        subject.setStatus(1);
        subject.setBclose(0);
        subject.setBalanceDirection("PROFIT".equals(category) ? "DEBIT" : "CREDIT");
        return subject;
    }

    private GlAccvouch buildVoucherRow(String subjectCode, BigDecimal md, BigDecimal mc, int inid, int ibook) {
        GlAccvouch row = new GlAccvouch();
        row.setId(inid);
        row.setCompanyId("COMPANY_A");
        row.setIyear(2022);
        row.setIperiod(12);
        row.setCsign("记");
        row.setInoId(2);
        row.setInid(inid);
        row.setCcode(subjectCode);
        row.setMd(md);
        row.setMc(mc);
        row.setIbook(ibook);
        return row;
    }
}
