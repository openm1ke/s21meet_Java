package ru.izpz.edu.scheduler.metrics;

public enum SchedulerRunStatus {
  SUCCESS("success"),
  FAILED("failed"),
  PARTIAL("partial");

  private final String metricTag;

  SchedulerRunStatus(String metricTag) {
    this.metricTag = metricTag;
  }

  public String tag() {
    return metricTag;
  }
}
