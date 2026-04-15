// 业务域：固定资产
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 固定资产卡片、变更处置、折旧期间等接口，下游会继续协调 资产卡片、折旧期间、凭证查询和分类元数据。
// 风险提醒：改坏后最容易影响 资产台账、折旧计提和资产凭证对应。

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

/**
 * FixedAssetServiceImpl：service 入口实现。
 * 接住上层请求，并把 固定资产相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 资产台账、折旧计提和资产凭证对应是否会被一起带坏。
 */
@Service
public class FixedAssetServiceImpl implements FixedAssetService {

    private final FixedAssetMetaCategorySupport fixedAssetMetaCategorySupport;
    private final FixedAssetCardOpeningSupport fixedAssetCardOpeningSupport;
    private final FixedAssetChangeDisposalSupport fixedAssetChangeDisposalSupport;
    private final FixedAssetDepreciationPeriodSupport fixedAssetDepreciationPeriodSupport;
    private final FixedAssetVoucherQuerySupport fixedAssetVoucherQuerySupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 获取元数据。
     */
    @Override
    public FixedAssetMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetMetaCategorySupport.getMeta(currentUserId, currentUsername, companyId, fiscalYear, fiscalPeriod);
    }

    /**
     * 查询Categories列表。
     */
    @Override
    public List<FixedAssetCategoryVO> listCategories(String companyId) {
        return fixedAssetMetaCategorySupport.listCategories(companyId);
    }

    /**
     * 创建分类。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetCategoryVO createCategory(FixedAssetCategorySaveDTO dto, String operatorName) {
        return fixedAssetMetaCategorySupport.createCategory(dto, operatorName);
    }

    /**
     * 更新分类。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetCategoryVO updateCategory(Long id, FixedAssetCategorySaveDTO dto, String operatorName) {
        return fixedAssetMetaCategorySupport.updateCategory(id, dto, operatorName);
    }

    /**
     * 查询卡片列表。
     */
    @Override
    public List<FixedAssetCardVO> listCards(String companyId, String bookCode, String keyword, Long categoryId, String status) {
        return fixedAssetCardOpeningSupport.listCards(companyId, bookCode, keyword, categoryId, status);
    }

    /**
     * 获取卡片。
     */
    @Override
    public FixedAssetCardVO getCard(Long id) {
        return fixedAssetCardOpeningSupport.getCard(id);
    }

    /**
     * 创建卡片。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetCardVO createCard(FixedAssetCardSaveDTO dto, String operatorName) {
        return fixedAssetCardOpeningSupport.createCard(dto, operatorName);
    }

    /**
     * 更新卡片。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetCardVO updateCard(Long id, FixedAssetCardSaveDTO dto, String operatorName) {
        return fixedAssetCardOpeningSupport.updateCard(id, dto, operatorName);
    }

    /**
     * 获取启用模板。
     */
    @Override
    public FixedAssetTemplateVO getOpeningTemplate(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetCardOpeningSupport.getOpeningTemplate(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    /**
     * 处理固定资产中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetOpeningImportResultVO importOpening(FixedAssetOpeningImportDTO dto, String operatorName) {
        return fixedAssetCardOpeningSupport.importOpening(dto, operatorName);
    }

    /**
     * 获取启用ImportResult。
     */
    @Override
    public FixedAssetOpeningImportResultVO getOpeningImportResult(Long batchId) {
        return fixedAssetCardOpeningSupport.getOpeningImportResult(batchId);
    }

    /**
     * 查询变更Bills列表。
     */
    @Override
    public List<FixedAssetChangeBillVO> listChangeBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetChangeDisposalSupport.listChangeBills(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    /**
     * 创建变更Bill。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetChangeBillVO createChangeBill(FixedAssetChangeBillSaveDTO dto, String operatorName) {
        return fixedAssetChangeDisposalSupport.createChangeBill(dto, operatorName);
    }

    /**
     * 处理固定资产中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetChangeBillVO postChangeBill(Long id, String operatorName) {
        return fixedAssetChangeDisposalSupport.postChangeBill(id, operatorName);
    }

    /**
     * 处理固定资产中的这一步。
     */
    @Override
    public FixedAssetDeprRunVO previewDepreciation(FixedAssetDeprPreviewDTO dto) {
        return fixedAssetDepreciationPeriodSupport.previewDepreciation(dto);
    }

    /**
     * 查询折旧执行列表。
     */
    @Override
    public List<FixedAssetDeprRunVO> listDepreciationRuns(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetDepreciationPeriodSupport.listDepreciationRuns(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    /**
     * 创建折旧执行。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetDeprRunVO createDepreciationRun(FixedAssetDeprPreviewDTO dto, String operatorName) {
        return fixedAssetDepreciationPeriodSupport.createDepreciationRun(dto, operatorName);
    }

    /**
     * 处理固定资产中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetDeprRunVO postDepreciationRun(Long id, String operatorName) {
        return fixedAssetDepreciationPeriodSupport.postDepreciationRun(id, operatorName);
    }

    /**
     * 查询处置Bills列表。
     */
    @Override
    public List<FixedAssetDisposalBillVO> listDisposalBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetChangeDisposalSupport.listDisposalBills(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    /**
     * 创建处置Bill。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetDisposalBillVO createDisposalBill(FixedAssetDisposalBillSaveDTO dto, String operatorName) {
        return fixedAssetChangeDisposalSupport.createDisposalBill(dto, operatorName);
    }

    /**
     * 处理固定资产中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetDisposalBillVO postDisposalBill(Long id, String operatorName) {
        return fixedAssetChangeDisposalSupport.postDisposalBill(id, operatorName);
    }

    /**
     * 关闭期间。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FixedAssetPeriodStatusVO closePeriod(FixedAssetPeriodCloseDTO dto, String operatorName) {
        return fixedAssetDepreciationPeriodSupport.closePeriod(dto, operatorName);
    }

    /**
     * 获取期间Status。
     */
    @Override
    public FixedAssetPeriodStatusVO getPeriodStatus(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return fixedAssetDepreciationPeriodSupport.getPeriodStatus(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    /**
     * 获取凭证Link。
     */
    @Override
    public FixedAssetVoucherLinkVO getVoucherLink(String companyId, String businessType, Long businessId) {
        return fixedAssetVoucherQuerySupport.getVoucherLink(companyId, businessType, businessId);
    }
}
