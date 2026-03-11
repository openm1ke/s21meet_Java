package ru.izpz.edu.scheduler.metrics;

public enum SchedulerRunStatus {
    SUCCESS("success"),
    FAILED("failed"),
    PARTIAL("partial");

    private final String tag;

    SchedulerRunStatus(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
