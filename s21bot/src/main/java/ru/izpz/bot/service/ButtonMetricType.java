package ru.izpz.bot.service;

public enum ButtonMetricType {
  KEYBOARD("keyboard"),
  LAST_COMMAND("last_command"),
  INLINE("inline");

  private final String metricTagValue;

  ButtonMetricType(String metricTagValue) {
    this.metricTagValue = metricTagValue;
  }

  public String tagValue() {
    return metricTagValue;
  }
}
