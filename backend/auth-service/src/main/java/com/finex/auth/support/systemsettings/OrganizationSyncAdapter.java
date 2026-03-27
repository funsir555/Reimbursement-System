package com.finex.auth.support.systemsettings;

import com.finex.auth.entity.SystemSyncConnector;

public interface OrganizationSyncAdapter {

    String getPlatformCode();

    ExternalSyncPayload pull(SystemSyncConnector connector);
}
