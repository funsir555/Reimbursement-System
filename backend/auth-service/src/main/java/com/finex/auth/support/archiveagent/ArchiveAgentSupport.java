package com.finex.auth.support.archiveagent;

import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class ArchiveAgentSupport {

    public static final String AGENT_STATUS_DRAFT = "DRAFT";
    public static final String AGENT_STATUS_READY = "READY";
    public static final String AGENT_STATUS_DISABLED = "DISABLED";
    public static final String AGENT_STATUS_ARCHIVED = "ARCHIVED";

    public static final String RUN_STATUS_PENDING = "PENDING";
    public static final String RUN_STATUS_RUNNING = "RUNNING";
    public static final String RUN_STATUS_SUCCESS = "SUCCESS";
    public static final String RUN_STATUS_FAILED = "FAILED";

    public static final String TRIGGER_TYPE_MANUAL = "MANUAL";
    public static final String TRIGGER_TYPE_SCHEDULE = "SCHEDULE";
    public static final String TRIGGER_TYPE_EVENT = "EVENT";

    public static final String SCHEDULE_MODE_CRON = "CRON";
    public static final String SCHEDULE_MODE_INTERVAL = "INTERVAL";

    public static final String SCHEDULE_STATUS_IDLE = "IDLE";
    public static final String SCHEDULE_STATUS_RUNNING = "RUNNING";
    public static final String SCHEDULE_STATUS_DISABLED = "DISABLED";

    public static final Set<String> NODE_TYPES = Set.of("start", "llm", "condition", "tool", "transform", "notify", "end");

    private static final DateTimeFormatter AGENT_CODE_TIME = DateTimeFormatter.ofPattern("yyMMddHHmm");
    private static final DateTimeFormatter RUN_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private ArchiveAgentSupport() {
    }

    public static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return AGENT_STATUS_DRAFT;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    public static String buildAgentCode() {
        return "AG" + LocalDateTime.now().format(AGENT_CODE_TIME) + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    public static String buildRunNo() {
        return "ARUN" + LocalDateTime.now().format(RUN_NO_TIME) + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    public static LocalDateTime computeNextFireAt(String scheduleMode, String cronExpression, Integer intervalMinutes, LocalDateTime now) {
        if (SCHEDULE_MODE_INTERVAL.equalsIgnoreCase(scheduleMode)) {
            int normalizedInterval = intervalMinutes == null || intervalMinutes <= 0 ? 60 : intervalMinutes;
            return now.plusMinutes(normalizedInterval);
        }
        if (cronExpression == null || cronExpression.isBlank()) {
            return null;
        }
        return CronExpression.parse(cronExpression).next(now);
    }
}
