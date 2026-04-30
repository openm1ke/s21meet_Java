package ru.izpz.edu.scheduler.metrics;

public enum SchedulerPhaseRequestStatus {
  SUCCESS("success"),
  FAILED("failed");

  private final String metricTag;

  SchedulerPhaseRequestStatus(String metricTag) {
    this.metricTag = metricTag;
  }

  public String tag() {
    return metricTag;
  }
}
