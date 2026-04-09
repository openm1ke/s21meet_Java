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

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

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
        }).when(studentProjectRefreshService).replaceProjects(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void getStudentProjectsByLogin_shouldMergeInProgressAndRegistered() throws ApiException {
        ParticipantProjectV1DTO inProgressProject = new ParticipantProjectV1DTO();
        inProgressProject.setId(100L);
        inProgressProject.setTitle("CPP1");

        ParticipantProjectV1DTO registeredProject = new ParticipantProjectV1DTO();
        registeredProject.setId(101L);
        registeredProject.setTitle("CPP2");

        ParticipantProjectsV1DTO inProgressResponse = new ParticipantProjectsV1DTO();
        inProgressResponse.setProjects(List.of(inProgressProject));
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of(registeredProject));

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS"))
                .thenReturn(inProgressResponse);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED"))
                .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(2, result.size());
        assertEquals("100", result.get(0).goalId());
        assertEquals("101", result.get(1).goalId());
        assertEquals("IN_PROGRESS", result.get(0).goalStatus());
        assertEquals("REGISTERED", result.get(1).goalStatus());
        assertNull(result.get(0).description());
        assertNull(result.get(0).experience());
        assertNull(result.get(0).laboriousness());
        assertNull(result.get(1).description());
        assertNull(result.get(1).experience());
        assertNull(result.get(1).laboriousness());
    }

    @Test
    void getStudentProjectsByLogin_shouldSkipDuplicatesByProjectId() throws ApiException {
        ParticipantProjectV1DTO inProgressProject = new ParticipantProjectV1DTO();
        inProgressProject.setId(100L);
        inProgressProject.setTitle("CPP1");

        ParticipantProjectV1DTO registeredDuplicate = new ParticipantProjectV1DTO();
        registeredDuplicate.setId(100L);
        registeredDuplicate.setTitle("CPP1");

        ParticipantProjectsV1DTO inProgressResponse = new ParticipantProjectsV1DTO();
        inProgressResponse.setProjects(List.of(inProgressProject));
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of(registeredDuplicate));

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS"))
                .thenReturn(inProgressResponse);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED"))
                .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        assertEquals("100", result.getFirst().goalId());
    }

    @Test
    void getStudentProjectsByLogin_shouldKeepCacheWhenStatusRequestFails() throws ApiException {
        ParticipantProjectV1DTO registeredProject = new ParticipantProjectV1DTO();
        registeredProject.setId(101L);
        registeredProject.setTitle("CPP2");
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of(registeredProject));

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS"))
                .thenThrow(new ApiException("boom"));
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED"))
                .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
        verify(restProjectsApiFacade).getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS");
        verify(restProjectsApiFacade).getParticipantProjectsByLogin("login", 1000L, "REGISTERED");
        verify(studentProjectRefreshService, never())
            .replaceProjects(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void getStudentProjectsByLogin_shouldKeepCacheWhenStatusRequestThrowsRuntimeException() throws ApiException {
        ParticipantProjectV1DTO registeredProject = new ParticipantProjectV1DTO();
        registeredProject.setId(101L);
        registeredProject.setTitle("CPP2");
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of(registeredProject));

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS"))
                .thenThrow(new IllegalStateException("runtime-boom"));
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED"))
                .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
        verify(studentProjectRefreshService, never())
            .replaceProjects(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void getStudentProjectsByLogin_shouldKeepProjectWithoutDetailsEnrichment() throws ApiException {
        ParticipantProjectV1DTO inProgressProject = new ParticipantProjectV1DTO();
        inProgressProject.setId(100L);
        inProgressProject.setTitle("CPP1");

        ParticipantProjectsV1DTO inProgressResponse = new ParticipantProjectsV1DTO();
        inProgressResponse.setProjects(List.of(inProgressProject));
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of());

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS"))
                .thenReturn(inProgressResponse);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED"))
                .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        assertEquals("100", result.getFirst().goalId());
        assertNull(result.getFirst().description());
        assertNull(result.getFirst().experience());
        assertNull(result.getFirst().laboriousness());
    }

    @Test
    void getStudentProjectsByLogin_shouldSkipNullProjectAndContinue() throws ApiException {
        ParticipantProjectV1DTO valid = new ParticipantProjectV1DTO();
        valid.setId(200L);
        valid.setTitle("CPP");

        ParticipantProjectsV1DTO inProgressResponse = new ParticipantProjectsV1DTO();
        inProgressResponse.setProjects(new java.util.ArrayList<>(java.util.Arrays.asList(null, valid)));
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of());

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS"))
            .thenReturn(inProgressResponse);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED"))
            .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        assertEquals("200", result.getFirst().goalId());
    }

    @Test
    void getStudentProjectsByLogin_shouldReturnEmpty_whenResponsesAreNull() throws ApiException {
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS"))
                .thenReturn(null);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED"))
                .thenReturn(new ParticipantProjectsV1DTO());

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
    }

    @Test
    void getStudentProjectsByLogin_shouldUseFallbackStatusAndClampCourseId() throws ApiException {
        ParticipantProjectV1DTO project = new ParticipantProjectV1DTO();
        project.setId(200L);
        project.setTitle("Exam");
        project.setType(null);
        project.setStatus(null);
        project.setFinalPercentage(95);
        project.setCompletionDateTime(OffsetDateTime.parse("2026-03-24T10:00:00Z"));
        project.setTeamMembers(Collections.emptyList());
        project.setCourseId(Long.MAX_VALUE);

        ParticipantProjectsV1DTO inProgressResponse = new ParticipantProjectsV1DTO();
        inProgressResponse.setProjects(List.of(project));
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of());

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS"))
                .thenReturn(inProgressResponse);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED"))
                .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        StudentProjectData data = result.getFirst();
        assertEquals("IN_PROGRESS", data.goalStatus());
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
        }).when(studentProjectRefreshService).replaceProjects(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList());

        ParticipantProjectsV1DTO response = new ParticipantProjectsV1DTO();
        response.setProjects(List.of());
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1L, "IN_PROGRESS")).thenReturn(response);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1L, "REGISTERED")).thenReturn(response);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
        verify(restProjectsApiFacade).getParticipantProjectsByLogin("login", 1L, "IN_PROGRESS");
        verify(restProjectsApiFacade).getParticipantProjectsByLogin("login", 1L, "REGISTERED");
    }

    @Test
    void getStudentProjectsByLogin_shouldSkipRefreshWhenCacheIsFresh() throws ApiException {
        ParticipantProjectsV1DTO response = new ParticipantProjectsV1DTO();
        response.setProjects(List.of());
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS")).thenReturn(response);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED")).thenReturn(response);

        provider.getStudentProjectsByLogin("login");
        provider.getStudentProjectsByLogin("login");

        verify(restProjectsApiFacade).getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS");
        verify(restProjectsApiFacade).getParticipantProjectsByLogin("login", 1000L, "REGISTERED");
    }

    @Test
    void getStudentProjectsByLogin_shouldSkipProjectWithNullId() throws ApiException {
        ParticipantProjectV1DTO noId = new ParticipantProjectV1DTO();
        noId.setId(null);
        noId.setTitle("Broken");

        ParticipantProjectV1DTO valid = new ParticipantProjectV1DTO();
        valid.setId(401L);
        valid.setTitle("Valid");

        ParticipantProjectsV1DTO inProgressResponse = new ParticipantProjectsV1DTO();
        inProgressResponse.setProjects(List.of(noId, valid));
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of());

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS"))
            .thenReturn(inProgressResponse);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED"))
            .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        assertEquals("401", result.getFirst().goalId());
    }

    @Test
    void getStudentProjectsByLogin_shouldContinueWhenResponseContainsNullProjectsList() throws ApiException {
        ParticipantProjectV1DTO registeredProject = new ParticipantProjectV1DTO();
        registeredProject.setId(501L);
        registeredProject.setTitle("Registered");

        ParticipantProjectsV1DTO inProgressResponse = new ParticipantProjectsV1DTO();
        inProgressResponse.setProjects(null);
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of(registeredProject));

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS"))
            .thenReturn(inProgressResponse);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED"))
            .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        assertEquals("501", result.getFirst().goalId());
    }

    @Test
    void getStudentProjectsByLogin_shouldPreferProjectStatusAndHandleMinAndNormalCourseId() throws ApiException {
        ParticipantProjectV1DTO minProject = new ParticipantProjectV1DTO();
        minProject.setId(301L);
        minProject.setTitle("MinCourse");
        minProject.setType(ParticipantProjectV1DTO.TypeEnum.GROUP);
        minProject.setStatus(ParticipantProjectV1DTO.StatusEnum.ACCEPTED);
        minProject.setCourseId(Long.MIN_VALUE);

        ParticipantProjectV1DTO normalProject = new ParticipantProjectV1DTO();
        normalProject.setId(302L);
        normalProject.setTitle("NormalCourse");
        normalProject.setType(ParticipantProjectV1DTO.TypeEnum.INDIVIDUAL);
        normalProject.setStatus(ParticipantProjectV1DTO.StatusEnum.FAILED);
        normalProject.setCourseId(42L);

        ParticipantProjectsV1DTO inProgressResponse = new ParticipantProjectsV1DTO();
        inProgressResponse.setProjects(List.of(minProject, normalProject));
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of());

        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS"))
                .thenReturn(inProgressResponse);
        when(restProjectsApiFacade.getParticipantProjectsByLogin("login", 1000L, "REGISTERED"))
                .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(2, result.size());
        assertEquals("ACCEPTED", result.get(0).goalStatus());
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), result.get(0).localCourseId());
        assertEquals("FAILED", result.get(1).goalStatus());
        assertEquals(Integer.valueOf(42), result.get(1).localCourseId());
    }

    @Test
    void firstNonBlank_shouldFallbackWhenFirstIsBlank() throws Exception {
        Method method = RestApiProjectsProvider.class.getDeclaredMethod("firstNonBlank", String.class, String.class);
        method.setAccessible(true);

        Object result = method.invoke(provider, " ", "REGISTERED");

        assertEquals("REGISTERED", result);
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
