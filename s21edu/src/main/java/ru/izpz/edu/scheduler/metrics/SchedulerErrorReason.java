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

  private final String metricTag;

  SchedulerErrorReason(String metricTag) {
    this.metricTag = metricTag;
  }

  public String tag() {
    return metricTag;
  }
}
