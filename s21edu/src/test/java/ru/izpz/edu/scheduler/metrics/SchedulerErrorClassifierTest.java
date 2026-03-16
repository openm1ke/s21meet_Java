package ru.izpz.edu.scheduler.metrics;

import org.junit.jupiter.api.Test;
import ru.izpz.dto.ApiException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchedulerErrorClassifierTest {

    private final SchedulerErrorClassifier classifier = new SchedulerErrorClassifier();

    @Test
    void classify_shouldReturnUnknown_whenErrorIsNull() {
        assertEquals(SchedulerErrorReason.UNKNOWN, classifier.classify(null));
    }

    @Test
    void classify_shouldReturnApiException_whenApiException() {
        assertEquals(SchedulerErrorReason.API_EXCEPTION, classifier.classify(new ApiException("api")));
    }

    @Test
    void classify_shouldReturnInterrupted_whenInterruptedException() {
        assertEquals(SchedulerErrorReason.INTERRUPTED, classifier.classify(new InterruptedException("stop")));
    }

    @Test
    void classify_shouldReturnTimeout_whenContainsTimeoutKeywords() {
        assertEquals(SchedulerErrorReason.TIMEOUT, classifier.classify(new RuntimeException("operation timed out")));
    }

    @Test
    void classify_shouldReturnInterrupted_whenContainsInterruptedKeyword() {
        assertEquals(SchedulerErrorReason.INTERRUPTED, classifier.classify(new RuntimeException("thread interrupted")));
    }

    @Test
    void classify_shouldReturnNetwork_whenContainsNetworkKeywords() {
        assertEquals(SchedulerErrorReason.NETWORK, classifier.classify(new RuntimeException("connection refused")));
    }

    @Test
    void classify_shouldReturnRateLimit_whenContainsRateKeywords() {
        assertEquals(SchedulerErrorReason.RATE_LIMIT, classifier.classify(new RuntimeException("too many requests 429")));
    }

    @Test
    void classify_shouldReturnExecutionException_whenNoKnownKeywords() {
        assertEquals(SchedulerErrorReason.EXECUTION_EXCEPTION, classifier.classify(new RuntimeException("boom")));
    }
}
