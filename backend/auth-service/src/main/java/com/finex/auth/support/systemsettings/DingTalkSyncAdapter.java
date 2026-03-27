package com.finex.auth.support.systemsettings;

import com.finex.auth.entity.SystemSyncConnector;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DingTalkSyncAdapter implements OrganizationSyncAdapter {

    @Override
    public String getPlatformCode() {
        return "DINGTALK";
    }

    @Override
    public ExternalSyncPayload pull(SystemSyncConnector connector) {
        return new ExternalSyncPayload(
                List.of(
                        new ExternalDepartmentData("DD_ROOT", "钉钉总部", null, "ding-root", 1),
                        new ExternalDepartmentData("DD_FINANCE", "钉钉财务中心", "DD_ROOT", "ding-finance", 1),
                        new ExternalDepartmentData("DD_OPERATE", "钉钉运营中心", "DD_ROOT", "ding-operate", 1)
                ),
                List.of(
                        new ExternalEmployeeData("ding.finance", "钉钉财务专员", "13900001111", "ding.finance@finex.com", "DD_FINANCE", "财务专员", "集团总部", "ding-user-01", 1),
                        new ExternalEmployeeData("ding.ops", "钉钉运营经理", "13900001112", "ding.ops@finex.com", "DD_OPERATE", "运营经理", "集团总部", "ding-user-02", 1)
                )
        );
    }
}
