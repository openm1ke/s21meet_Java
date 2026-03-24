package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.api.ProjectApi;
import ru.izpz.dto.model.ParticipantProjectV1DTO;
import ru.izpz.dto.model.ParticipantProjectsV1DTO;
import ru.izpz.dto.model.ProjectV1DTO;
import ru.izpz.edu.config.ProjectsProviderConfig;
import ru.izpz.edu.dto.StudentProjectData;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestApiProjectsProviderTest {

    @Mock
    private ParticipantApi participantApi;
    @Mock
    private ProjectApi projectApi;

    private RestApiProjectsProvider provider;

    @BeforeEach
    void setUp() {
        ProjectsProviderConfig.ProjectsProperties properties = new ProjectsProviderConfig.ProjectsProperties();
        properties.getRest().setPageSize(1000);
        provider = new RestApiProjectsProvider(participantApi, projectApi, properties);
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

        ProjectV1DTO inProgressDetails = new ProjectV1DTO();
        inProgressDetails.setDescription("desc1");
        inProgressDetails.setXp(150);
        inProgressDetails.setDurationHours(12);

        ProjectV1DTO registeredDetails = new ProjectV1DTO();
        registeredDetails.setDescription("desc2");
        registeredDetails.setXp(200);
        registeredDetails.setDurationHours(20);

        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS"))
                .thenReturn(inProgressResponse);
        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "REGISTERED"))
                .thenReturn(registeredResponse);
        when(projectApi.getProjectByProjectId(100L)).thenReturn(inProgressDetails);
        when(projectApi.getProjectByProjectId(101L)).thenReturn(registeredDetails);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(2, result.size());
        assertEquals("100", result.get(0).goalId());
        assertEquals("101", result.get(1).goalId());
        assertEquals("IN_PROGRESS", result.get(0).goalStatus());
        assertEquals("REGISTERED", result.get(1).goalStatus());
        assertEquals("desc1", result.get(0).description());
        assertEquals(Integer.valueOf(150), result.get(0).experience());
        assertEquals(Integer.valueOf(12), result.get(0).laboriousness());
        assertEquals("desc2", result.get(1).description());
        assertEquals(Integer.valueOf(200), result.get(1).experience());
        assertEquals(Integer.valueOf(20), result.get(1).laboriousness());
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

        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS"))
                .thenReturn(inProgressResponse);
        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "REGISTERED"))
                .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        assertEquals("100", result.getFirst().goalId());
    }

    @Test
    void getStudentProjectsByLogin_shouldContinueWhenStatusRequestFails() throws ApiException {
        ParticipantProjectV1DTO registeredProject = new ParticipantProjectV1DTO();
        registeredProject.setId(101L);
        registeredProject.setTitle("CPP2");
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of(registeredProject));

        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS"))
                .thenThrow(new ApiException("boom"));
        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "REGISTERED"))
                .thenReturn(registeredResponse);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        assertEquals("101", result.getFirst().goalId());
        verify(participantApi).getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS");
        verify(participantApi).getParticipantProjectsByLogin("login", 1000L, 0L, "REGISTERED");
    }

    @Test
    void getStudentProjectsByLogin_shouldKeepProjectWhenDetailsRequestFails() throws ApiException {
        ParticipantProjectV1DTO inProgressProject = new ParticipantProjectV1DTO();
        inProgressProject.setId(100L);
        inProgressProject.setTitle("CPP1");

        ParticipantProjectsV1DTO inProgressResponse = new ParticipantProjectsV1DTO();
        inProgressResponse.setProjects(List.of(inProgressProject));
        ParticipantProjectsV1DTO registeredResponse = new ParticipantProjectsV1DTO();
        registeredResponse.setProjects(List.of());

        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS"))
                .thenReturn(inProgressResponse);
        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "REGISTERED"))
                .thenReturn(registeredResponse);
        when(projectApi.getProjectByProjectId(100L)).thenThrow(new ApiException("detail-boom"));

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(1, result.size());
        assertEquals("100", result.getFirst().goalId());
        assertNull(result.getFirst().description());
        assertNull(result.getFirst().experience());
        assertNull(result.getFirst().laboriousness());
        verify(projectApi).getProjectByProjectId(100L);
    }

    @Test
    void getStudentProjectsByLogin_shouldReturnEmpty_whenResponsesAreNull() throws ApiException {
        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS"))
                .thenReturn(null);
        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "REGISTERED"))
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

        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS"))
                .thenReturn(inProgressResponse);
        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "REGISTERED"))
                .thenReturn(registeredResponse);
        when(projectApi.getProjectByProjectId(200L)).thenReturn(null);

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
        provider = new RestApiProjectsProvider(participantApi, projectApi, properties);

        ParticipantProjectsV1DTO response = new ParticipantProjectsV1DTO();
        response.setProjects(List.of());
        when(participantApi.getParticipantProjectsByLogin("login", 1L, 0L, "IN_PROGRESS")).thenReturn(response);
        when(participantApi.getParticipantProjectsByLogin("login", 1L, 0L, "REGISTERED")).thenReturn(response);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertTrue(result.isEmpty());
        verify(participantApi).getParticipantProjectsByLogin("login", 1L, 0L, "IN_PROGRESS");
        verify(participantApi).getParticipantProjectsByLogin("login", 1L, 0L, "REGISTERED");
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

        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS"))
                .thenReturn(inProgressResponse);
        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "REGISTERED"))
                .thenReturn(registeredResponse);
        when(projectApi.getProjectByProjectId(301L)).thenReturn(null);
        when(projectApi.getProjectByProjectId(302L)).thenReturn(null);

        List<StudentProjectData> result = provider.getStudentProjectsByLogin("login");

        assertEquals(2, result.size());
        assertEquals("ACCEPTED", result.get(0).goalStatus());
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), result.get(0).localCourseId());
        assertEquals("FAILED", result.get(1).goalStatus());
        assertEquals(Integer.valueOf(42), result.get(1).localCourseId());
    }

    @Test
    void resolveProjectDetails_shouldReturnCachedValue_whenAlreadyCached() throws Exception {
        Method method = RestApiProjectsProvider.class.getDeclaredMethod("resolveProjectDetails", long.class, Map.class);
        method.setAccessible(true);

        ProjectV1DTO cached = new ProjectV1DTO();
        cached.setDescription("cached");
        Map<Long, ProjectV1DTO> cache = new HashMap<>();
        cache.put(500L, cached);

        Object result = method.invoke(provider, 500L, cache);

        assertSame(cached, result);
        verify(projectApi, never()).getProjectByProjectId(500L);
    }
}
