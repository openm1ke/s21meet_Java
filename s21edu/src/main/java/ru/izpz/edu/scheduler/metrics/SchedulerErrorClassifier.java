package ru.izpz.edu.scheduler.metrics;

import java.util.Locale;
import org.springframework.stereotype.Component;
import ru.izpz.edu.exception.PlatformClientException;

@Component
public class SchedulerErrorClassifier {

  public SchedulerErrorReason classify(Throwable error) {
    if (error == null) {
      return SchedulerErrorReason.UNKNOWN;
    }
    if (error instanceof PlatformClientException) {
      return SchedulerErrorReason.API_EXCEPTION;
    }
    if (error instanceof InterruptedException) {
      return SchedulerErrorReason.INTERRUPTED;
    }

    String className = error.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    String message = error.getMessage() == null ? "" : error.getMessage().toLowerCase(Locale.ROOT);

    if (containsAny(className, message, "timeout", "timed out")) {
      return SchedulerErrorReason.TIMEOUT;
    }
    if (containsAny(className, message, "interrupted")) {
      return SchedulerErrorReason.INTERRUPTED;
    }
    if (containsAny(
        className,
        message,
        "connect",
        "connection refused",
        "unknown host",
        "network",
        "retryable")) {
      return SchedulerErrorReason.NETWORK;
    }
    if (containsAny(className, message, "rate", "throttle", "too many requests", "429")) {
      return SchedulerErrorReason.RATE_LIMIT;
    }
    return SchedulerErrorReason.EXECUTION_EXCEPTION;
  }

  private boolean containsAny(String className, String message, String... needles) {
    for (String needle : needles) {
      if (className.contains(needle) || message.contains(needle)) {
        return true;
      }
    }
    return false;
  }
}
