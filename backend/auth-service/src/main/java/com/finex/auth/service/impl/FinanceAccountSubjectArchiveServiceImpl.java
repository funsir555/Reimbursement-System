package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectCloseDTO;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectMetaVO;
import com.finex.auth.dto.FinanceAccountSubjectOptionVO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.dto.FinanceAccountSubjectStatusDTO;
import com.finex.auth.dto.FinanceAccountSubjectSummaryVO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.service.FinanceAccountSubjectArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FinanceAccountSubjectArchiveServiceImpl implements FinanceAccountSubjectArchiveService {

    private static final String CATEGORY_ASSET = "ASSET";
    private static final String CATEGORY_LIABILITY = "LIABILITY";
    private static final String CATEGORY_EQUITY = "EQUITY";
    private static final String CATEGORY_COST = "COST";
    private static final String CATEGORY_PROFIT = "PROFIT";
    private static final String BALANCE_DEBIT = "DEBIT";
    private static final String BALANCE_CREDIT = "CREDIT";

    private final FinanceAccountSubjectMapper financeAccountSubjectMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final GlAccvouchMapper glAccvouchMapper;
    private final ObjectMapper objectMapper;

    @Override
    public FinanceAccountSubjectMetaVO getMeta() {
        FinanceAccountSubjectMetaVO meta = new FinanceAccountSubjectMetaVO();
        meta.setSubjectCategoryOptions(List.of(
                option(CATEGORY_ASSET, "资产"),
                option(CATEGORY_LIABILITY, "负债"),
                option(CATEGORY_EQUITY, "权益"),
                option(CATEGORY_COST, "成本"),
                option(CATEGORY_PROFIT, "损益")
        ));
        meta.setStatusOptions(List.of(option("1", "启用"), option("0", "停用")));
        meta.setCloseStatusOptions(List.of(option("0", "未封存"), option("1", "已封存")));
        meta.setYesNoOptions(List.of(option("1", "是"), option("0", "否")));
        return meta;
    }

    @Override
    public List<FinanceAccountSubjectSummaryVO> listSubjects(String companyId, String keyword, String subjectCategory, Integer status, Integer bclose) {
        QueryWrapper<FinanceAccountSubject> query = new QueryWrapper<>();
        query.eq("company_id", requireCompanyId(companyId));
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("subject_code", normalizedKeyword)
                    .or()
                    .like("subject_name", normalizedKeyword)
                    .or()
                    .like("chelp", normalizedKeyword));
        }
        String normalizedCategory = trimToNull(subjectCategory);
        if (normalizedCategory != null) {
            query.eq("subject_category", normalizedCategory);
        }
        if (status != null) {
            query.eq("status", normalizeFlag(status, 1));
        }
        if (bclose != null) {
            query.eq("bclose", normalizeFlag(bclose, 0));
        }
        query.orderByAsc("subject_level", "sort_order", "subject_code", "id");
        return buildTree(financeAccountSubjectMapper.selectList(query));
    }

    @Override
    public FinanceAccountSubjectDetailVO getSubjectDetail(String companyId, String subjectCode) {
        return toDetail(requireSubject(companyId, subjectCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceAccountSubjectDetailVO createSubject(String companyId, FinanceAccountSubjectSaveDTO dto, String operatorName) {
        validateSavePayload(dto);
        String normalizedCompanyId = requireCompanyId(companyId);
        requireEnabledCompany(normalizedCompanyId);

        String subjectCode = requireText(dto.getSubjectCode(), "科目编码不能为空");
        if (financeAccountSubjectMapper.selectOne(
                Wrappers.<FinanceAccountSubject>lambdaQuery()
                        .eq(FinanceAccountSubject::getCompanyId, normalizedCompanyId)
                        .eq(FinanceAccountSubject::getSubjectCode, subjectCode)
                        .last("limit 1")
        ) != null) {
            throw new IllegalStateException("当前公司下已存在相同科目编码");
        }

        FinanceAccountSubject parent = null;
        String parentSubjectCode = trimToNull(dto.getParentSubjectCode());
        if (parentSubjectCode != null) {
            parent = requireSubject(normalizedCompanyId, parentSubjectCode);
            if (!subjectCode.startsWith(parent.getSubjectCode())) {
                throw new IllegalArgumentException("子科目编码必须以前级科目编码为前缀");
            }
            if (!isEnabled(parent.getStatus()) || isClosed(parent.getBclose())) {
                throw new IllegalStateException("上级科目未启用或已封存，不能新增子科目");
            }
        }

        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId(normalizedCompanyId);
        subject.setSubjectCode(subjectCode);
        subject.setParentSubjectCode(parentSubjectCode);
        subject.setSubjectLevel(resolveSubjectLevel(dto, parent));
        subject.setSubjectName(requireText(dto.getSubjectName(), "科目名称不能为空"));
        subject.setSubjectCategory(resolveCategory(dto.getSubjectCategory()));
        subject.setBalanceDirection(resolveBalanceDirection(subjectCode, subject.getSubjectCategory(), parent));
        subject.setTemplateCode(null);
        subject.setSortOrder(resolveNextSortOrder(normalizedCompanyId, parentSubjectCode));
        applyMutableFields(subject, dto, true);
        if (parent != null) {
            parent.setLeafFlag(0);
            financeAccountSubjectMapper.updateById(parent);
        }
        financeAccountSubjectMapper.insert(subject);
        return toDetail(requireSubject(normalizedCompanyId, subjectCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceAccountSubjectDetailVO updateSubject(String companyId, String subjectCode, FinanceAccountSubjectSaveDTO dto, String operatorName) {
        validateSavePayload(dto);
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedSubjectCode = requireText(subjectCode, "科目编码不能为空");
        FinanceAccountSubject existing = requireSubject(normalizedCompanyId, normalizedSubjectCode);

        if (!Objects.equals(normalizedSubjectCode, requireText(dto.getSubjectCode(), "科目编码不能为空"))) {
            throw new IllegalArgumentException("首版不支持修改科目编码");
        }
        String payloadParentCode = trimToNull(dto.getParentSubjectCode());
        if (!Objects.equals(trimToNull(existing.getParentSubjectCode()), payloadParentCode)) {
            throw new IllegalArgumentException("首版不支持修改上级科目");
        }
        Integer payloadLevel = dto.getSubjectLevel();
        if (payloadLevel != null && !Objects.equals(existing.getSubjectLevel(), payloadLevel)) {
            throw new IllegalArgumentException("首版不支持修改科目级次");
        }

        FinanceAccountSubject snapshot = cloneSubject(existing);
        applyMutableFields(existing, dto, false);
        if (hasChildren(normalizedCompanyId, normalizedSubjectCode)) {
            existing.setLeafFlag(0);
        }
        validateControlledFieldChanges(snapshot, existing);
        financeAccountSubjectMapper.updateById(existing);
        return toDetail(requireSubject(normalizedCompanyId, normalizedSubjectCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatus(String companyId, String subjectCode, FinanceAccountSubjectStatusDTO dto, String operatorName) {
        FinanceAccountSubject subject = requireSubject(companyId, subjectCode);
        int nextStatus = normalizeFlag(dto == null ? null : dto.getStatus(), 1);
        if (nextStatus == 1) {
            validateParentAvailableForEnable(subject);
        } else if (hasEnabledChildren(subject.getCompanyId(), subject.getSubjectCode())) {
            throw new IllegalStateException("请先停用下级科目，再停用当前科目");
        }
        subject.setStatus(nextStatus);
        financeAccountSubjectMapper.updateById(subject);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCloseStatus(String companyId, String subjectCode, FinanceAccountSubjectCloseDTO dto, String operatorName) {
        FinanceAccountSubject subject = requireSubject(companyId, subjectCode);
        int nextClose = normalizeFlag(dto == null ? null : dto.getBclose(), 0);
        if (nextClose == 0) {
            validateParentAvailableForEnable(subject);
        } else if (hasOpenChildren(subject.getCompanyId(), subject.getSubjectCode())) {
            throw new IllegalStateException("请先封存下级科目，再封存当前科目");
        }
        subject.setBclose(nextClose);
        financeAccountSubjectMapper.updateById(subject);
        return Boolean.TRUE;
    }

    private List<FinanceAccountSubjectSummaryVO> buildTree(List<FinanceAccountSubject> subjects) {
        Map<String, FinanceAccountSubjectSummaryVO> summaryByCode = new LinkedHashMap<>();
        for (FinanceAccountSubject subject : subjects) {
            FinanceAccountSubjectSummaryVO summary = toSummary(subject);
            summaryByCode.put(subject.getSubjectCode(), summary);
        }
        List<FinanceAccountSubjectSummaryVO> roots = new ArrayList<>();
        for (FinanceAccountSubject subject : subjects) {
            FinanceAccountSubjectSummaryVO summary = summaryByCode.get(subject.getSubjectCode());
            String parentCode = trimToNull(subject.getParentSubjectCode());
            FinanceAccountSubjectSummaryVO parent = parentCode == null ? null : summaryByCode.get(parentCode);
            if (parent == null) {
                roots.add(summary);
            } else {
                parent.getChildren().add(summary);
                parent.setHasChildren(true);
            }
        }
        return roots;
    }

    private FinanceAccountSubjectSummaryVO toSummary(FinanceAccountSubject subject) {
        FinanceAccountSubjectSummaryVO summary = new FinanceAccountSubjectSummaryVO();
        summary.setSubjectCode(subject.getSubjectCode());
        summary.setSubjectName(subject.getSubjectName());
        summary.setParentSubjectCode(subject.getParentSubjectCode());
        summary.setSubjectLevel(subject.getSubjectLevel());
        summary.setBalanceDirection(subject.getBalanceDirection());
        summary.setSubjectCategory(subject.getSubjectCategory());
        summary.setChelp(subject.getChelp());
        summary.setLeafFlag(subject.getLeafFlag());
        summary.setStatus(subject.getStatus());
        summary.setBclose(subject.getBclose());
        summary.setBperson(subject.getBperson());
        summary.setBcus(subject.getBcus());
        summary.setBsup(subject.getBsup());
        summary.setBdept(subject.getBdept());
        summary.setBitem(subject.getBitem());
        summary.setBcash(subject.getBcash());
        summary.setBbank(subject.getBbank());
        summary.setBr(subject.getBr());
        summary.setBe(subject.getBe());
        summary.setTemplateCode(subject.getTemplateCode());
        summary.setSortOrder(subject.getSortOrder());
        summary.setUpdatedAt(subject.getUpdatedAt());
        summary.setBdC(subject.getBdC());
        summary.setHasChildren(hasChildren(subject.getCompanyId(), subject.getSubjectCode()));
        summary.setAuxiliarySummary(buildAuxiliarySummary(subject));
        summary.setCashBankSummary(buildCashBankSummary(subject));
        if (summary.getChildren() == null) {
            summary.setChildren(new ArrayList<>());
        }
        return summary;
    }

    private FinanceAccountSubjectDetailVO toDetail(FinanceAccountSubject subject) {
        FinanceAccountSubjectDetailVO detail = new FinanceAccountSubjectDetailVO();
        detail.setId(subject.getId());
        detail.setCompanyId(subject.getCompanyId());
        detail.setSubjectCode(subject.getSubjectCode());
        detail.setSubjectName(subject.getSubjectName());
        detail.setParentSubjectCode(subject.getParentSubjectCode());
        detail.setSubjectLevel(subject.getSubjectLevel());
        detail.setBalanceDirection(subject.getBalanceDirection());
        detail.setSubjectCategory(subject.getSubjectCategory());
        detail.setCclassany(subject.getCclassany());
        detail.setBproperty(subject.getBproperty());
        detail.setCbookType(subject.getCbookType());
        detail.setChelp(subject.getChelp());
        detail.setCexchName(subject.getCexchName());
        detail.setCmeasure(subject.getCmeasure());
        detail.setBperson(subject.getBperson());
        detail.setBcus(subject.getBcus());
        detail.setBsup(subject.getBsup());
        detail.setBdept(subject.getBdept());
        detail.setBitem(subject.getBitem());
        detail.setCassItem(subject.getCassItem());
        detail.setBr(subject.getBr());
        detail.setBe(subject.getBe());
        detail.setCgather(subject.getCgather());
        detail.setLeafFlag(subject.getLeafFlag());
        detail.setBexchange(subject.getBexchange());
        detail.setBcash(subject.getBcash());
        detail.setBbank(subject.getBbank());
        detail.setBused(subject.getBused());
        detail.setBdC(subject.getBdC());
        detail.setDbegin(subject.getDbegin());
        detail.setDend(subject.getDend());
        detail.setItrans(subject.getItrans());
        detail.setBclose(subject.getBclose());
        detail.setCother(subject.getCother());
        detail.setIotherused(subject.getIotherused());
        detail.setBReport(subject.getBReport());
        detail.setBGCJS(subject.getBGCJS());
        detail.setBCashItem(subject.getBCashItem());
        detail.setIViewItem(subject.getIViewItem());
        detail.setBcDefine1(subject.getBcDefine1());
        detail.setBcDefine2(subject.getBcDefine2());
        detail.setBcDefine3(subject.getBcDefine3());
        detail.setBcDefine4(subject.getBcDefine4());
        detail.setBcDefine5(subject.getBcDefine5());
        detail.setBcDefine6(subject.getBcDefine6());
        detail.setBcDefine7(subject.getBcDefine7());
        detail.setBcDefine8(subject.getBcDefine8());
        detail.setBcDefine9(subject.getBcDefine9());
        detail.setBcDefine10(subject.getBcDefine10());
        detail.setBcDefine11(subject.getBcDefine11());
        detail.setBcDefine12(subject.getBcDefine12());
        detail.setBcDefine13(subject.getBcDefine13());
        detail.setBcDefine14(subject.getBcDefine14());
        detail.setBcDefine15(subject.getBcDefine15());
        detail.setBcDefine16(subject.getBcDefine16());
        detail.setStatus(subject.getStatus());
        detail.setTemplateCode(subject.getTemplateCode());
        detail.setSortOrder(subject.getSortOrder());
        detail.setCreatedAt(subject.getCreatedAt());
        detail.setUpdatedAt(subject.getUpdatedAt());
        detail.setHasChildren(hasChildren(subject.getCompanyId(), subject.getSubjectCode()));
        return detail;
    }

    private void applyMutableFields(FinanceAccountSubject target, FinanceAccountSubjectSaveDTO dto, boolean createMode) {
        target.setSubjectName(requireText(dto.getSubjectName(), "科目名称不能为空"));
        target.setSubjectCategory(resolveCategory(dto.getSubjectCategory()));
        target.setCclassany(defaultText(dto.getCclassany(), target.getSubjectCategory()));
        target.setBproperty(normalizeFlag(dto.getBproperty(), resolveDefaultProperty(target.getBalanceDirection())));
        target.setCbookType(defaultText(dto.getCbookType(), defaultBookType(target)));
        target.setChelp(defaultText(dto.getChelp(), target.getSubjectCode()));
        target.setCexchName(defaultText(dto.getCexchName(), "CNY"));
        target.setCmeasure(trimToNull(dto.getCmeasure()));
        target.setBperson(normalizeFlag(dto.getBperson(), 0));
        target.setBcus(normalizeFlag(dto.getBcus(), 0));
        target.setBsup(normalizeFlag(dto.getBsup(), 0));
        target.setBdept(normalizeFlag(dto.getBdept(), 0));
        target.setBitem(normalizeFlag(dto.getBitem(), 0));
        target.setCassItem(trimToNull(dto.getCassItem()));
        target.setBr(normalizeFlag(dto.getBr(), defaultCashBookFlag(target)));
        target.setBe(normalizeFlag(dto.getBe(), defaultBankBookFlag(target)));
        target.setCgather(defaultText(dto.getCgather(), "0"));
        target.setLeafFlag(normalizeLeafFlag(dto.getLeafFlag(), createMode ? 1 : target.getLeafFlag()));
        target.setBexchange(normalizeFlag(dto.getBexchange(), 0));
        target.setBcash(normalizeFlag(dto.getBcash(), defaultCashSubjectFlag(target)));
        target.setBbank(normalizeFlag(dto.getBbank(), defaultBankSubjectFlag(target)));
        target.setBused(normalizeFlag(dto.getBused(), target.getBbank() == 1 ? 1 : 0));
        target.setBdC(normalizeFlag(dto.getBdC(), target.getBbank() == 1 ? 1 : 0));
        target.setDbegin(dto.getDbegin());
        target.setDend(dto.getDend());
        target.setItrans(dto.getItrans() == null ? 0 : dto.getItrans());
        target.setBclose(normalizeFlag(dto.getBclose(), 0));
        target.setCother(trimToNull(dto.getCother()));
        target.setIotherused(dto.getIotherused() == null ? 0 : Math.max(dto.getIotherused(), 0));
        target.setBReport(normalizeFlag(dto.getBReport(), 0));
        target.setBGCJS(normalizeFlag(dto.getBGCJS(), 0));
        target.setBCashItem(normalizeFlag(dto.getBCashItem(), 0));
        target.setIViewItem(dto.getIViewItem() == null ? 0 : Math.max(dto.getIViewItem(), 0));
        target.setBcDefine1(defaultDefineFlag(target.getBcDefine1()));
        target.setBcDefine2(defaultDefineFlag(target.getBcDefine2()));
        target.setBcDefine3(defaultDefineFlag(target.getBcDefine3()));
        target.setBcDefine4(defaultDefineFlag(target.getBcDefine4()));
        target.setBcDefine5(defaultDefineFlag(target.getBcDefine5()));
        target.setBcDefine6(defaultDefineFlag(target.getBcDefine6()));
        target.setBcDefine7(defaultDefineFlag(target.getBcDefine7()));
        target.setBcDefine8(defaultDefineFlag(target.getBcDefine8()));
        target.setBcDefine9(defaultDefineFlag(target.getBcDefine9()));
        target.setBcDefine10(defaultDefineFlag(target.getBcDefine10()));
        target.setBcDefine11(defaultDefineFlag(target.getBcDefine11()));
        target.setBcDefine12(defaultDefineFlag(target.getBcDefine12()));
        target.setBcDefine13(defaultDefineFlag(target.getBcDefine13()));
        target.setBcDefine14(defaultDefineFlag(target.getBcDefine14()));
        target.setBcDefine15(defaultDefineFlag(target.getBcDefine15()));
        target.setBcDefine16(defaultDefineFlag(target.getBcDefine16()));
        target.setStatus(createMode ? 1 : normalizeFlag(target.getStatus(), 1));
        target.setUpdatedAt(LocalDateTime.now());
        if (createMode) {
            target.setCreatedAt(LocalDateTime.now());
        }
    }

    private void validateControlledFieldChanges(FinanceAccountSubject before, FinanceAccountSubject after) {
        if (!hasVoucherReference(before.getCompanyId(), before.getSubjectCode())) {
            return;
        }
        if (!Objects.equals(before.getSubjectCategory(), after.getSubjectCategory())
                || !Objects.equals(before.getBalanceDirection(), after.getBalanceDirection())
                || !Objects.equals(before.getLeafFlag(), after.getLeafFlag())
                || !Objects.equals(before.getBperson(), after.getBperson())
                || !Objects.equals(before.getBcus(), after.getBcus())
                || !Objects.equals(before.getBsup(), after.getBsup())
                || !Objects.equals(before.getBdept(), after.getBdept())
                || !Objects.equals(before.getBitem(), after.getBitem())
                || !Objects.equals(before.getCassItem(), after.getCassItem())
                || !Objects.equals(before.getBcash(), after.getBcash())
                || !Objects.equals(before.getBbank(), after.getBbank())
                || !Objects.equals(before.getBused(), after.getBused())
                || !Objects.equals(before.getBdC(), after.getBdC())
                || !Objects.equals(before.getBexchange(), after.getBexchange())
                || !Objects.equals(before.getItrans(), after.getItrans())) {
            throw new IllegalStateException("当前科目已被总账引用，不能修改受控属性");
        }
    }

    private Integer resolveSubjectLevel(FinanceAccountSubjectSaveDTO dto, FinanceAccountSubject parent) {
        if (parent != null) {
            int computedLevel = (parent.getSubjectLevel() == null ? 1 : parent.getSubjectLevel()) + 1;
            if (dto.getSubjectLevel() != null && !Objects.equals(dto.getSubjectLevel(), computedLevel)) {
                throw new IllegalArgumentException("子科目级次必须与上级科目层级匹配");
            }
            return computedLevel;
        }
        if (dto.getSubjectLevel() != null && dto.getSubjectLevel() != 1) {
            throw new IllegalArgumentException("根科目级次必须为 1");
        }
        return 1;
    }

    private String resolveBalanceDirection(String subjectCode, String subjectCategory, FinanceAccountSubject parent) {
        if (parent != null && trimToNull(parent.getBalanceDirection()) != null) {
            return parent.getBalanceDirection();
        }
        String code = trimToNull(subjectCode);
        if (code != null && !code.isEmpty()) {
            char first = code.charAt(0);
            if (first == '1' || first == '4' || first == '6') {
                return BALANCE_DEBIT;
            }
            if (first == '2' || first == '3' || first == '5') {
                return BALANCE_CREDIT;
            }
        }
        return switch (resolveCategory(subjectCategory)) {
            case CATEGORY_ASSET, CATEGORY_COST -> BALANCE_DEBIT;
            case CATEGORY_LIABILITY, CATEGORY_EQUITY -> BALANCE_CREDIT;
            default -> BALANCE_CREDIT;
        };
    }

    private String resolveCategory(String subjectCategory) {
        String normalized = trimToNull(subjectCategory);
        if (normalized == null) {
            return CATEGORY_ASSET;
        }
        Set<String> allowed = Set.of(CATEGORY_ASSET, CATEGORY_LIABILITY, CATEGORY_EQUITY, CATEGORY_COST, CATEGORY_PROFIT);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("科目类别不合法");
        }
        return normalized;
    }

    private Integer resolveNextSortOrder(String companyId, String parentSubjectCode) {
        var query = Wrappers.<FinanceAccountSubject>lambdaQuery()
                .eq(FinanceAccountSubject::getCompanyId, companyId)
                .orderByDesc(FinanceAccountSubject::getSortOrder, FinanceAccountSubject::getId)
                .last("limit 1");
        if (parentSubjectCode == null) {
            query.isNull(FinanceAccountSubject::getParentSubjectCode);
        } else {
            query.eq(FinanceAccountSubject::getParentSubjectCode, parentSubjectCode);
        }
        List<FinanceAccountSubject> siblings = financeAccountSubjectMapper.selectList(query);
        if (siblings.isEmpty()) {
            return 10;
        }
        Integer maxSortOrder = siblings.get(0).getSortOrder();
        return (maxSortOrder == null ? 0 : maxSortOrder) + 10;
    }

    private void validateSavePayload(FinanceAccountSubjectSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("科目数据不能为空");
        }
        requireText(dto.getSubjectCode(), "科目编码不能为空");
        requireText(dto.getSubjectName(), "科目名称不能为空");
    }

    private void validateParentAvailableForEnable(FinanceAccountSubject subject) {
        String parentSubjectCode = trimToNull(subject.getParentSubjectCode());
        if (parentSubjectCode == null) {
            return;
        }
        FinanceAccountSubject parent = requireSubject(subject.getCompanyId(), parentSubjectCode);
        if (!isEnabled(parent.getStatus()) || isClosed(parent.getBclose())) {
            throw new IllegalStateException("上级科目未启用或已封存，不能执行当前操作");
        }
    }

    private String buildAuxiliarySummary(FinanceAccountSubject subject) {
        List<String> tags = new ArrayList<>();
        if (isEnabled(subject.getBperson())) {
            tags.add("个人");
        }
        if (isEnabled(subject.getBcus())) {
            tags.add("客户");
        }
        if (isEnabled(subject.getBsup())) {
            tags.add("供应商");
        }
        if (isEnabled(subject.getBdept())) {
            tags.add("部门");
        }
        if (isEnabled(subject.getBitem())) {
            tags.add("项目");
        }
        return tags.isEmpty() ? "无" : String.join(" / ", tags);
    }

    private String buildCashBankSummary(FinanceAccountSubject subject) {
        List<String> tags = new ArrayList<>();
        if (isEnabled(subject.getBcash())) {
            tags.add("现金科目");
        }
        if (isEnabled(subject.getBbank())) {
            tags.add("银行科目");
        }
        if (isEnabled(subject.getBr())) {
            tags.add("日记账");
        }
        if (isEnabled(subject.getBe())) {
            tags.add("银行账");
        }
        return tags.isEmpty() ? "无" : String.join(" / ", tags);
    }

    private FinanceAccountSubject requireSubject(String companyId, String subjectCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedSubjectCode = requireText(subjectCode, "科目编码不能为空");
        FinanceAccountSubject subject = financeAccountSubjectMapper.selectOne(
                Wrappers.<FinanceAccountSubject>lambdaQuery()
                        .eq(FinanceAccountSubject::getCompanyId, normalizedCompanyId)
                        .eq(FinanceAccountSubject::getSubjectCode, normalizedSubjectCode)
                        .last("limit 1")
        );
        if (subject == null) {
            throw new IllegalStateException("会计科目不存在");
        }
        return subject;
    }

    private void requireEnabledCompany(String companyId) {
        SystemCompany company = systemCompanyMapper.selectById(companyId);
        if (company == null || !Objects.equals(company.getStatus(), 1)) {
            throw new IllegalStateException("当前公司不存在或已停用");
        }
    }

    private boolean hasChildren(String companyId, String subjectCode) {
        Long count = financeAccountSubjectMapper.selectCount(
                Wrappers.<FinanceAccountSubject>lambdaQuery()
                        .eq(FinanceAccountSubject::getCompanyId, companyId)
                        .eq(FinanceAccountSubject::getParentSubjectCode, subjectCode)
        );
        return count != null && count > 0;
    }

    private boolean hasEnabledChildren(String companyId, String subjectCode) {
        Long count = financeAccountSubjectMapper.selectCount(
                Wrappers.<FinanceAccountSubject>lambdaQuery()
                        .eq(FinanceAccountSubject::getCompanyId, companyId)
                        .eq(FinanceAccountSubject::getParentSubjectCode, subjectCode)
                        .eq(FinanceAccountSubject::getStatus, 1)
        );
        return count != null && count > 0;
    }

    private boolean hasOpenChildren(String companyId, String subjectCode) {
        Long count = financeAccountSubjectMapper.selectCount(
                Wrappers.<FinanceAccountSubject>lambdaQuery()
                        .eq(FinanceAccountSubject::getCompanyId, companyId)
                        .eq(FinanceAccountSubject::getParentSubjectCode, subjectCode)
                        .eq(FinanceAccountSubject::getBclose, 0)
        );
        return count != null && count > 0;
    }

    private boolean hasVoucherReference(String companyId, String subjectCode) {
        Long count = glAccvouchMapper.selectCount(
                Wrappers.lambdaQuery(com.finex.auth.entity.GlAccvouch.class)
                        .eq(com.finex.auth.entity.GlAccvouch::getCompanyId, companyId)
                        .eq(com.finex.auth.entity.GlAccvouch::getCcode, subjectCode)
        );
        return count != null && count > 0;
    }

    private FinanceAccountSubject cloneSubject(FinanceAccountSubject source) {
        return objectMapper.convertValue(source, FinanceAccountSubject.class);
    }

    private String defaultBookType(FinanceAccountSubject subject) {
        if (isEnabled(subject.getBcash())) {
            return "CASH";
        }
        if (isEnabled(subject.getBbank())) {
            return "BANK";
        }
        return "GENERAL";
    }

    private int resolveDefaultProperty(String balanceDirection) {
        return BALANCE_DEBIT.equalsIgnoreCase(trimToNull(balanceDirection)) ? 1 : 0;
    }

    private int defaultCashSubjectFlag(FinanceAccountSubject subject) {
        return startsWithSubjectCode(subject, "1001") ? 1 : 0;
    }

    private int defaultBankSubjectFlag(FinanceAccountSubject subject) {
        return startsWithSubjectCode(subject, "1002") ? 1 : 0;
    }

    private int defaultCashBookFlag(FinanceAccountSubject subject) {
        return isEnabled(subject.getBcash()) || defaultCashSubjectFlag(subject) == 1 || defaultBankSubjectFlag(subject) == 1 ? 1 : 0;
    }

    private int defaultBankBookFlag(FinanceAccountSubject subject) {
        return isEnabled(subject.getBbank()) || defaultBankSubjectFlag(subject) == 1 ? 1 : 0;
    }

    private boolean startsWithSubjectCode(FinanceAccountSubject subject, String prefix) {
        return trimToNull(subject.getSubjectCode()) != null && subject.getSubjectCode().startsWith(prefix);
    }

    private Integer defaultDefineFlag(Integer value) {
        return value == null ? 0 : normalizeFlag(value, 0);
    }

    private Integer normalizeLeafFlag(Integer value, Integer defaultValue) {
        return normalizeFlag(value, defaultValue == null ? 1 : defaultValue);
    }

    private boolean isEnabled(Integer value) {
        return Objects.equals(normalizeFlag(value, 0), 1);
    }

    private boolean isClosed(Integer value) {
        return Objects.equals(normalizeFlag(value, 0), 1);
    }

    private Integer normalizeFlag(Integer value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value != 0 && value != 1) {
            throw new IllegalArgumentException("状态类字段只能为 0 或 1");
        }
        return value;
    }

    private FinanceAccountSubjectOptionVO option(String value, String label) {
        FinanceAccountSubjectOptionVO option = new FinanceAccountSubjectOptionVO();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }

    private String defaultText(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String requireCompanyId(String companyId) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            throw new IllegalArgumentException("公司主体不能为空");
        }
        return normalized;
    }

    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
