package ru.izpz.edu.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import ru.izpz.dto.ApiException;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SchedulerMetricsService {

    private final MeterRegistry meterRegistry;
    private final CampusCatalog campusCatalog;
    private final Map<String, AtomicInteger> clusterPlacesGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastPhaseDurationNanos = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastSuccessTimestampSeconds = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastEventsSavedCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastNotifyRecipientCounts = new ConcurrentHashMap<>();

    public SchedulerMetricsService(MeterRegistry meterRegistry, CampusCatalog campusCatalog) {
        this.meterRegistry = meterRegistry;
        this.campusCatalog = campusCatalog;
    }

    public Timer.Sample startPhaseTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopPhaseTimer(String schedulerName, String phase, Timer.Sample sample) {
        long nanos = sample.stop(phaseTimer(schedulerName, phase));
        getLastPhaseDurationGauge(schedulerName, phase).set(nanos);
    }

    public void recordClusterPlaces(String campusId, String clusterName, int freePlaces, int occupiedPlaces) {
        String normalizedName = (clusterName == null || clusterName.isBlank()) ? "unknown" : clusterName;
        getClusterPlacesGauge(campusId, normalizedName, "free").set(Math.max(freePlaces, 0));
        getClusterPlacesGauge(campusId, normalizedName, "occupied").set(Math.max(occupiedPlaces, 0));
    }

    public void recordPhaseIssue(String schedulerName, String phase, String issueType) {
        incrementCounter(
            "edu_scheduler_phase_issues_total",
            "scheduler", schedulerName,
            "phase", phase,
            "issue_type", issueType
        );
    }

    public void recordPhaseRequest(String schedulerName, String phase, String status) {
        incrementCounter(
            "edu_scheduler_phase_requests_total",
            "scheduler", schedulerName,
            "phase", phase,
            "status", status
        );
    }

    public void recordRunStatus(String schedulerName, String status) {
        incrementCounter(
            "edu_scheduler_run_total",
            "scheduler", schedulerName,
            "status", status
        );
    }

    public void recordExternalApiSuccess(String schedulerName, String client, String operation) {
        recordExternalApiCall(schedulerName, client, operation, "success", "none");
    }

    public void recordExternalApiError(String schedulerName, String client, String operation, Throwable error) {
        recordExternalApiCall(schedulerName, client, operation, "error", classifyExternalApiError(error));
    }

    public void recordLastSuccess(String schedulerName) {
        getLastSuccessGauge(schedulerName).set(Instant.now().getEpochSecond());
    }

    public void recordEventsSaved(String schedulerName, long created, long updated) {
        if (created > 0) {
            meterRegistry.counter(
                "edu_events_saved_total",
                "scheduler", schedulerName,
                "result", "created"
            ).increment(created);
        }
        if (updated > 0) {
            meterRegistry.counter(
                "edu_events_saved_total",
                "scheduler", schedulerName,
                "result", "updated"
            ).increment(updated);
        }
        getLastEventsSavedGauge(schedulerName, "created").set(Math.max(created, 0));
        getLastEventsSavedGauge(schedulerName, "updated").set(Math.max(updated, 0));
    }

    public void recordNotifyRecipients(String schedulerName, long uniqueUsers, long deliveries) {
        if (uniqueUsers > 0) {
            meterRegistry.counter(
                "edu_notify_recipients_total",
                "scheduler", schedulerName,
                "recipient_type", "unique_users"
            ).increment(uniqueUsers);
        }
        if (deliveries > 0) {
            meterRegistry.counter(
                "edu_notify_recipients_total",
                "scheduler", schedulerName,
                "recipient_type", "deliveries"
            ).increment(deliveries);
        }
        getLastNotifyRecipientsGauge(schedulerName, "unique_users").set(Math.max(uniqueUsers, 0));
        getLastNotifyRecipientsGauge(schedulerName, "deliveries").set(Math.max(deliveries, 0));
    }

    private Timer phaseTimer(String schedulerName, String phase) {
        return Timer.builder("edu_scheduler_phase_duration_seconds")
            .description("Scheduler phase duration")
            .tags("scheduler", schedulerName, "phase", phase)
            .register(meterRegistry);
    }

    private void incrementCounter(String name, String... tags) {
        meterRegistry.counter(name, tags).increment();
    }

    private void recordExternalApiCall(
        String schedulerName,
        String client,
        String operation,
        String outcome,
        String reason
    ) {
        incrementCounter(
            "edu_external_api_calls_total",
            "scheduler", schedulerName,
            "client", client,
            "operation", operation,
            "outcome", outcome,
            "reason", reason
        );
    }

    private String classifyExternalApiError(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        if (error instanceof ApiException) {
            return "api_exception";
        }
        String className = error.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (className.contains("timeout")) {
            return "timeout";
        }
        if (className.contains("connect")) {
            return "network";
        }
        if (className.contains("rate") || className.contains("throttle")) {
            return "rate_limit";
        }
        return "unknown";
    }

    private AtomicInteger getClusterPlacesGauge(String campusId, String clusterName, String placeType) {
        String key = campusId + "|" + clusterName + "|" + placeType;
        return clusterPlacesGauges.computeIfAbsent(key, ignored -> {
            AtomicInteger holder = new AtomicInteger(0);
            io.micrometer.core.instrument.Gauge.builder("edu_cluster_places_current", holder, AtomicInteger::get)
                .description("Current free/occupied places per cluster")
                .tags(
                    "campus_id", campusId,
                    "campus_name", campusCatalog.campusName(campusId),
                    "cluster_name", clusterName,
                    "place_type", placeType
                )
                .register(meterRegistry);
            return holder;
        });
    }

    private AtomicLong getLastPhaseDurationGauge(String schedulerName, String phase) {
        String key = schedulerName + "|" + phase;
        return lastPhaseDurationNanos.computeIfAbsent(key, ignored -> {
            AtomicLong holder = new AtomicLong(0L);
            io.micrometer.core.instrument.Gauge.builder(
                    "edu_scheduler_phase_last_duration_seconds",
                    holder,
                    value -> value.get() / 1_000_000_000d
                )
                .description("Last scheduler phase duration in seconds")
                .tags("scheduler", schedulerName, "phase", phase)
                .register(meterRegistry);
            return holder;
        });
    }

    private AtomicLong getLastSuccessGauge(String schedulerName) {
        return lastSuccessTimestampSeconds.computeIfAbsent(schedulerName, ignored -> {
            AtomicLong holder = new AtomicLong(0L);
            io.micrometer.core.instrument.Gauge.builder(
                    "edu_scheduler_last_success_timestamp_seconds",
                    holder,
                    AtomicLong::get
                )
                .description("Unix timestamp of last successful scheduler run")
                .tags("scheduler", schedulerName)
                .register(meterRegistry);
            return holder;
        });
    }

    private AtomicLong getLastEventsSavedGauge(String schedulerName, String result) {
        String key = schedulerName + "|" + result;
        return lastEventsSavedCounts.computeIfAbsent(key, ignored -> {
            AtomicLong holder = new AtomicLong(0L);
            io.micrometer.core.instrument.Gauge.builder(
                    "edu_events_saved_last",
                    holder,
                    AtomicLong::get
                )
                .description("Events created/updated during the last scheduler run")
                .tags("scheduler", schedulerName, "result", result)
                .register(meterRegistry);
            return holder;
        });
    }

    private AtomicLong getLastNotifyRecipientsGauge(String schedulerName, String recipientType) {
        String key = schedulerName + "|" + recipientType;
        return lastNotifyRecipientCounts.computeIfAbsent(key, ignored -> {
            AtomicLong holder = new AtomicLong(0L);
            io.micrometer.core.instrument.Gauge.builder(
                    "edu_notify_recipients_last",
                    holder,
                    AtomicLong::get
                )
                .description("Unique users and deliveries for the last notify scheduler run")
                .tags("scheduler", schedulerName, "recipient_type", recipientType)
                .register(meterRegistry);
            return holder;
        });
    }
}
