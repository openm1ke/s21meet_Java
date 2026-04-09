package ru.izpz.edu.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.izpz.edu.scheduler.metrics.SchedulerErrorClassifier;
import ru.izpz.edu.scheduler.metrics.SchedulerErrorReason;
import ru.izpz.edu.scheduler.metrics.SchedulerPhaseRequestStatus;
import ru.izpz.edu.scheduler.metrics.SchedulerRunStatus;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerMetricsServiceTest {

    private SimpleMeterRegistry meterRegistry;
    private SchedulerMetricsService service;
    private CampusCatalog campusCatalog;
    private String campusId;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        campusCatalog = new CampusCatalog();
        service = new SchedulerMetricsService(meterRegistry, campusCatalog, new SchedulerErrorClassifier());
        campusId = campusCatalog.targetCampusIds().getFirst();
    }

    @Test
    void recordClusterPlaces_clampsValuesAndNormalizesNullName() {
        service.recordClusterPlaces(campusId, null, -5, 3);

        Gauge freeGauge = gauge(
            "edu_cluster_places_current",
            "campus_id", campusId,
            "campus_name", campusCatalog.campusName(campusId),
            "cluster_name", "unknown",
            "place_type", "free"
        );
        Gauge occupiedGauge = gauge(
            "edu_cluster_places_current",
            "campus_id", campusId,
            "campus_name", campusCatalog.campusName(campusId),
            "cluster_name", "unknown",
            "place_type", "occupied"
        );

        assertEquals(0.0, freeGauge.value());
        assertEquals(3.0, occupiedGauge.value());
    }

    @Test
    void recordClusterPlaces_normalizesBlankNameAndNegativeOccupied() {
        service.recordClusterPlaces(campusId, "   ", 4, -2);

        Gauge freeGauge = gauge(
            "edu_cluster_places_current",
            "campus_id", campusId,
            "campus_name", campusCatalog.campusName(campusId),
            "cluster_name", "unknown",
            "place_type", "free"
        );
        Gauge occupiedGauge = gauge(
            "edu_cluster_places_current",
            "campus_id", campusId,
            "campus_name", campusCatalog.campusName(campusId),
            "cluster_name", "unknown",
            "place_type", "occupied"
        );

        assertEquals(4.0, freeGauge.value());
        assertEquals(0.0, occupiedGauge.value());
    }

    @Test
    void recordClusterPlaces_keepsNonBlankName() {
        service.recordClusterPlaces(campusId, "A1", 2, 2);

        Gauge freeGauge = gauge(
            "edu_cluster_places_current",
            "campus_id", campusId,
            "campus_name", campusCatalog.campusName(campusId),
            "cluster_name", "A1",
            "place_type", "free"
        );

        assertEquals(2.0, freeGauge.value());
    }

    @Test
    void recordParticipantsMetrics_andReset_affectsGauges() {
        service.recordParticipantsByCampus(campusId, 7);
        service.recordParticipantsByCampusAndStageGroup(campusId, "GroupA", 5);
        service.recordParticipantsByCampusAndStageName(campusId, null, 2);
        String campusName = campusCatalog.campusName(campusId);

        Gauge campusGauge = gauge(
            "edu_participants_by_campus",
            "campus_id", campusId,
            "campus_name", campusName
        );
        Gauge groupGauge = gauge(
            "edu_participants_by_campus_stage_group",
            "campus_id", campusId,
            "campus_name", campusName,
            "stage_group_name", "GroupA"
        );
        Gauge stageGauge = gauge(
            "edu_participants_by_campus_stage_name",
            "campus_id", campusId,
            "campus_name", campusName,
            "stage_name", "unknown"
        );

        assertEquals(7.0, campusGauge.value());
        assertEquals(5.0, groupGauge.value());
        assertEquals(2.0, stageGauge.value());

        service.resetParticipantMetrics();

        assertEquals(0.0, campusGauge.value());
        assertEquals(0.0, groupGauge.value());
        assertEquals(0.0, stageGauge.value());
    }

    @Test
    void timers_andLastSuccess_updateGauges() {
        var sample = service.startPhaseTimer();
        service.stopPhaseTimer("scheduler", "phase", sample);
        service.recordLastSuccess("scheduler");

        Gauge phaseGauge = gauge(
            "edu_scheduler_phase_last_duration_seconds",
            "scheduler", "scheduler",
            "phase", "phase"
        );
        Gauge lastSuccessGauge = gauge(
            "edu_scheduler_last_success_timestamp_seconds",
            "scheduler", "scheduler"
        );

        assertTrue(phaseGauge.value() >= 0);
        assertTrue(lastSuccessGauge.value() > 0);
    }

    @Test
    void recordPhase_counters_useTags() {
        service.recordPhaseRequest("scheduler", "phase", SchedulerPhaseRequestStatus.SUCCESS);
        service.recordPhaseIssue("scheduler", "phase", SchedulerErrorReason.TIMEOUT);
        service.recordRunStatus("scheduler", SchedulerRunStatus.SUCCESS);

        Counter requestCounter = counter(
            "edu_scheduler_phase_requests_total",
            "scheduler", "scheduler",
            "phase", "phase",
            "status", SchedulerPhaseRequestStatus.SUCCESS.tag()
        );
        Counter issueCounter = counter(
            "edu_scheduler_phase_issues_total",
            "scheduler", "scheduler",
            "phase", "phase",
            "issue_type", SchedulerErrorReason.TIMEOUT.tag()
        );
        Counter runCounter = counter(
            "edu_scheduler_run_total",
            "scheduler", "scheduler",
            "status", SchedulerRunStatus.SUCCESS.tag()
        );

        assertEquals(1.0, requestCounter.count());
        assertEquals(1.0, issueCounter.count());
        assertEquals(1.0, runCounter.count());
    }

    @Test
    void recordExternalApiMetrics_tracksSuccessAndError() {
        service.recordExternalApiSuccess("scheduler", "client", "operation");
        service.recordExternalApiError("scheduler", "client", "operation", new RuntimeException("boom"));

        Counter successCounter = counter(
            "edu_external_api_calls_total",
            "scheduler", "scheduler",
            "client", "client",
            "operation", "operation",
            "outcome", "success",
            "reason", SchedulerErrorReason.NONE.tag()
        );
        Counter errorCounter = counter(
            "edu_external_api_calls_total",
            "scheduler", "scheduler",
            "client", "client",
            "operation", "operation",
            "outcome", "error",
            "reason", SchedulerErrorReason.EXECUTION_EXCEPTION.tag()
        );

        assertEquals(1.0, successCounter.count());
        assertEquals(1.0, errorCounter.count());
    }

    @Test
    void recordEventsAndNotifyRecipients_emitsCountersAndGauges() {
        service.recordEventsSaved("scheduler", 2, 3);
        service.recordNotifyRecipients("scheduler", 4, 5);

        Counter createdCounter = counter(
            "edu_events_saved_total",
            "scheduler", "scheduler",
            "result", "created"
        );
        Counter updatedCounter = counter(
            "edu_events_saved_total",
            "scheduler", "scheduler",
            "result", "updated"
        );
        Gauge createdGauge = gauge(
            "edu_events_saved_last",
            "scheduler", "scheduler",
            "result", "created"
        );
        Gauge recipientsGauge = gauge(
            "edu_notify_recipients_last",
            "scheduler", "scheduler",
            "recipient_type", "unique_users"
        );

        assertEquals(2.0, createdCounter.count());
        assertEquals(3.0, updatedCounter.count());
        assertEquals(2.0, createdGauge.value());
        assertEquals(4.0, recipientsGauge.value());
    }

    @Test
    void recordEventsAndNotifyRecipients_whenNonPositiveValues_onlyUpdatesLastGauges() {
        service.recordEventsSaved("scheduler", 0, -3);
        service.recordNotifyRecipients("scheduler", 0, -1);

        Counter createdCounter = meterRegistry.find("edu_events_saved_total")
            .tags("scheduler", "scheduler", "result", "created")
            .counter();
        Counter updatedCounter = meterRegistry.find("edu_events_saved_total")
            .tags("scheduler", "scheduler", "result", "updated")
            .counter();
        Counter uniqueUsersCounter = meterRegistry.find("edu_notify_recipients_total")
            .tags("scheduler", "scheduler", "recipient_type", "unique_users")
            .counter();
        Counter deliveriesCounter = meterRegistry.find("edu_notify_recipients_total")
            .tags("scheduler", "scheduler", "recipient_type", "deliveries")
            .counter();

        Gauge createdGauge = gauge(
            "edu_events_saved_last",
            "scheduler", "scheduler",
            "result", "created"
        );
        Gauge updatedGauge = gauge(
            "edu_events_saved_last",
            "scheduler", "scheduler",
            "result", "updated"
        );
        Gauge uniqueUsersGauge = gauge(
            "edu_notify_recipients_last",
            "scheduler", "scheduler",
            "recipient_type", "unique_users"
        );
        Gauge deliveriesGauge = gauge(
            "edu_notify_recipients_last",
            "scheduler", "scheduler",
            "recipient_type", "deliveries"
        );

        assertNull(createdCounter);
        assertNull(updatedCounter);
        assertNull(uniqueUsersCounter);
        assertNull(deliveriesCounter);
        assertEquals(0.0, createdGauge.value());
        assertEquals(0.0, updatedGauge.value());
        assertEquals(0.0, uniqueUsersGauge.value());
        assertEquals(0.0, deliveriesGauge.value());
    }

    @Test
    void recordParticipantsByCampusAndStageGroup_normalizesBlankStageName() {
        service.recordParticipantsByCampusAndStageGroup(campusId, " ", 3);

        Gauge groupGauge = gauge(
            "edu_participants_by_campus_stage_group",
            "campus_id", campusId,
            "campus_name", campusCatalog.campusName(campusId),
            "stage_group_name", "unknown"
        );

        assertEquals(3.0, groupGauge.value());
    }

    @Test
    void setCredentialsSyncInProgress_setsGaugeToOneAndZero() {
        service.setCredentialsSyncInProgress("credentials_sync", true);
        Gauge inProgress = gauge(
            "edu_credentials_sync_in_progress",
            "scheduler", "credentials_sync"
        );
        assertEquals(1.0, inProgress.value());

        service.setCredentialsSyncInProgress("credentials_sync", false);
        assertEquals(0.0, inProgress.value());
    }

    @Test
    void recordCredentialsSyncBatch_recordsCountersSummaryAndTimer() {
        service.recordCredentialsSyncBatch("credentials_sync", 5, 2, 1, 3, Duration.ofMillis(10));

        Counter batchesCounter = counter(
            "edu_credentials_sync_batches_total",
            "scheduler", "credentials_sync"
        );
        Counter successCounter = counter(
            "edu_credentials_sync_requests_total",
            "scheduler", "credentials_sync",
            "result", "success"
        );
        Counter noDataCounter = counter(
            "edu_credentials_sync_requests_total",
            "scheduler", "credentials_sync",
            "result", "no_data"
        );
        Counter failedCounter = counter(
            "edu_credentials_sync_requests_total",
            "scheduler", "credentials_sync",
            "result", "failed"
        );

        assertEquals(1.0, batchesCounter.count());
        assertEquals(2.0, successCounter.count());
        assertEquals(1.0, noDataCounter.count());
        assertEquals(3.0, failedCounter.count());
        assertEquals(1L, meterRegistry.find("edu_credentials_sync_batch_size").tags("scheduler", "credentials_sync").summary().count());
        assertFalse(
            meterRegistry.find("edu_credentials_sync_batch_duration_seconds").tags("scheduler", "credentials_sync").timer().totalTime(java.util.concurrent.TimeUnit.NANOSECONDS) <= 0
        );
    }

    @Test
    void recordCredentialsSyncBatch_withNonPositiveValuesAndNullDuration_doesNotCreateOutcomeCounters() {
        service.recordCredentialsSyncBatch("credentials_sync", -10, 0, 0, 0, null);

        Counter successCounter = meterRegistry.find("edu_credentials_sync_requests_total")
            .tags("scheduler", "credentials_sync", "result", "success")
            .counter();
        Counter noDataCounter = meterRegistry.find("edu_credentials_sync_requests_total")
            .tags("scheduler", "credentials_sync", "result", "no_data")
            .counter();
        Counter failedCounter = meterRegistry.find("edu_credentials_sync_requests_total")
            .tags("scheduler", "credentials_sync", "result", "failed")
            .counter();

        assertNull(successCounter);
        assertNull(noDataCounter);
        assertNull(failedCounter);
        assertEquals(1L, meterRegistry.find("edu_credentials_sync_batch_size").tags("scheduler", "credentials_sync").summary().count());
    }

    @Test
    void recordCredentialsSyncRun_recordsAllOutcomesAndGauges() {
        service.recordCredentialsSyncRun(
            "credentials_sync",
            new SchedulerMetricsService.CredentialsSyncRunStats(0, 0, 0, 0, 0, 0),
            Duration.ofMillis(1)
        );
        service.recordCredentialsSyncRun(
            "credentials_sync",
            new SchedulerMetricsService.CredentialsSyncRunStats(10, 5, 2, 3, 1, 1),
            Duration.ofMillis(2)
        );
        service.recordCredentialsSyncRun(
            "credentials_sync",
            new SchedulerMetricsService.CredentialsSyncRunStats(10, 5, 2, 5, 0, 0),
            null
        );

        Counter skippedCounter = counter(
            "edu_credentials_sync_runs_total",
            "scheduler", "credentials_sync",
            "outcome", "skipped"
        );
        Counter partialCounter = counter(
            "edu_credentials_sync_runs_total",
            "scheduler", "credentials_sync",
            "outcome", "partial"
        );
        Counter successCounter = counter(
            "edu_credentials_sync_runs_total",
            "scheduler", "credentials_sync",
            "outcome", "success"
        );

        assertEquals(1.0, skippedCounter.count());
        assertEquals(1.0, partialCounter.count());
        assertEquals(1.0, successCounter.count());

        Gauge requestedGauge = gauge(
            "edu_credentials_sync_last",
            "scheduler", "credentials_sync",
            "metric", "requested"
        );
        Gauge failedGauge = gauge(
            "edu_credentials_sync_last",
            "scheduler", "credentials_sync",
            "metric", "failed"
        );
        Gauge updatedAtGauge = gauge(
            "edu_credentials_sync_last",
            "scheduler", "credentials_sync",
            "metric", "updated_at"
        );

        assertEquals(5.0, requestedGauge.value());
        assertEquals(0.0, failedGauge.value());
        assertTrue(updatedAtGauge.value() > 0);
    }

    private Gauge gauge(String name, String... tags) {
        Gauge gauge = meterRegistry.find(name).tags(tags).gauge();
        assertNotNull(gauge, "Gauge " + name + " with tags " + List.of(tags) + " should exist");
        return gauge;
    }

    private Counter counter(String name, String... tags) {
        Counter counter = meterRegistry.find(name).tags(tags).counter();
        assertNotNull(counter, "Counter " + name + " with tags " + List.of(tags) + " should exist");
        return counter;
    }
}
