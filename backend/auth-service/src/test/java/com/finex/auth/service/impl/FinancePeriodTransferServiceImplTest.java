package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.dto.FinancePeriodTransferPreviewDTO;
import com.finex.auth.dto.FinancePeriodTransferPreviewResultVO;
import com.finex.auth.dto.FinancePeriodTransferGenerateDTO;
import com.finex.auth.dto.FinancePeriodTransferRuleVO;
import com.finex.auth.dto.FinanceVoucherEntryDTO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSetModuleEnable;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinancePeriodTransferRule;
import com.finex.auth.entity.FinancePeriodTransferRuleLine;
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
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @Test
    void previewProfitKeepsRedDebitInItsOriginalColumn() {
        when(ruleMapper.selectById(1L)).thenReturn(buildProfitRule());
        when(accountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("4103", "本年利润", "EQUITY", 1, 1),
                buildSubject("560101", "办公费", "PROFIT", 2, 1)
        ));
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                buildVoucherRow("560101", new BigDecimal("100.00"), BigDecimal.ZERO, 1, 1),
                buildVoucherRow("560101", new BigDecimal("-150.00"), BigDecimal.ZERO, 2, 1)
        ));

        FinancePeriodTransferPreviewResultVO preview = service.preview(buildPreviewDTO(), 1L, "tester");

        assertEquals(1, preview.getGeneratedEntryCount());
        assertEquals(new BigDecimal("-50.00"), preview.getDetails().get(0).getAmount());
        assertEquals("贷方", preview.getDetails().get(0).getDirectionLabel());
        assertEquals(new BigDecimal("50.00"), preview.getTotalAmount());
    }

    @Test
    void generatedProfitVoucherKeepsRedDebitAndCreditInTheirOriginalColumns() {
        when(ruleMapper.selectById(1L)).thenReturn(buildProfitRule());
        when(accountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("4103", "本年利润", "EQUITY", 1, 1),
                buildSubject("560101", "办公费", "PROFIT", 2, 1),
                buildSubject("6001", "收入", "PROFIT", 1, 1)
        ));
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                buildVoucherRow("560101", new BigDecimal("-100.00"), BigDecimal.ZERO, 1, 1),
                buildVoucherRow("6001", BigDecimal.ZERO, new BigDecimal("-70.00"), 2, 1)
        ));

        FinancePeriodTransferPreviewResultVO preview = service.preview(buildPreviewDTO(), 1L, "tester");
        assertEquals(List.of("贷方", "借方"), preview.getDetails().stream().map(item -> item.getDirectionLabel()).toList());
        assertEquals(List.of(new BigDecimal("-100.00"), new BigDecimal("-70.00")),
                preview.getDetails().stream().map(item -> item.getAmount()).toList());
        assertEquals(new BigDecimal("170.00"), preview.getTotalAmount());

        FinanceVoucherSaveDTO voucher = generateVoucher(preview, 2);
        List<FinanceVoucherEntryDTO> entries = voucher.getEntries();
        assertEquals(new BigDecimal("-100.00"), entries.get(0).getMc());
        assertEquals(new BigDecimal("-100.00"), entries.get(1).getMd());
        assertEquals(new BigDecimal("-70.00"), entries.get(2).getMd());
        assertEquals(new BigDecimal("-70.00"), entries.get(3).getMc());
        assertEquals(entries.stream().map(FinanceVoucherEntryDTO::getMd).reduce(BigDecimal.ZERO, BigDecimal::add),
                entries.stream().map(FinanceVoucherEntryDTO::getMc).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void profitNetUsesTheNegativeColumnThatDrivesMixedMovement() {
        when(ruleMapper.selectById(1L)).thenReturn(buildProfitRule());
        when(accountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("4103", "本年利润", "EQUITY", 1, 1),
                buildSubject("560101", "办公费", "PROFIT", 2, 1),
                buildSubject("6001", "收入", "PROFIT", 1, 1)
        ));
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                buildVoucherRow("560101", new BigDecimal("-100.00"), BigDecimal.ZERO, 1, 1),
                buildVoucherRow("560101", BigDecimal.ZERO, new BigDecimal("50.00"), 2, 1),
                buildVoucherRow("6001", new BigDecimal("100.00"), BigDecimal.ZERO, 3, 1),
                buildVoucherRow("6001", BigDecimal.ZERO, new BigDecimal("-50.00"), 4, 1)
        ));

        FinancePeriodTransferPreviewResultVO preview = service.preview(buildPreviewDTO(), 1L, "tester");
        assertEquals(List.of(new BigDecimal("-150.00"), new BigDecimal("-150.00")),
                preview.getDetails().stream().map(item -> item.getAmount()).toList());
        assertEquals(List.of("贷方", "借方"), preview.getDetails().stream().map(item -> item.getDirectionLabel()).toList());
        List<FinanceVoucherEntryDTO> entries = generateVoucher(preview, 2).getEntries();
        assertEquals(new BigDecimal("-150.00"), entries.get(0).getMc());
        assertEquals(new BigDecimal("-150.00"), entries.get(1).getMd());
        assertEquals(new BigDecimal("-150.00"), entries.get(2).getMd());
        assertEquals(new BigDecimal("-150.00"), entries.get(3).getMc());
    }

    @Test
    void customOccurrenceMetricsKeepSignedDebitCreditAndNet() {
        FinancePeriodTransferRule rule = buildProfitRule();
        rule.setRuleType("CUSTOM");
        when(ruleMapper.selectById(1L)).thenReturn(rule);
        when(accountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("4103", "本年利润", "EQUITY", 1, 1),
                buildSubject("560101", "办公费", "PROFIT", 2, 1)
        ));
        GlAccsum balance = new GlAccsum();
        balance.setCompanyId("COMPANY_A");
        balance.setCcode("560101");
        balance.setMd(new BigDecimal("-80.00"));
        balance.setMc(new BigDecimal("-20.00"));
        balance.setMe(new BigDecimal("-60.00"));
        when(glAccsumMapper.selectList(any())).thenReturn(List.of(balance));
        when(ruleLineMapper.selectList(any())).thenReturn(List.of(
                customLine(1L, "CURRENT_DEBIT"),
                customLine(2L, "CURRENT_CREDIT"),
                customLine(3L, "CURRENT_NET")
        ));

        FinancePeriodTransferPreviewResultVO preview = service.preview(buildPreviewDTO(), 1L, "tester");
        assertEquals(List.of(new BigDecimal("-80.00"), new BigDecimal("-20.00"), new BigDecimal("-60.00")),
                preview.getDetails().stream().map(item -> item.getAmount()).toList());
        assertEquals(List.of("贷方", "借方", "贷方"),
                preview.getDetails().stream().map(item -> item.getDirectionLabel()).toList());
        assertEquals(new BigDecimal("160.00"), preview.getTotalAmount());
        List<FinanceVoucherEntryDTO> entries = generateVoucher(preview, 3).getEntries();
        assertEquals(new BigDecimal("-80.00"), entries.get(0).getMc());
        assertEquals(new BigDecimal("-20.00"), entries.get(2).getMd());
        assertEquals(new BigDecimal("-60.00"), entries.get(4).getMc());
    }

    @Test
    void customRedOccurrenceIsAvailableWhenAssistedEndingBalanceIsZero() {
        FinancePeriodTransferRule rule = buildProfitRule();
        rule.setRuleType("CUSTOM");
        when(ruleMapper.selectById(1L)).thenReturn(rule);
        when(accountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("4103", "本年利润", "EQUITY", 1, 1),
                buildSubject("560101", "办公费", "PROFIT", 2, 1)
        ));
        GlAccass balance = new GlAccass();
        balance.setCompanyId("COMPANY_A");
        balance.setCcode("560101");
        balance.setCdeptId("D1");
        balance.setMd(new BigDecimal("-100.00"));
        balance.setMc(BigDecimal.ZERO);
        balance.setMe(BigDecimal.ZERO);
        when(glAccassMapper.selectList(any())).thenReturn(List.of(balance));
        when(ruleLineMapper.selectList(any())).thenReturn(List.of(customLine(1L, "CURRENT_DEBIT")));

        FinancePeriodTransferPreviewResultVO preview = service.preview(buildPreviewDTO(), 1L, "tester");
        assertEquals(1, preview.getGeneratedEntryCount());
        assertEquals(new BigDecimal("-100.00"), preview.getDetails().get(0).getAmount());
        assertEquals("贷方", preview.getDetails().get(0).getDirectionLabel());
    }

    @Test
    void assistedProfitTransferKeepsEachDepartmentsSignedAmount() {
        FinancePeriodTransferRule rule = buildProfitRule();
        rule.setTransferGranularity("ASSIST");
        when(ruleMapper.selectById(1L)).thenReturn(rule);
        FinanceAccountSubject target = buildSubject("4103", "本年利润", "EQUITY", 1, 1);
        target.setBdept(1);
        when(accountSubjectMapper.selectList(any())).thenReturn(List.of(target,
                buildSubject("560101", "办公费", "PROFIT", 2, 1)));
        GlAccvouch first = buildVoucherRow("560101", new BigDecimal("-40.00"), BigDecimal.ZERO, 1, 1);
        first.setCdeptId("D1");
        GlAccvouch second = buildVoucherRow("560101", new BigDecimal("-60.00"), BigDecimal.ZERO, 2, 1);
        second.setCdeptId("D2");
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(first, second));

        FinancePeriodTransferPreviewResultVO preview = service.preview(buildPreviewDTO(), 1L, "tester");
        assertEquals(List.of(new BigDecimal("-40.00"), new BigDecimal("-60.00")),
                preview.getDetails().stream().map(item -> item.getAmount()).toList());
        List<FinanceVoucherEntryDTO> entries = generateVoucher(preview, 2).getEntries();
        assertEquals("D1", entries.get(0).getCdeptId());
        assertEquals("D1", entries.get(1).getCdeptId());
        assertEquals("D2", entries.get(2).getCdeptId());
        assertEquals("D2", entries.get(3).getCdeptId());
        assertEquals(new BigDecimal("-40.00"), entries.get(1).getMd());
        assertEquals(new BigDecimal("-60.00"), entries.get(3).getMd());
    }

    private FinancePeriodTransferRuleLine customLine(Long id, String metricType) {
        FinancePeriodTransferRuleLine line = new FinancePeriodTransferRuleLine();
        line.setId(id);
        line.setRuleId(1L);
        line.setLineNo(id.intValue());
        line.setSourceSubjectCode("560101");
        line.setTargetSubjectCode("4103");
        line.setMetricType(metricType);
        line.setRatio(BigDecimal.ONE);
        return line;
    }

    @Test
    void generateRejectsOldPreviewThatConvertedRedDebitToPositiveOppositeSide() {
        when(ruleMapper.selectById(1L)).thenReturn(buildProfitRule());
        when(accountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("4103", "本年利润", "EQUITY", 1, 1),
                buildSubject("560101", "办公费", "PROFIT", 2, 1)
        ));
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                buildVoucherRow("560101", new BigDecimal("-100.00"), BigDecimal.ZERO, 1, 1)
        ));
        FinancePeriodTransferPreviewResultVO preview = service.preview(buildPreviewDTO(), 1L, "tester");
        ArgumentCaptor<FinancePeriodTransferRun> runCaptor = ArgumentCaptor.forClass(FinancePeriodTransferRun.class);
        ArgumentCaptor<FinancePeriodTransferRunDetail> detailCaptor = ArgumentCaptor.forClass(FinancePeriodTransferRunDetail.class);
        verify(runMapper).insert(runCaptor.capture());
        verify(runDetailMapper).insert(detailCaptor.capture());
        FinancePeriodTransferRunDetail stale = detailCaptor.getValue();
        stale.setAmount(new BigDecimal("100.00"));
        stale.setDirection("DEBIT");
        when(runMapper.selectById(preview.getRunId())).thenReturn(runCaptor.getValue());
        when(runDetailMapper.selectList(any())).thenReturn(List.of(stale));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.generate(buildGenerateDTO(preview), 1L, "tester"));
        assertTrue(error.getMessage().contains("重新预览"));
        verify(financeVoucherService, never()).saveVoucher(any(), any(), any());
    }

    private FinanceVoucherSaveDTO generateVoucher(FinancePeriodTransferPreviewResultVO preview, int detailCount) {
        ArgumentCaptor<FinancePeriodTransferRun> runCaptor = ArgumentCaptor.forClass(FinancePeriodTransferRun.class);
        ArgumentCaptor<FinancePeriodTransferRunDetail> detailCaptor = ArgumentCaptor.forClass(FinancePeriodTransferRunDetail.class);
        verify(runMapper).insert(runCaptor.capture());
        verify(runDetailMapper, times(detailCount)).insert(detailCaptor.capture());
        when(runMapper.selectById(preview.getRunId())).thenReturn(runCaptor.getValue());
        when(runDetailMapper.selectList(any())).thenReturn(detailCaptor.getAllValues());
        when(runMapper.selectList(any())).thenReturn(List.of());
        FinanceVoucherSaveResultVO saveResult = new FinanceVoucherSaveResultVO();
        saveResult.setVoucherNo("COMPANY_A~2022~12~转~3");
        saveResult.setCsign("转");
        saveResult.setInoId(3);
        when(financeVoucherService.saveVoucher(any(), any(), any())).thenReturn(saveResult);
        service.generate(buildGenerateDTO(preview), 1L, "tester");
        ArgumentCaptor<FinanceVoucherSaveDTO> voucherCaptor = ArgumentCaptor.forClass(FinanceVoucherSaveDTO.class);
        verify(financeVoucherService).saveVoucher(voucherCaptor.capture(), any(), any());
        return voucherCaptor.getValue();
    }

    private FinancePeriodTransferGenerateDTO buildGenerateDTO(FinancePeriodTransferPreviewResultVO preview) {
        FinancePeriodTransferGenerateDTO dto = new FinancePeriodTransferGenerateDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setIyear(2022);
        dto.setIperiod(12);
        dto.setRunId(preview.getRunId());
        dto.setPreviewToken(preview.getPreviewToken());
        return dto;
    }

    @Test
    void generatedProfitVoucherReversesBothDebitAndCreditBalances() {
        FinancePeriodTransferRule rule = buildProfitRule();
        when(ruleMapper.selectById(1L)).thenReturn(rule);
        FinanceAccountSubject expense = buildSubject("560101", "办公费", "PROFIT", 2, 1);
        FinanceAccountSubject income = buildSubject("6001", "主营业务收入", "PROFIT", 1, 1);
        income.setBalanceDirection("CREDIT");
        when(accountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("4103", "本年利润", "EQUITY", 1, 1), expense, income
        ));
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                buildVoucherRow("560101", new BigDecimal("100.00"), BigDecimal.ZERO, 1, 1),
                buildVoucherRow("6001", BigDecimal.ZERO, new BigDecimal("200.00"), 2, 1)
        ));

        FinancePeriodTransferPreviewResultVO preview = service.preview(buildPreviewDTO(), 1L, "tester");
        assertEquals(List.of("贷方", "借方"), preview.getDetails().stream()
                .map(item -> item.getDirectionLabel()).toList());

        ArgumentCaptor<FinancePeriodTransferRun> runCaptor = ArgumentCaptor.forClass(FinancePeriodTransferRun.class);
        ArgumentCaptor<FinancePeriodTransferRunDetail> detailCaptor = ArgumentCaptor.forClass(FinancePeriodTransferRunDetail.class);
        verify(runMapper).insert(runCaptor.capture());
        verify(runDetailMapper, times(2)).insert(detailCaptor.capture());
        when(runMapper.selectById(preview.getRunId())).thenReturn(runCaptor.getValue());
        when(runDetailMapper.selectList(any())).thenReturn(detailCaptor.getAllValues());
        when(runMapper.selectList(any())).thenReturn(List.of());
        FinanceVoucherSaveResultVO saveResult = new FinanceVoucherSaveResultVO();
        saveResult.setVoucherNo("COMPANY_A~2022~12~转~3");
        saveResult.setCsign("转");
        saveResult.setInoId(3);
        when(financeVoucherService.saveVoucher(any(), any(), any())).thenReturn(saveResult);

        FinancePeriodTransferGenerateDTO generateDTO = new FinancePeriodTransferGenerateDTO();
        generateDTO.setCompanyId("COMPANY_A");
        generateDTO.setIyear(2022);
        generateDTO.setIperiod(12);
        generateDTO.setRunId(preview.getRunId());
        generateDTO.setPreviewToken(preview.getPreviewToken());
        service.generate(generateDTO, 1L, "tester");

        ArgumentCaptor<FinanceVoucherSaveDTO> voucherCaptor = ArgumentCaptor.forClass(FinanceVoucherSaveDTO.class);
        verify(financeVoucherService).saveVoucher(voucherCaptor.capture(), any(), any());
        List<FinanceVoucherEntryDTO> entries = voucherCaptor.getValue().getEntries();
        assertEquals(4, entries.size());
        assertEquals("560101", entries.get(0).getCcode());
        assertEquals(new BigDecimal("100.00"), entries.get(0).getMc());
        assertEquals(BigDecimal.ZERO.setScale(2), entries.get(0).getMd());
        assertEquals("4103", entries.get(1).getCcode());
        assertEquals(new BigDecimal("100.00"), entries.get(1).getMd());
        assertEquals("6001", entries.get(2).getCcode());
        assertEquals(new BigDecimal("200.00"), entries.get(2).getMd());
        assertEquals("4103", entries.get(3).getCcode());
        assertEquals(new BigDecimal("200.00"), entries.get(3).getMc());
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
