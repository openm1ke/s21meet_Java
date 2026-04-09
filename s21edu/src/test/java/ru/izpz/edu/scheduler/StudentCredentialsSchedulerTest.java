package ru.izpz.edu.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.dto.GraphQLStudentCredentialsDto;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.service.CampusCatalog;
import ru.izpz.edu.service.GraphQLService;
import ru.izpz.edu.service.SchedulerMetricsService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCredentialsSchedulerTest {

    @Mock
    private CampusClient campusClient;
    @Mock
    private CampusCatalog campusCatalog;
    @Mock
    private GraphQLService graphQLService;
    @Mock
    private StudentCredentialsRepository studentCredentialsRepository;
    @Mock
    private SchedulerMetricsService schedulerMetricsService;

    private StudentCredentialsScheduler scheduler;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        scheduler = new StudentCredentialsScheduler(
            campusClient,
            campusCatalog,
            graphQLService,
            studentCredentialsRepository,
            schedulerMetricsService,
            executorService
        );
        ReflectionTestUtils.setField(scheduler, "pageSize", 2);
        ReflectionTestUtils.setField(scheduler, "batchSize", 2);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void runNightlySync_runsSingleFullPhaseAndRefreshesOnlyMissingCampusLogins() throws Exception {
        when(campusCatalog.targetCampusIds()).thenReturn(List.of("msk"));
        when(campusClient.getParticipantsByCampus("msk", 2, 0)).thenReturn(List.of("new1", "known1"));
        when(campusClient.getParticipantsByCampus("msk", 2, 2)).thenReturn(List.of());
        when(studentCredentialsRepository.findExistingLogins(List.of("new1", "known1")))
            .thenReturn(List.of("known1"));

        when(graphQLService.refreshCredentialsWithLimits("new1"))
            .thenReturn(new GraphQLStudentCredentialsDto("s1", "u1", "school", true, false));
        scheduler.runNightlySync();

        verify(campusClient).getParticipantsByCampus("msk", 2, 0);
        verify(studentCredentialsRepository).findExistingLogins(List.of("new1", "known1"));
        verify(graphQLService).refreshCredentialsWithLimits("new1");
        verify(graphQLService, never()).refreshCredentialsWithLimits("known1");
        verify(schedulerMetricsService).setCredentialsSyncInProgress("credentials_sync", true);
        verify(schedulerMetricsService).setCredentialsSyncInProgress("credentials_sync", false);
    }

    @Test
    void runNightlySync_skipsWhenInvalidConfig() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 0);

        scheduler.runNightlySync();

        verifyNoInteractions(campusCatalog);
        verifyNoInteractions(campusClient);
        verifyNoInteractions(studentCredentialsRepository);
        verifyNoInteractions(graphQLService);
        verify(schedulerMetricsService, never()).recordCredentialsSyncRun(anyString(), any(), any());
    }
}
