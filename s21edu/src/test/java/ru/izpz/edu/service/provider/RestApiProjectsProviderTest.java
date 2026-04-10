package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.model.ParticipantProjectV1DTO;
import ru.izpz.dto.model.ParticipantProjectsV1DTO;
import ru.izpz.edu.config.ProjectsProviderConfig;
import ru.izpz.edu.dto.StudentProjectData;
import ru.izpz.edu.model.StudentProject;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.repository.StudentProjectRepository;
import ru.izpz.edu.service.StudentProjectRefreshService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestApiProjectsProviderTest {

    @Mock
    private RestProjectsApiFacade restProjectsApiFacade;
    @Mock
    private StudentProjectRepository studentProjectRepository;
    @Mock
    private StudentProjectRefreshService studentProjectRefreshService;
    @Mock
    private StudentCredentialsRepository studentCredentialsRepository;

    private RestApiProjectsProvider provider;
    private AtomicReference<List<StudentProject>> cachedProjects;
    private AtomicReference<OffsetDateTime> maxUpdatedAt;

    @BeforeEach
    void setUp() {
        ProjectsProviderConfig.ProjectsProperties properties = new ProjectsProviderConfig.ProjectsProperties();
        properties.getRest().setPageSize(1000);
        cachedProjects = new AtomicReference<>(List.of());
        maxUpdatedAt = new AtomicReference<>(null);
        provider = new RestApiProjectsProvider(
            restProjectsApiFacade,
            properties,
            studentProjectRepository,
            studentProjectRefreshService,
            studentCredentialsRepository
        );

        lenient().when(studentProjectRepository.findAllByLoginAndSnapshotFalseOrderBySortOrderAsc("login"))
            .thenAnswer(invocation -> cachedProjects.get());
        lenient().when(studentProjectRepository.findMaxUpdatedAtByLogin("login"))
            .thenAnswer(invocation -> maxUpdatedAt.get());
        lenient().when(studentCredentialsRepository.findById("login"))
            .thenReturn(Optional.empty());
        lenient().doAnswer(invocation -> {
            String login = invocation.getArgument(0);
            String userId = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            List<StudentProjectData> projects = invocation.getArgument(2, List.class);
            cachedProjects.set(toEntities(login, userId, projects));
            maxUpdatedAt.set(OffsetDateTime.now());
            return null;
        }).when(studentProjectRefreshService).replaceProjects(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void getStudentProjectsByLogin_shouldFetchAllStatusesWithSingleRequest() throws ApiException {
        ParticipantProjectV1DTO p1 = new ParticipantProjectV1DTO();
        p1.setId(100L);
        p1.setTitle("CPP1");
        p1.setStatus(ParticipantProjectV1DTO.StatusEnum.IN_PROGRESS);

        ParticipantProjectV1DTO p2 = new ParticipantProjectV1DTO();
        p2.setId(101L);
        p2.setTitle("CPP2");
        p2.setStatus(ParticipantProjectV1DTO.StatusEnum.REGISTERED);

        ParticipantProjectsV1DTO response = new ParticipantProjectsV1DTO();
        response.setProjects(List.of(p1, p2));

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, null))
            .thenReturn(response);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(2, result.size());
        assertEquals("100", result.get(0).goalId());
        assertEquals("101", result.get(1).goalId());
        assertEquals("IN_PROGRESS", result.get(0).goalStatus());
        assertEquals("REGISTERED", result.get(1).goalStatus());
        assertNull(result.get(0).description());
        assertNull(result.get(0).experience());
        assertNull(result.get(0).laboriousness());
        verify(restProjectsApiFacade).getParticipantProjectsByLogin("login", 1000L, null);
    }

    @Test
    void getStudentProjectsByLogin_shouldSkipAssigned() throws ApiException {
        ParticipantProjectV1DTO assigned = new ParticipantProjectV1DTO();
        assigned.setId(10L);
        assigned.setTitle("Assigned");
        assigned.setStatus(ParticipantProjectV1DTO.StatusEnum.ASSIGNED);

        ParticipantProjectV1DTO inProgress = new ParticipantProjectV1DTO();
        inProgress.setId(11L);
        inProgress.setTitle("InProgress");
        inProgress.setStatus(ParticipantProjectV1DTO.StatusEnum.IN_PROGRESS);

        ParticipantProjectsV1DTO response = new ParticipantProjectsV1DTO();
        response.setProjects(List.of(assigned, inProgress));

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, null))
            .thenReturn(response);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        assertEquals("11", result.getFirst().goalId());
    }

    @Test
    void getStudentProjectsByLogin_shouldSkipDuplicatesByProjectId() throws ApiException {
        ParticipantProjectV1DTO p1 = new ParticipantProjectV1DTO();
        p1.setId(100L);
        p1.setTitle("CPP1");
        p1.setStatus(ParticipantProjectV1DTO.StatusEnum.IN_PROGRESS);

        ParticipantProjectV1DTO duplicate = new ParticipantProjectV1DTO();
        duplicate.setId(100L);
        duplicate.setTitle("CPP1-dup");
        duplicate.setStatus(ParticipantProjectV1DTO.StatusEnum.REGISTERED);

        ParticipantProjectsV1DTO response = new ParticipantProjectsV1DTO();
        response.setProjects(List.of(p1, duplicate));

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, null))
            .thenReturn(response);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        assertEquals("100", result.getFirst().goalId());
    }

    @Test
    void getStudentProjectsByLogin_shouldKeepCacheWhenRequestFails() throws ApiException {
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, null))
            .thenThrow(new ApiException("boom"));

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
        verify(studentProjectRefreshService, never()).replaceProjects(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void getStudentProjectsByLogin_shouldKeepCacheWhenRuntimeError() throws ApiException {
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, null))
            .thenThrow(new IllegalStateException("runtime-boom"));

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
        verify(studentProjectRefreshService, never()).replaceProjects(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void getStudentProjectsByLogin_shouldHandleNullResponseAndNullProjectsList() throws ApiException {
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, null))
            .thenReturn(null);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");
        assertTrue(result.isEmpty());

        maxUpdatedAt.set(null);
        ParticipantProjectsV1DTO withNullProjects = new ParticipantProjectsV1DTO();
        withNullProjects.setProjects(null);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, null))
            .thenReturn(withNullProjects);

        result = provider.getStudentProjectsByLogin("login");
        assertTrue(result.isEmpty());
    }

    @Test
    void getStudentProjectsByLogin_shouldSkipNullProjectAndNullId() throws ApiException {
        ParticipantProjectV1DTO valid = new ParticipantProjectV1DTO();
        valid.setId(200L);
        valid.setTitle("CPP");
        valid.setStatus(ParticipantProjectV1DTO.StatusEnum.REGISTERED);

        ParticipantProjectV1DTO noId = new ParticipantProjectV1DTO();
        noId.setId(null);
        noId.setTitle("Broken");
        noId.setStatus(ParticipantProjectV1DTO.StatusEnum.IN_PROGRESS);

        ParticipantProjectsV1DTO response = new ParticipantProjectsV1DTO();
        response.setProjects(new ArrayList<>(java.util.Arrays.asList(null, noId, valid)));

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, null))
            .thenReturn(response);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        assertEquals("200", result.getFirst().goalId());
    }

    @Test
    void getStudentProjectsByLogin_shouldMapCourseIdAndDateAndMembers() throws ApiException {
        ParticipantProjectV1DTO project = new ParticipantProjectV1DTO();
        project.setId(200L);
        project.setTitle("Exam");
        project.setType(null);
        project.setStatus(ParticipantProjectV1DTO.StatusEnum.ACCEPTED);
        project.setFinalPercentage(95);
        project.setCompletionDateTime(OffsetDateTime.parse("2026-03-24T10:00:00Z"));
        project.setTeamMembers(List.of());
        project.setCourseId(Long.MAX_VALUE);

        ParticipantProjectsV1DTO response = new ParticipantProjectsV1DTO();
        response.setProjects(List.of(project));

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, null))
            .thenReturn(response);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        StudentProjectData data = result.getFirst();
        assertEquals("ACCEPTED", data.goalStatus());
        assertNull(data.executionType());
        assertEquals(Integer.valueOf(0), data.amountMembers());
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), data.localCourseId());
        assertEquals("2026-03-24T10:00Z", data.dateTime());
    }

    @Test
    void getStudentProjectsByLogin_shouldUseMinimumPageSizeWhenConfiguredNonPositive() throws ApiException {
        ProjectsProviderConfig.ProjectsProperties properties = new ProjectsProviderConfig.ProjectsProperties();
        properties.getRest().setPageSize(0);
        cachedProjects = new AtomicReference<>(List.of());
        maxUpdatedAt = new AtomicReference<>(null);
        provider = new RestApiProjectsProvider(
            restProjectsApiFacade,
            properties,
            studentProjectRepository,
            studentProjectRefreshService,
            studentCredentialsRepository
        );
        lenient().when(studentProjectRepository.findAllByLoginAndSnapshotFalseOrderBySortOrderAsc("login"))
            .thenAnswer(invocation -> cachedProjects.get());
        lenient().when(studentProjectRepository.findMaxUpdatedAtByLogin("login"))
            .thenAnswer(invocation -> maxUpdatedAt.get());
        lenient().when(studentCredentialsRepository.findById("login"))
            .thenReturn(Optional.empty());
        lenient().doAnswer(invocation -> {
            String login = invocation.getArgument(0);
            String userId = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            List<StudentProjectData> projects = invocation.getArgument(2, List.class);
            cachedProjects.set(toEntities(login, userId, projects));
            maxUpdatedAt.set(OffsetDateTime.now());
            return null;
        }).when(studentProjectRefreshService).replaceProjects(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyList()
        );

        ParticipantProjectsV1DTO response = new ParticipantProjectsV1DTO();
        response.setProjects(List.of());
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1L, null)).thenReturn(response);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
        verify(restProjectsApiFacade).getParticipantProjectsByLogin("login", 1L, null);
    }

    @Test
    void getStudentProjectsByLogin_shouldSkipRefreshWhenCacheIsFresh() throws ApiException {
        ParticipantProjectsV1DTO response = new ParticipantProjectsV1DTO();
        response.setProjects(List.of());
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, null)).thenReturn(response);

        provider.getStudentProjectsByLogin("login");
        provider.getStudentProjectsByLogin("login");

        verify(restProjectsApiFacade, times(1)).getParticipantProjectsByLogin("login", 1000L, null);
    }

    private List<StudentProject> toEntities(String login, String userId, List<StudentProjectData> projects) {
        List<StudentProject> entities = new ArrayList<>();
        for (int i = 0; i < projects.size(); i++) {
            StudentProjectData data = projects.get(i);
            StudentProject entity = new StudentProject();
            entity.setLogin(login);
            entity.setUserId(userId);
            entity.setGoalId(data.goalId());
            entity.setName(data.name());
            entity.setDescription(data.description());
            entity.setExperience(data.experience());
            entity.setDateTime(data.dateTime());
            entity.setFinalPercentage(data.finalPercentage());
            entity.setLaboriousness(data.laboriousness());
            entity.setExecutionType(data.executionType());
            entity.setGoalStatus(data.goalStatus());
            entity.setAmountMembers(data.amountMembers());
            entity.setLocalCourseId(data.localCourseId());
            entity.setSortOrder(i);
            entity.setSnapshot(false);
            entity.setUpdatedAt(OffsetDateTime.now());
            entities.add(entity);
        }
        return entities;
    }
}
