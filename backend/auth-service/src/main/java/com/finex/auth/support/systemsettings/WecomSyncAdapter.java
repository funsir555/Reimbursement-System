package com.finex.auth.support.systemsettings;

import com.finex.auth.entity.SystemSyncConnector;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WecomSyncAdapter implements OrganizationSyncAdapter {

    @Override
    public String getPlatformCode() {
        return "WECOM";
    }

    @Override
    public ExternalSyncPayload pull(SystemSyncConnector connector) {
        return new ExternalSyncPayload(
                List.of(
                        new ExternalDepartmentData("WC_ROOT", "企微集团", null, "wecom-root", 1),
                        new ExternalDepartmentData("WC_HR", "企微人力中心", "WC_ROOT", "wecom-hr", 1),
                        new ExternalDepartmentData("WC_IT", "企微信息中心", "WC_ROOT", "wecom-it", 1)
                ),
                List.of(
                        new ExternalEmployeeData("wecom.hr", "企微HRBP", "13900002221", "wecom.hr@finex.com", "WC_HR", "HRBP", "集团总部", "wecom-user-01", 1),
                        new ExternalEmployeeData("wecom.it", "企微运维工程师", "13900002222", "wecom.it@finex.com", "WC_IT", "运维工程师", "集团总部", "wecom-user-02", 1)
                )
        );
    }
}
