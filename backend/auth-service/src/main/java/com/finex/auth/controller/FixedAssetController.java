package com.finex.auth.controller;

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
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.FixedAssetService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/finance/fixed-assets")
@RequiredArgsConstructor
public class FixedAssetController {

    private static final String VIEW = "finance:fixed_assets:view";
    private static final String CREATE = "finance:fixed_assets:create";
    private static final String EDIT = "finance:fixed_assets:edit";
    private static final String DELETE = "finance:fixed_assets:delete";
    private static final String IMPORT = "finance:fixed_assets:import";
    private static final String CHANGE = "finance:fixed_assets:change";
    private static final String DEPRECIATE = "finance:fixed_assets:depreciate";
    private static final String DISPOSE = "finance:fixed_assets:dispose";
    private static final String CLOSE_PERIOD = "finance:fixed_assets:close_period";
    private static final String VIEW_VOUCHER = "finance:fixed_assets:view_voucher_link";

    private final FixedAssetService fixedAssetService;
    private final AccessControlService accessControlService;

    @GetMapping("/meta")
    public Result<FixedAssetMetaVO> meta(@RequestParam(required = false) String companyId,
                                         @RequestParam(required = false) Integer fiscalYear,
                                         @RequestParam(required = false) Integer fiscalPeriod,
                                         HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(fixedAssetService.getMeta(getCurrentUserId(request), getCurrentUsername(request), companyId, fiscalYear, fiscalPeriod));
    }

    @GetMapping("/categories")
    public Result<List<FixedAssetCategoryVO>> listCategories(@RequestParam String companyId, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(fixedAssetService.listCategories(companyId));
    }

