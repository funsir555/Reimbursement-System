package com.finex.auth.dto;

import java.util.List;
import lombok.Data;

@Data
public class FinanceModuleEnableSummaryVO {

    private String companyId;

    private String moduleCode;

    private String moduleName;

    private boolean enabled;

    private boolean implemented;

    private boolean toggleAllowed;

    private boolean disableAllowed;

    private boolean backupAllowed;

    private boolean clearAllowed;

    private boolean backupRecordAllowed;

    private String blockedMessage;

    private String clearBlockedMessage;

    private List<String> placeholderActions;
}
