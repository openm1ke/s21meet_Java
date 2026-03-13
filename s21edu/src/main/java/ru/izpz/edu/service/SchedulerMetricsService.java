package ru.izpz.edu.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import ru.izpz.edu.scheduler.metrics.SchedulerErrorClassifier;
import ru.izpz.edu.scheduler.metrics.SchedulerErrorReason;
import ru.izpz.edu.scheduler.metrics.SchedulerPhaseRequestStatus;
import ru.izpz.edu.scheduler.metrics.SchedulerRunStatus;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SchedulerMetricsService {
    private static final String TAG_SCHEDULER = "scheduler";
    private static final String TAG_PHASE = "phase";
    private static final String TAG_RESULT = "result";
    private static final String TAG_RECIPIENT_TYPE = "recipient_type";
    private static final String TAG_CAMPUS_ID = "campus_id";
    private static final String TAG_CAMPUS_NAME = "campus_name";

    private final MeterRegistry meterRegistry;
    private final CampusCatalog campusCatalog;
    private final SchedulerErrorClassifier schedulerErrorClassifier;
    private final Map<String, AtomicInteger> clusterPlacesGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> participantByCampusGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> participantByCampusStageGroupGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> participantByCampusStageNameGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastPhaseDurationNanos = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastSuccessTimestampSeconds = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastEventsSavedCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastNotifyRecipientCounts = new ConcurrentHashMap<>();

    public SchedulerMetricsService(
        MeterRegistry meterRegistry,
        CampusCatalog campusCatalog,
        SchedulerErrorClassifier schedulerErrorClassifier
    ) {
        this.meterRegistry = meterRegistry;
        this.campusCatalog = campusCatalog;
        this.schedulerErrorClassifier = schedulerErrorClassifier;
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

    public void resetParticipantMetrics() {
        participantByCampusGauges.values().forEach(holder -> holder.set(0L));
        participantByCampusStageGroupGauges.values().forEach(holder -> holder.set(0L));
        participantByCampusStageNameGauges.values().forEach(holder -> holder.set(0L));
    }

    public void recordParticipantsByCampus(String campusId, long count) {
        getParticipantByCampusGauge(campusId).set(Math.max(count, 0L));
    }

    public void recordParticipantsByCampusAndStageGroup(String campusId, String stageGroupName, long count) {
        String normalizedGroup = normalizeStageLabel(stageGroupName);
        getParticipantByCampusStageGroupGauge(campusId, normalizedGroup).set(Math.max(count, 0L));
    }

    public void recordParticipantsByCampusAndStageName(String campusId, String stageName, long count) {
        String normalizedStage = normalizeStageLabel(stageName);
        getParticipantByCampusStageNameGauge(campusId, normalizedStage).set(Math.max(count, 0L));
    }

    public void recordPhaseIssue(String schedulerName, String phase, SchedulerErrorReason issueType) {
        incrementCounter(
            "edu_scheduler_phase_issues_total",
            TAG_SCHEDULER, schedulerName,
            TAG_PHASE, phase,
            "issue_type", issueType.tag()
        );
    }

    public void recordPhaseRequest(String schedulerName, String phase, SchedulerPhaseRequestStatus status) {
        incrementCounter(
            "edu_scheduler_phase_requests_total",
            TAG_SCHEDULER, schedulerName,
            TAG_PHASE, phase,
            "status", status.tag()
        );
    }

    public void recordRunStatus(String schedulerName, SchedulerRunStatus status) {
        incrementCounter(
            "edu_scheduler_run_total",
            TAG_SCHEDULER, schedulerName,
            "status", status.tag()
        );
    }

    public void recordExternalApiSuccess(String schedulerName, String client, String operation) {
        recordExternalApiCall(schedulerName, client, operation, "success", SchedulerErrorReason.NONE);
    }

    public void recordExternalApiError(String schedulerName, String client, String operation, Throwable error) {
        recordExternalApiCall(
            schedulerName,
            client,
            operation,
            "error",
            schedulerErrorClassifier.classify(error)
        );
    }

    public void recordLastSuccess(String schedulerName) {
        getLastSuccessGauge(schedulerName).set(Instant.now().getEpochSecond());
    }

    public void recordEventsSaved(String schedulerName, long created, long updated) {
        if (created > 0) {
            meterRegistry.counter(
                "edu_events_saved_total",
                TAG_SCHEDULER, schedulerName,
                TAG_RESULT, "created"
            ).increment(created);
        }
        if (updated > 0) {
            meterRegistry.counter(
                "edu_events_saved_total",
                TAG_SCHEDULER, schedulerName,
                TAG_RESULT, "updated"
            ).increment(updated);
        }
        getLastEventsSavedGauge(schedulerName, "created").set(Math.max(created, 0));
        getLastEventsSavedGauge(schedulerName, "updated").set(Math.max(updated, 0));
    }

    public void recordNotifyRecipients(String schedulerName, long uniqueUsers, long deliveries) {
        if (uniqueUsers > 0) {
            meterRegistry.counter(
                "edu_notify_recipients_total",
                TAG_SCHEDULER, schedulerName,
                TAG_RECIPIENT_TYPE, "unique_users"
            ).increment(uniqueUsers);
        }
        if (deliveries > 0) {
            meterRegistry.counter(
                "edu_notify_recipients_total",
                TAG_SCHEDULER, schedulerName,
                TAG_RECIPIENT_TYPE, "deliveries"
            ).increment(deliveries);
        }
        getLastNotifyRecipientsGauge(schedulerName, "unique_users").set(Math.max(uniqueUsers, 0));
        getLastNotifyRecipientsGauge(schedulerName, "deliveries").set(Math.max(deliveries, 0));
    }

    private Timer phaseTimer(String schedulerName, String phase) {
        return Timer.builder("edu_scheduler_phase_duration_seconds")
            .description("Scheduler phase duration")
            .tags(TAG_SCHEDULER, schedulerName, TAG_PHASE, phase)
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
        SchedulerErrorReason reason
    ) {
        incrementCounter(
            "edu_external_api_calls_total",
            TAG_SCHEDULER, schedulerName,
            "client", client,
            "operation", operation,
            "outcome", outcome,
            "reason", reason.tag()
        );
    }

    private AtomicInteger getClusterPlacesGauge(String campusId, String clusterName, String placeType) {
        String key = campusId + "|" + clusterName + "|" + placeType;
        return clusterPlacesGauges.computeIfAbsent(key, ignored -> {
            AtomicInteger holder = new AtomicInteger(0);
            io.micrometer.core.instrument.Gauge.builder("edu_cluster_places_current", holder, AtomicInteger::get)
                .description("Current free/occupied places per cluster")
                .tags(
                    TAG_CAMPUS_ID, campusId,
                    TAG_CAMPUS_NAME, campusCatalog.campusName(campusId),
                    "cluster_name", clusterName,
                    "place_type", placeType
                )
                .register(meterRegistry);
            return holder;
        });
    }

    private AtomicLong getParticipantByCampusGauge(String campusId) {
        return participantByCampusGauges.computeIfAbsent(campusId, ignored -> {
            AtomicLong holder = new AtomicLong(0L);
            io.micrometer.core.instrument.Gauge.builder("edu_participants_by_campus", holder, AtomicLong::get)
                .description("Participants count by campus")
                .tags(
                    TAG_CAMPUS_ID, campusId,
                    TAG_CAMPUS_NAME, campusCatalog.campusName(campusId)
                )
                .register(meterRegistry);
            return holder;
        });
    }

    private AtomicLong getParticipantByCampusStageGroupGauge(String campusId, String stageGroupName) {
        String key = campusId + "|" + stageGroupName;
        return participantByCampusStageGroupGauges.computeIfAbsent(key, ignored -> {
            AtomicLong holder = new AtomicLong(0L);
            io.micrometer.core.instrument.Gauge.builder("edu_participants_by_campus_stage_group", holder, AtomicLong::get)
                .description("Participants count by campus and stage group")
                .tags(
                    TAG_CAMPUS_ID, campusId,
                    TAG_CAMPUS_NAME, campusCatalog.campusName(campusId),
                    "stage_group_name", stageGroupName
                )
                .register(meterRegistry);
            return holder;
        });
    }

    private AtomicLong getParticipantByCampusStageNameGauge(String campusId, String stageName) {
        String key = campusId + "|" + stageName;
        return participantByCampusStageNameGauges.computeIfAbsent(key, ignored -> {
            AtomicLong holder = new AtomicLong(0L);
            io.micrometer.core.instrument.Gauge.builder("edu_participants_by_campus_stage_name", holder, AtomicLong::get)
                .description("Participants count by campus and stage name")
                .tags(
                    TAG_CAMPUS_ID, campusId,
                    TAG_CAMPUS_NAME, campusCatalog.campusName(campusId),
                    "stage_name", stageName
                )
                .register(meterRegistry);
            return holder;
        });
    }

    private String normalizeStageLabel(String value) {
        return (value == null || value.isBlank()) ? "unknown" : value;
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
                .tags(TAG_SCHEDULER, schedulerName, TAG_PHASE, phase)
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
                .tags(TAG_SCHEDULER, schedulerName)
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
                .tags(TAG_SCHEDULER, schedulerName, TAG_RESULT, result)
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
                .tags(TAG_SCHEDULER, schedulerName, TAG_RECIPIENT_TYPE, recipientType)
                .register(meterRegistry);
            return holder;
        });
    }
}
