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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
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
}
