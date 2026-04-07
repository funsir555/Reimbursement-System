package com.finex.auth.support.archiveagent;

import com.finex.auth.entity.ArchiveAgentRun;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
@RequiredArgsConstructor
public class ArchiveAgentTriggerDispatcher implements TriggerDispatcher {

    private final ArchiveAgentExecutionWorker archiveAgentExecutionWorker;
    @Qualifier("finexAsyncExecutor")
    private final Executor finexAsyncExecutor;

    @Override
    public void dispatch(ArchiveAgentRun run) {
        finexAsyncExecutor.execute(() -> archiveAgentExecutionWorker.process(run.getId()));
    }
}
