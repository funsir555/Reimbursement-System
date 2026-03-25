package com.finex.auth.controller;

import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;
import com.finex.auth.service.ProcessManagementService;
import com.finex.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/process-management")
@RequiredArgsConstructor
public class ProcessManagementController {

    private final ProcessManagementService processManagementService;

    @GetMapping("/overview")
    public Result<ProcessCenterOverviewVO> overview() {
        return Result.success(processManagementService.getOverview());
    }

    @GetMapping("/template-types")
    public Result<List<ProcessTemplateTypeVO>> templateTypes() {
        return Result.success(processManagementService.getTemplateTypes());
    }

    @GetMapping("/form-options")
    public Result<ProcessTemplateFormOptionsVO> formOptions(@RequestParam String templateType) {
        return Result.success(processManagementService.getFormOptions(templateType));
    }

    @PostMapping("/templates")
    public Result<ProcessTemplateSaveResultVO> createTemplate(
            @Valid @RequestBody ProcessTemplateSaveDTO dto,
            HttpServletRequest request
    ) {
        return Result.success(
                "模板保存成功",
                processManagementService.saveTemplate(dto, getCurrentUsername(request))
        );
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        if (username instanceof String value && !value.isBlank()) {
            return value;
        }
        return "流程管理员";
    }
}
