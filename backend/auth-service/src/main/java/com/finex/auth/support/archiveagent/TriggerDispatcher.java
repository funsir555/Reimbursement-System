package com.finex.auth.support.archiveagent;

import com.finex.auth.entity.ArchiveAgentRun;

public interface TriggerDispatcher {

    void dispatch(ArchiveAgentRun run);
}
