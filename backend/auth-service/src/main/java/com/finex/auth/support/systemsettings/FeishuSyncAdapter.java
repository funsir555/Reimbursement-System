package com.finex.auth.support.systemsettings;

import com.finex.auth.entity.SystemSyncConnector;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FeishuSyncAdapter implements OrganizationSyncAdapter {

    @Override
    public String getPlatformCode() {
        return "FEISHU";
    }

    @Override
    public ExternalSyncPayload pull(SystemSyncConnector connector) {
        return new ExternalSyncPayload(
                List.of(
                        new ExternalDepartmentData("FS_ROOT", "飞书集团", null, "feishu-root", 1),
                        new ExternalDepartmentData("FS_SALES", "飞书销售中心", "FS_ROOT", "feishu-sales", 1),
                        new ExternalDepartmentData("FS_PM", "飞书产品中心", "FS_ROOT", "feishu-pm", 1)
                ),
                List.of(
                        new ExternalEmployeeData("feishu.sales", "飞书销售顾问", "13900003331", "feishu.sales@finex.com", "FS_SALES", "销售顾问", "集团总部", "feishu-user-01", 1),
                        new ExternalEmployeeData("feishu.pm", "飞书产品经理", "13900003332", "feishu.pm@finex.com", "FS_PM", "产品经理", "集团总部", "feishu-user-02", 1)
                )
        );
    }
}
