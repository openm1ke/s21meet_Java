package ru.izpz.edu.scheduler.metrics;

public enum SchedulerErrorReason {
    NONE("none"),
    API_EXCEPTION("api_exception"),
    TIMEOUT("timeout"),
    NETWORK("network"),
    RATE_LIMIT("rate_limit"),
    INTERRUPTED("interrupted"),
    EXECUTION_EXCEPTION("execution_exception"),
    UNKNOWN("unknown");

    private final String tag;

    SchedulerErrorReason(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
