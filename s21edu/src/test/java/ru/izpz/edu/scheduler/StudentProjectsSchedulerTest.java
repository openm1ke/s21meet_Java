package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import ru.izpz.edu.config.ProjectsProviderConfig;
import ru.izpz.edu.model.StudentCredentials;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.service.CampusCatalog;
import ru.izpz.edu.service.provider.CampusRoutingProjectsProvider;

import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentProjectsSchedulerTest {

    @Mock
    private StudentCredentialsRepository studentCredentialsRepository;
    @Mock
    private CampusRoutingProjectsProvider campusRoutingProjectsProvider;
    @Mock
    private CampusCatalog campusCatalog;

    private ExecutorService executorService;
    private StudentProjectsScheduler scheduler;
    private ProjectsProviderConfig.ProjectsProperties projectsProperties;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        projectsProperties = new ProjectsProviderConfig.ProjectsProperties();
        scheduler = new StudentProjectsScheduler(
            studentCredentialsRepository,
            campusRoutingProjectsProvider,
            campusCatalog,
            projectsProperties,
            executorService
        );
        ReflectionTestUtils.setField(scheduler, "pageSize", 2);
        ReflectionTestUtils.setField(scheduler, "batchSize", 2);
        ReflectionTestUtils.setField(scheduler, "graphQlBatchSize", 2);
        ReflectionTestUtils.setField(scheduler, "restBatchSize", 2);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void refreshActiveProjects_prioritizesCampusesAndProcessesOnlyStale() {
        String mskId = "6bfe3c56-0211-4fe1-9e59-51616caac4dd";
        String kznId = "7c293c9c-f28c-4b10-be29-560e4b000a34";
        String nskId = "46e7d965-21e9-4936-bea9-f5ea0d1fddf2";
        StudentCredentials msk = credential("a1", "u1", mskId);
        StudentCredentials kzn = credential("a2", "u2", kznId);

        when(campusCatalog.targetCampusIds()).thenReturn(List.of(mskId, kznId, nskId));
        when(campusRoutingProjectsProvider.providerTypeForSchoolId(mskId)).thenReturn("graphql");
        when(campusRoutingProjectsProvider.providerTypeForSchoolId(kznId)).thenReturn("rest");
        when(campusRoutingProjectsProvider.providerTypeForSchoolId(nskId)).thenReturn("rest");
        when(studentCredentialsRepository.countActiveCredentialsBySchoolId(mskId)).thenReturn(1L);
        when(studentCredentialsRepository.countActiveCredentialsBySchoolId(kznId)).thenReturn(1L);
        when(studentCredentialsRepository.countActiveCredentialsBySchoolId(nskId)).thenReturn(0L);
        when(studentCredentialsRepository.countStaleActiveCredentialsBySchoolId(eq(mskId), any(OffsetDateTime.class))).thenReturn(1L);
        when(studentCredentialsRepository.countStaleActiveCredentialsBySchoolId(eq(kznId), any(OffsetDateTime.class))).thenReturn(1L);
        when(studentCredentialsRepository.countStaleActiveCredentialsBySchoolId(eq(nskId), any(OffsetDateTime.class))).thenReturn(0L);
        when(studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(eq(""), eq(mskId), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(msk));
        when(studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(eq("a1"), eq(mskId), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());
        when(studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(eq(""), eq(kznId), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(kzn));
        when(studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(eq("a2"), eq(kznId), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());
        when(studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(eq(""), eq(nskId), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());
        when(campusRoutingProjectsProvider.refreshStudentProjects(msk))
            .thenReturn(CampusRoutingProjectsProvider.RefreshResult.SUCCESS);
        when(campusRoutingProjectsProvider.refreshStudentProjects(kzn))
            .thenReturn(CampusRoutingProjectsProvider.RefreshResult.SUCCESS);

        scheduler.refreshActiveProjects();

        verify(campusRoutingProjectsProvider).refreshStudentProjects(msk);
        verify(campusRoutingProjectsProvider).refreshStudentProjects(kzn);
    }

    @Test
    void refreshActiveProjects_skipsWhenInvalidConfig() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 0);
        ReflectionTestUtils.setField(scheduler, "graphQlBatchSize", 0);
        ReflectionTestUtils.setField(scheduler, "restBatchSize", 0);

        scheduler.refreshActiveProjects();

        verify(studentCredentialsRepository, never()).findStaleActiveCredentialsAfterBySchoolId(any(), any(), any(), any(Pageable.class));
        verify(studentCredentialsRepository, never()).countActiveCredentialsBySchoolId(any());
        verify(campusRoutingProjectsProvider, never()).refreshStudentProjects(any(StudentCredentials.class));
    }

    @Test
    void refreshActiveProjects_skipsWhenInvalidPageSize() {
        ReflectionTestUtils.setField(scheduler, "pageSize", 0);

        scheduler.refreshActiveProjects();

        verify(studentCredentialsRepository, never()).findStaleActiveCredentialsAfterBySchoolId(any(), any(), any(), any(Pageable.class));
        verify(campusRoutingProjectsProvider, never()).refreshStudentProjects(any(StudentCredentials.class));
    }

    @Test
    void logSchedulerInit_doesNotThrow() {
        assertDoesNotThrow(scheduler::logSchedulerInit);
    }

    @Test
    void refreshActiveProjects_handlesSkippedAndFailedResults() {
        String mskId = "6bfe3c56-0211-4fe1-9e59-51616caac4dd";
        StudentCredentials skipped = credential("skip-login", "u1", mskId);
        StudentCredentials failed = credential("fail-login", "u2", mskId);

        when(campusCatalog.targetCampusIds()).thenReturn(List.of(mskId));
        when(campusCatalog.campusName(mskId)).thenReturn("MSK");
        when(campusRoutingProjectsProvider.providerTypeForSchoolId(mskId)).thenReturn("graphql");
        when(studentCredentialsRepository.countActiveCredentialsBySchoolId(mskId)).thenReturn(2L);
        when(studentCredentialsRepository.countStaleActiveCredentialsBySchoolId(eq(mskId), any(OffsetDateTime.class))).thenReturn(2L);
        when(studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(eq(""), eq(mskId), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(skipped, failed));
        when(studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(eq("fail-login"), eq(mskId), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());
        when(campusRoutingProjectsProvider.refreshStudentProjects(skipped))
            .thenReturn(CampusRoutingProjectsProvider.RefreshResult.SKIPPED_NO_PROVIDER);
        when(campusRoutingProjectsProvider.refreshStudentProjects(failed))
            .thenThrow(new RuntimeException("boom"));

        scheduler.refreshActiveProjects();

        verify(campusRoutingProjectsProvider).refreshStudentProjects(skipped);
        verify(campusRoutingProjectsProvider).refreshStudentProjects(failed);
    }

    @Test
    void refreshActiveProjects_handlesTimeoutResult() {
        String mskId = "6bfe3c56-0211-4fe1-9e59-51616caac4dd";
        StudentCredentials timeoutCandidate = credential("timeout-login", "u1", mskId);

        ReflectionTestUtils.setField(scheduler, "taskTimeout", Duration.ofMillis(1));
        ReflectionTestUtils.setField(scheduler, "batchSize", 1);
        ReflectionTestUtils.setField(scheduler, "graphQlBatchSize", 1);
        ReflectionTestUtils.setField(scheduler, "restBatchSize", 1);

        when(campusCatalog.targetCampusIds()).thenReturn(List.of(mskId));
        when(campusCatalog.campusName(mskId)).thenReturn("MSK");
        when(campusRoutingProjectsProvider.providerTypeForSchoolId(mskId)).thenReturn("graphql");
        when(studentCredentialsRepository.countActiveCredentialsBySchoolId(mskId)).thenReturn(1L);
        when(studentCredentialsRepository.countStaleActiveCredentialsBySchoolId(eq(mskId), any(OffsetDateTime.class))).thenReturn(1L);
        when(studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(eq(""), eq(mskId), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(timeoutCandidate));
        when(studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(eq("timeout-login"), eq(mskId), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());
        when(campusRoutingProjectsProvider.refreshStudentProjects(timeoutCandidate))
            .thenAnswer(invocation -> {
                LockSupport.parkNanos(Duration.ofMillis(50).toNanos());
                return CampusRoutingProjectsProvider.RefreshResult.SUCCESS;
            });

        scheduler.refreshActiveProjects();

        verify(campusRoutingProjectsProvider).refreshStudentProjects(timeoutCandidate);
    }

    @Test
    void refreshActiveProjects_coversMiddleBatchDebugPath() {
        String mskId = "6bfe3c56-0211-4fe1-9e59-51616caac4dd";
        StudentCredentials c1 = credential("a1", "u1", mskId);
        StudentCredentials c2 = credential("a2", "u2", mskId);
        StudentCredentials c3 = credential("a3", "u3", mskId);

        ReflectionTestUtils.setField(scheduler, "batchSize", 1);
        ReflectionTestUtils.setField(scheduler, "graphQlBatchSize", 1);
        ReflectionTestUtils.setField(scheduler, "restBatchSize", 1);
        ReflectionTestUtils.setField(scheduler, "pageSize", 10);

        when(campusCatalog.targetCampusIds()).thenReturn(List.of(mskId));
        when(campusCatalog.campusName(mskId)).thenReturn("MSK");
        when(campusRoutingProjectsProvider.providerTypeForSchoolId(mskId)).thenReturn("rest");
        when(studentCredentialsRepository.countActiveCredentialsBySchoolId(mskId)).thenReturn(3L);
        when(studentCredentialsRepository.countStaleActiveCredentialsBySchoolId(eq(mskId), any(OffsetDateTime.class))).thenReturn(3L);
        when(studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(eq(""), eq(mskId), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(c1, c2, c3));
        when(studentCredentialsRepository.findStaleActiveCredentialsAfterBySchoolId(eq("a3"), eq(mskId), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());
        when(campusRoutingProjectsProvider.refreshStudentProjects(any(StudentCredentials.class)))
            .thenReturn(CampusRoutingProjectsProvider.RefreshResult.SUCCESS);

        scheduler.refreshActiveProjects();

        verify(campusRoutingProjectsProvider).refreshStudentProjects(c1);
        verify(campusRoutingProjectsProvider).refreshStudentProjects(c2);
        verify(campusRoutingProjectsProvider).refreshStudentProjects(c3);
    }

    private StudentCredentials credential(String login, String userId, String schoolId) {
        StudentCredentials credentials = new StudentCredentials();
        credentials.setLogin(login);
        credentials.setUserId(userId);
        credentials.setSchoolId(schoolId);
        credentials.setIsActive(true);
        return credentials;
    }
}