    @PostMapping("/categories")
    public Result<FixedAssetCategoryVO> createCategory(@Valid @RequestBody FixedAssetCategorySaveDTO dto, HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), CREATE, EDIT);
        return Result.success("鍥哄畾璧勪骇绫诲埆淇濆瓨鎴愬姛", fixedAssetService.createCategory(dto, getCurrentUsername(request)));
    }

    @PutMapping("/categories/{id}")
    public Result<FixedAssetCategoryVO> updateCategory(@PathVariable Long id, @Valid @RequestBody FixedAssetCategorySaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EDIT);
        return Result.success("鍥哄畾璧勪骇绫诲埆鏇存柊鎴愬姛", fixedAssetService.updateCategory(id, dto, getCurrentUsername(request)));
    }

    @GetMapping("/cards")
    public Result<List<FixedAssetCardVO>> listCards(@RequestParam String companyId,
                                                    @RequestParam(required = false) String bookCode,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) String status,
                                                    HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(fixedAssetService.listCards(companyId, bookCode, keyword, categoryId, status));
    }

    @GetMapping("/cards/{id}")
    public Result<FixedAssetCardVO> getCard(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(fixedAssetService.getCard(id));
    }

    @PostMapping("/cards")
    public Result<FixedAssetCardVO> createCard(@Valid @RequestBody FixedAssetCardSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), CREATE);
        return Result.success("鍥哄畾璧勪骇鍗＄墖淇濆瓨鎴愬姛", fixedAssetService.createCard(dto, getCurrentUsername(request)));
    }

    @PutMapping("/cards/{id}")
    public Result<FixedAssetCardVO> updateCard(@PathVariable Long id, @Valid @RequestBody FixedAssetCardSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), EDIT);
        return Result.success("鍥哄畾璧勪骇鍗＄墖鏇存柊鎴愬姛", fixedAssetService.updateCard(id, dto, getCurrentUsername(request)));
    }

    @PostMapping("/opening-import/template")
    public Result<FixedAssetTemplateVO> openingTemplate(@RequestParam String companyId,
                                                        @RequestParam(required = false) String bookCode,
                                                        @RequestParam(required = false) Integer fiscalYear,
                                                        @RequestParam(required = false) Integer fiscalPeriod,
                                                        HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), VIEW, IMPORT);
        return Result.success(fixedAssetService.getOpeningTemplate(companyId, bookCode, fiscalYear, fiscalPeriod));
    }

    @PostMapping("/opening-import")
    public Result<FixedAssetOpeningImportResultVO> importOpening(@Valid @RequestBody FixedAssetOpeningImportDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), IMPORT);
        return Result.success("鍥哄畾璧勪骇鏈熷垵瀵煎叆瀹屾垚", fixedAssetService.importOpening(dto, getCurrentUsername(request)));
    }

    @GetMapping("/opening-import/{batchId}")
    public Result<FixedAssetOpeningImportResultVO> openingImportDetail(@PathVariable Long batchId, HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), VIEW, IMPORT);
        return Result.success(fixedAssetService.getOpeningImportResult(batchId));
    }

    @GetMapping("/change-bills")
    public Result<List<FixedAssetChangeBillVO>> listChangeBills(@RequestParam String companyId,
                                                                @RequestParam(required = false) String bookCode,
                                                                @RequestParam(required = false) Integer fiscalYear,
                                                                @RequestParam(required = false) Integer fiscalPeriod,
                                                                HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(fixedAssetService.listChangeBills(companyId, bookCode, fiscalYear, fiscalPeriod));
    }

    @PostMapping("/change-bills")
    public Result<FixedAssetChangeBillVO> createChangeBill(@Valid @RequestBody FixedAssetChangeBillSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), CHANGE);
        return Result.success("鍥哄畾璧勪骇鍙樺姩鍗曚繚瀛樻垚鍔?", fixedAssetService.createChangeBill(dto, getCurrentUsername(request)));
    }

    @PostMapping("/change-bills/{id}/post")
    public Result<FixedAssetChangeBillVO> postChangeBill(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), CHANGE);
        return Result.success("鍥哄畾璧勪骇鍙樺姩鍗曡繃璐︽垚鍔?", fixedAssetService.postChangeBill(id, getCurrentUsername(request)));
    }

    @PostMapping("/depreciation-runs/preview")
    public Result<FixedAssetDeprRunVO> previewDepreciation(@Valid @RequestBody FixedAssetDeprPreviewDTO dto, HttpServletRequest request) {
        accessControlService.requireAnyPermission(getCurrentUserId(request), VIEW, DEPRECIATE);
        return Result.success(fixedAssetService.previewDepreciation(dto));
    }

    @GetMapping("/depreciation-runs")
    public Result<List<FixedAssetDeprRunVO>> listDepreciationRuns(@RequestParam String companyId,
                                                                  @RequestParam(required = false) String bookCode,
                                                                  @RequestParam(required = false) Integer fiscalYear,
                                                                  @RequestParam(required = false) Integer fiscalPeriod,
                                                                  HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(fixedAssetService.listDepreciationRuns(companyId, bookCode, fiscalYear, fiscalPeriod));
    }

    @PostMapping("/depreciation-runs")
    public Result<FixedAssetDeprRunVO> createDepreciationRun(@Valid @RequestBody FixedAssetDeprPreviewDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), DEPRECIATE);
        return Result.success("鍥哄畾璧勪骇鎶樻棫鎵规鐢熸垚鎴愬姛", fixedAssetService.createDepreciationRun(dto, getCurrentUsername(request)));
    }

    @PostMapping("/depreciation-runs/{id}/post")
    public Result<FixedAssetDeprRunVO> postDepreciationRun(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), DEPRECIATE);
        return Result.success("鍥哄畾璧勪骇鎶樻棫杩囪处鎴愬姛", fixedAssetService.postDepreciationRun(id, getCurrentUsername(request)));
    }

    @GetMapping("/disposal-bills")
    public Result<List<FixedAssetDisposalBillVO>> listDisposalBills(@RequestParam String companyId,
                                                                    @RequestParam(required = false) String bookCode,
                                                                    @RequestParam(required = false) Integer fiscalYear,
                                                                    @RequestParam(required = false) Integer fiscalPeriod,
                                                                    HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(fixedAssetService.listDisposalBills(companyId, bookCode, fiscalYear, fiscalPeriod));
    }

    @PostMapping("/disposal-bills")
    public Result<FixedAssetDisposalBillVO> createDisposalBill(@Valid @RequestBody FixedAssetDisposalBillSaveDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), DISPOSE);
        return Result.success("鍥哄畾璧勪骇澶勭疆鍗曚繚瀛樻垚鍔?", fixedAssetService.createDisposalBill(dto, getCurrentUsername(request)));
    }

    @PostMapping("/disposal-bills/{id}/post")
    public Result<FixedAssetDisposalBillVO> postDisposalBill(@PathVariable Long id, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), DISPOSE);
        return Result.success("鍥哄畾璧勪骇澶勭疆杩囪处鎴愬姛", fixedAssetService.postDisposalBill(id, getCurrentUsername(request)));
    }

    @PostMapping("/period-close")
    public Result<FixedAssetPeriodStatusVO> closePeriod(@Valid @RequestBody FixedAssetPeriodCloseDTO dto, HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), CLOSE_PERIOD);
        return Result.success("鍥哄畾璧勪骇鏈熼棿缁撹处鎴愬姛", fixedAssetService.closePeriod(dto, getCurrentUsername(request)));
    }

    @GetMapping("/period-close/status")
    public Result<FixedAssetPeriodStatusVO> periodStatus(@RequestParam String companyId,
                                                         @RequestParam(required = false) String bookCode,
                                                         @RequestParam(required = false) Integer fiscalYear,
                                                         @RequestParam(required = false) Integer fiscalPeriod,
                                                         HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW);
        return Result.success(fixedAssetService.getPeriodStatus(companyId, bookCode, fiscalYear, fiscalPeriod));
    }

    @GetMapping("/voucher-link")
    public Result<FixedAssetVoucherLinkVO> voucherLink(@RequestParam String companyId,
                                                       @RequestParam String businessType,
                                                       @RequestParam Long businessId,
                                                       HttpServletRequest request) {
        accessControlService.requirePermission(getCurrentUserId(request), VIEW_VOUCHER);
        return Result.success(fixedAssetService.getVoucherLink(companyId, businessType, businessId));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId instanceof Long value) {
            return value;
        }
        if (userId instanceof Integer value) {
            return value.longValue();
        }
        throw new IllegalStateException("????????ID");
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "system";
    }
}
