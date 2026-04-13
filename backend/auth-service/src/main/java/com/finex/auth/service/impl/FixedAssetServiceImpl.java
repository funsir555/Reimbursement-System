package com.finex.auth.service.impl;

import com.finex.auth.dto.FixedAssetCardSaveDTO;
import com.finex.auth.dto.FixedAssetCardVO;
import com.finex.auth.dto.FixedAssetCategorySaveDTO;
import com.finex.auth.dto.FixedAssetCategoryVO;
import com.finex.auth.dto.FixedAssetChangeBillSaveDTO;
import com.finex.auth.dto.FixedAssetChangeBillVO;
import com.finex.auth.dto.FixedAssetDeprPreviewDTO;
import com.finex.auth.dto.FixedAssetDeprRunVO;
import com.finex.auth.dto.FixedAssetDisposalBillSaveDTO;
import com.finex.auth.dto.FixedAssetDisposalBillVO;
import com.finex.auth.dto.FixedAssetMetaVO;
import com.finex.auth.dto.FixedAssetOpeningImportDTO;
import com.finex.auth.dto.FixedAssetOpeningImportResultVO;
import com.finex.auth.dto.FixedAssetPeriodCloseDTO;
import com.finex.auth.dto.FixedAssetPeriodStatusVO;
import com.finex.auth.dto.FixedAssetTemplateVO;
import com.finex.auth.dto.FixedAssetVoucherLinkVO;
import com.finex.auth.mapper.FaAssetAccountPolicyMapper;
import com.finex.auth.mapper.FaAssetCardMapper;
import com.finex.auth.mapper.FaAssetCategoryMapper;
import com.finex.auth.mapper.FaAssetChangeBillMapper;
import com.finex.auth.mapper.FaAssetChangeLineMapper;
import com.finex.auth.mapper.FaAssetDeprLineMapper;
import com.finex.auth.mapper.FaAssetDeprRunMapper;
import com.finex.auth.mapper.FaAssetDisposalBillMapper;
import com.finex.auth.mapper.FaAssetDisposalLineMapper;
import com.finex.auth.mapper.FaAssetOpeningImportLineMapper;
import com.finex.auth.mapper.FaAssetOpeningImportMapper;
import com.finex.auth.mapper.FaAssetPeriodCloseMapper;
import com.finex.auth.mapper.FaAssetVoucherLinkMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FixedAssetService;
import com.finex.auth.service.impl.fixedasset.FixedAssetCardOpeningSupport;
import com.finex.auth.service.impl.fixedasset.FixedAssetChangeDisposalSupport;
import com.finex.auth.service.impl.fixedasset.FixedAssetDepreciationPeriodSupport;
import com.finex.auth.service.impl.fixedasset.FixedAssetMetaCategorySupport;
import com.finex.auth.service.impl.fixedasset.FixedAssetVoucherQuerySupport;
import com.finex.auth.service.impl.fixedasset.SharedFixedAssetSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FixedAssetServiceImpl implements FixedAssetService {

    private final FixedAssetMetaCategorySupport fixedAssetMetaCategorySupport;
    private final FixedAssetCardOpeningSupport fixedAssetCardOpeningSupport;
    private final FixedAssetChangeDisposalSupport fixedAssetChangeDisposalSupport;
    private final FixedAssetDepreciationPeriodSupport fixedAssetDepreciationPeriodSupport;
    private final FixedAssetVoucherQuerySupport fixedAssetVoucherQuerySupport;

    @Autowired
    public FixedAssetServiceImpl(
            FaAssetCategoryMapper faAssetCategoryMapper,
            FaAssetAccountPolicyMapper faAssetAccountPolicyMapper,
            FaAssetCardMapper faAssetCardMapper,
            FaAssetChangeBillMapper faAssetChangeBillMapper,
            FaAssetChangeLineMapper faAssetChangeLineMapper,
            FaAssetDeprRunMapper faAssetDeprRunMapper,
            FaAssetDeprLineMapper faAssetDeprLineMapper,
            FaAssetDisposalBillMapper faAssetDisposalBillMapper,
            FaAssetDisposalLineMapper faAssetDisposalLineMapper,
            FaAssetOpeningImportMapper faAssetOpeningImportMapper,
            FaAssetOpeningImportLineMapper faAssetOpeningImportLineMapper,
            FaAssetVoucherLinkMapper faAssetVoucherLinkMapper,
            FaAssetPeriodCloseMapper faAssetPeriodCloseMapper,
            GlAccvouchMapper glAccvouchMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper
    ) {
        SharedFixedAssetSupport support = new SharedFixedAssetSupport(
                faAssetCategoryMapper,
                faAssetAccountPolicyMapper,
                faAssetCardMapper,
                faAssetChangeBillMapper,
                faAssetChangeLineMapper,
                faAssetDeprRunMapper,
                faAssetDeprLineMapper,
                faAssetDisposalBillMapper,
                faAssetDisposalLineMapper,
                faAssetOpeningImportMapper,
                faAssetOpeningImportLineMapper,
                faAssetVoucherLinkMapper,
                faAssetPeriodCloseMapper,
                glAccvouchMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper
        );
        this.fixedAssetMetaCategorySupport = new FixedAssetMetaCategorySupport(support);
        this.fixedAssetCardOpeningSupport = new FixedAssetCardOpeningSupport(support);
        this.fixedAssetChangeDisposalSupport = new FixedAssetChangeDisposalSupport(support);
        this.fixedAssetDepreciationPeriodSupport = new FixedAssetDepreciationPeriodSupport(support);
        this.fixedAssetVoucherQuerySupport = new FixedAssetVoucherQuerySupport(support);
    }

    FixedAssetServiceImpl(
            FixedAssetMetaCategorySupport fixedAssetMetaCategorySupport,
            FixedAssetCardOpeningSupport fixedAssetCardOpeningSupport,
            FixedAssetChangeDisposalSupport fixedAssetChangeDisposalSupport,
            FixedAssetDepreciationPeriodSupport fixedAssetDepreciationPeriodSupport,
            FixedAssetVoucherQuerySupport fixedAssetVoucherQuerySupport
    ) {
        this.fixedAssetMetaCategorySupport = fixedAssetMetaCategorySupport;
        this.fixedAssetCardOpeningSupport = fixedAssetCardOpeningSupport;
        this.fixedAssetChangeDisposalSupport = fixedAssetChangeDisposalSupport;
        this.fixedAssetDepreciationPeriodSupport = fixedAssetDepreciationPeriodSupport;
        this.fixedAssetVoucherQuerySupport = fixedAssetVoucherQuerySupport;
    }

    @Override
    public FixedAssetMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetMetaCategorySupport.getMeta(currentUserId, currentUsername, companyId, fiscalYear, fiscalPeriod);
    }

    @Override
    public List<FixedAssetCategoryVO> listCategories(String companyId) {
        return fixedAssetMetaCategorySupport.listCategories(companyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetCategoryVO createCategory(FixedAssetCategorySaveDTO dto, String operatorName) {
        return fixedAssetMetaCategorySupport.createCategory(dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetCategoryVO updateCategory(Long id, FixedAssetCategorySaveDTO dto, String operatorName) {
        return fixedAssetMetaCategorySupport.updateCategory(id, dto, operatorName);
    }

    @Override
    public List<FixedAssetCardVO> listCards(String companyId, String bookCode, String keyword, Long categoryId, String status) {
        return fixedAssetCardOpeningSupport.listCards(companyId, bookCode, keyword, categoryId, status);
    }

    @Override
    public FixedAssetCardVO getCard(Long id) {
        return fixedAssetCardOpeningSupport.getCard(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetCardVO createCard(FixedAssetCardSaveDTO dto, String operatorName) {
        return fixedAssetCardOpeningSupport.createCard(dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetCardVO updateCard(Long id, FixedAssetCardSaveDTO dto, String operatorName) {
        return fixedAssetCardOpeningSupport.updateCard(id, dto, operatorName);
    }

    @Override
    public FixedAssetTemplateVO getOpeningTemplate(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetCardOpeningSupport.getOpeningTemplate(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetOpeningImportResultVO importOpening(FixedAssetOpeningImportDTO dto, String operatorName) {
        return fixedAssetCardOpeningSupport.importOpening(dto, operatorName);
    }

    @Override
    public FixedAssetOpeningImportResultVO getOpeningImportResult(Long batchId) {
        return fixedAssetCardOpeningSupport.getOpeningImportResult(batchId);
    }

    @Override
    public List<FixedAssetChangeBillVO> listChangeBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetChangeDisposalSupport.listChangeBills(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetChangeBillVO createChangeBill(FixedAssetChangeBillSaveDTO dto, String operatorName) {
        return fixedAssetChangeDisposalSupport.createChangeBill(dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetChangeBillVO postChangeBill(Long id, String operatorName) {
        return fixedAssetChangeDisposalSupport.postChangeBill(id, operatorName);
    }

    @Override
    public FixedAssetDeprRunVO previewDepreciation(FixedAssetDeprPreviewDTO dto) {
        return fixedAssetDepreciationPeriodSupport.previewDepreciation(dto);
    }

    @Override
    public List<FixedAssetDeprRunVO> listDepreciationRuns(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetDepreciationPeriodSupport.listDepreciationRuns(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetDeprRunVO createDepreciationRun(FixedAssetDeprPreviewDTO dto, String operatorName) {
        return fixedAssetDepreciationPeriodSupport.createDepreciationRun(dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetDeprRunVO postDepreciationRun(Long id, String operatorName) {
        return fixedAssetDepreciationPeriodSupport.postDepreciationRun(id, operatorName);
    }

    @Override
    public List<FixedAssetDisposalBillVO> listDisposalBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetChangeDisposalSupport.listDisposalBills(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetDisposalBillVO createDisposalBill(FixedAssetDisposalBillSaveDTO dto, String operatorName) {
        return fixedAssetChangeDisposalSupport.createDisposalBill(dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetDisposalBillVO postDisposalBill(Long id, String operatorName) {
        return fixedAssetChangeDisposalSupport.postDisposalBill(id, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetPeriodStatusVO closePeriod(FixedAssetPeriodCloseDTO dto, String operatorName) {
        return fixedAssetDepreciationPeriodSupport.closePeriod(dto, operatorName);
    }

    @Override
    public FixedAssetPeriodStatusVO getPeriodStatus(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetDepreciationPeriodSupport.getPeriodStatus(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    @Override
    public FixedAssetVoucherLinkVO getVoucherLink(String companyId, String businessType, Long businessId) {
        return fixedAssetVoucherQuerySupport.getVoucherLink(companyId, businessType, businessId);
    }
}
