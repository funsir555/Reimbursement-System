package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceCashFlowItemSaveDTO;
import com.finex.auth.dto.FinanceCashFlowItemStatusDTO;
import com.finex.auth.dto.FinanceCashFlowItemSummaryVO;
import com.finex.auth.entity.FinanceCashFlowItem;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FinanceCashFlowItemMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.service.FinanceCashFlowArchiveService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class FinanceCashFlowArchiveServiceImpl implements FinanceCashFlowArchiveService {

    private static final String DIRECTION_INFLOW = "INFLOW";
    private static final String DIRECTION_OUTFLOW = "OUTFLOW";

    private final FinanceCashFlowItemMapper financeCashFlowItemMapper;
    private final SystemCompanyMapper systemCompanyMapper;

    public FinanceCashFlowArchiveServiceImpl(
            FinanceCashFlowItemMapper financeCashFlowItemMapper,
            SystemCompanyMapper systemCompanyMapper
    ) {
        this.financeCashFlowItemMapper = financeCashFlowItemMapper;
        this.systemCompanyMapper = systemCompanyMapper;
    }

    @Override
    public List<FinanceCashFlowItemSummaryVO> listCashFlows(String companyId, String keyword, String direction, Integer status) {
        String normalizedCompanyId = requireCompanyId(companyId);
        requireEnabledCompany(normalizedCompanyId);
        String normalizedKeyword = trimToNull(keyword);
        String normalizedDirection = normalizeDirection(direction, false);
        return financeCashFlowItemMapper.selectList(
                        Wrappers.<FinanceCashFlowItem>lambdaQuery()
                                .eq(FinanceCashFlowItem::getCompanyId, normalizedCompanyId)
                                .and(normalizedKeyword != null, wrapper -> wrapper
                                        .like(FinanceCashFlowItem::getCashFlowCode, normalizedKeyword)
                                        .or()
                                        .like(FinanceCashFlowItem::getCashFlowName, normalizedKeyword)
                                )
                                .eq(normalizedDirection != null, FinanceCashFlowItem::getDirection, normalizedDirection)
                                .eq(status != null, FinanceCashFlowItem::getStatus, normalizeFlag(status))
                                .orderByAsc(FinanceCashFlowItem::getSortOrder, FinanceCashFlowItem::getCashFlowCode, FinanceCashFlowItem::getId)
                ).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceCashFlowItemSummaryVO createCashFlow(String companyId, FinanceCashFlowItemSaveDTO dto) {
        String normalizedCompanyId = requireCompanyId(companyId);
        requireEnabledCompany(normalizedCompanyId);
        validateSavePayload(dto);
        String cashFlowCode = trimToNull(dto.getCashFlowCode());
        if (findByCompanyAndCode(normalizedCompanyId, cashFlowCode) != null) {
            throw new IllegalStateException("当前公司已存在相同编码的现金流量");
        }
        FinanceCashFlowItem entity = new FinanceCashFlowItem();
        entity.setCompanyId(normalizedCompanyId);
        entity.setCashFlowCode(cashFlowCode);
        entity.setCashFlowName(trimToNull(dto.getCashFlowName()));
        entity.setDirection(normalizeDirection(dto.getDirection(), true));
        entity.setStatus(normalizeFlag(dto.getStatus() == null ? 1 : dto.getStatus()));
        entity.setSortOrder(resolveSortOrder(normalizedCompanyId, dto.getSortOrder()));
        financeCashFlowItemMapper.insert(entity);
        return toSummary(requireCashFlow(normalizedCompanyId, entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceCashFlowItemSummaryVO updateCashFlow(String companyId, Long id, FinanceCashFlowItemSaveDTO dto) {
        String normalizedCompanyId = requireCompanyId(companyId);
        requireEnabledCompany(normalizedCompanyId);
        validateSavePayload(dto);
        FinanceCashFlowItem entity = requireCashFlow(normalizedCompanyId, id);
        String cashFlowCode = trimToNull(dto.getCashFlowCode());
        FinanceCashFlowItem duplicate = findByCompanyAndCode(normalizedCompanyId, cashFlowCode);
        if (duplicate != null && !Objects.equals(duplicate.getId(), entity.getId())) {
            throw new IllegalStateException("当前公司已存在相同编码的现金流量");
        }
        entity.setCashFlowCode(cashFlowCode);
        entity.setCashFlowName(trimToNull(dto.getCashFlowName()));
        entity.setDirection(normalizeDirection(dto.getDirection(), true));
        entity.setStatus(normalizeFlag(dto.getStatus() == null ? entity.getStatus() : dto.getStatus()));
        entity.setSortOrder(resolveSortOrder(normalizedCompanyId, dto.getSortOrder()));
        financeCashFlowItemMapper.updateById(entity);
        return toSummary(requireCashFlow(normalizedCompanyId, entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCashFlowStatus(String companyId, Long id, FinanceCashFlowItemStatusDTO dto) {
        String normalizedCompanyId = requireCompanyId(companyId);
        requireEnabledCompany(normalizedCompanyId);
        FinanceCashFlowItem entity = requireCashFlow(normalizedCompanyId, id);
        entity.setStatus(normalizeFlag(dto == null ? null : dto.getStatus()));
        financeCashFlowItemMapper.updateById(entity);
        return Boolean.TRUE;
    }

    private FinanceCashFlowItemSummaryVO toSummary(FinanceCashFlowItem entity) {
        FinanceCashFlowItemSummaryVO summary = new FinanceCashFlowItemSummaryVO();
        summary.setId(entity.getId());
        summary.setCompanyId(entity.getCompanyId());
        summary.setCashFlowCode(entity.getCashFlowCode());
        summary.setCashFlowName(entity.getCashFlowName());
        summary.setDirection(entity.getDirection());
        summary.setStatus(entity.getStatus());
        summary.setSortOrder(entity.getSortOrder());
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        return summary;
    }

    private FinanceCashFlowItem requireCashFlow(String companyId, Long id) {
        if (id == null) {
            throw new IllegalArgumentException("现金流量标识不能为空");
        }
        FinanceCashFlowItem entity = financeCashFlowItemMapper.selectOne(
                Wrappers.<FinanceCashFlowItem>lambdaQuery()
                        .eq(FinanceCashFlowItem::getCompanyId, companyId)
                        .eq(FinanceCashFlowItem::getId, id)
                        .last("limit 1")
        );
        if (entity == null) {
            throw new IllegalStateException("现金流量不存在");
        }
        return entity;
    }

    private FinanceCashFlowItem findByCompanyAndCode(String companyId, String cashFlowCode) {
        return financeCashFlowItemMapper.selectOne(
                Wrappers.<FinanceCashFlowItem>lambdaQuery()
                        .eq(FinanceCashFlowItem::getCompanyId, companyId)
                        .eq(FinanceCashFlowItem::getCashFlowCode, cashFlowCode)
                        .last("limit 1")
        );
    }

    private void validateSavePayload(FinanceCashFlowItemSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("现金流量数据不能为空");
        }
        if (trimToNull(dto.getCashFlowCode()) == null) {
            throw new IllegalArgumentException("现金流量编码不能为空");
        }
        if (trimToNull(dto.getCashFlowName()) == null) {
            throw new IllegalArgumentException("现金流量名称不能为空");
        }
        normalizeDirection(dto.getDirection(), true);
    }

    private String normalizeDirection(String direction, boolean required) {
        String normalized = trimToNull(direction);
        if (normalized == null) {
            if (required) {
                throw new IllegalArgumentException("现金流量方向不能为空");
            }
            return null;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!Objects.equals(upper, DIRECTION_INFLOW) && !Objects.equals(upper, DIRECTION_OUTFLOW)) {
            throw new IllegalArgumentException("现金流量方向不合法");
        }
        return upper;
    }

    private int resolveSortOrder(String companyId, Integer sortOrder) {
        if (sortOrder != null && sortOrder >= 0) {
            return sortOrder;
        }
        Long count = financeCashFlowItemMapper.selectCount(
                Wrappers.<FinanceCashFlowItem>lambdaQuery()
                        .eq(FinanceCashFlowItem::getCompanyId, companyId)
        );
        return (count == null ? 0 : count.intValue()) + 1;
    }

    private String requireCompanyId(String companyId) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            throw new IllegalArgumentException("公司主体不能为空");
        }
        return normalized;
    }

    private void requireEnabledCompany(String companyId) {
        SystemCompany company = systemCompanyMapper.selectById(companyId);
        if (company == null || !Objects.equals(company.getStatus(), 1)) {
            throw new IllegalStateException("当前公司不存在或已停用");
        }
    }

    private Integer normalizeFlag(Integer status) {
        if (status == null) {
            return 0;
        }
        return status == 0 ? 0 : 1;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
