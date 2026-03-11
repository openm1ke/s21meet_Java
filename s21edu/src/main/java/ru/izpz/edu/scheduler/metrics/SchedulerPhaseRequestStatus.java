package ru.izpz.edu.scheduler.metrics;

public enum SchedulerPhaseRequestStatus {
    SUCCESS("success"),
    FAILED("failed");

    private final String tag;

    SchedulerPhaseRequestStatus(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
