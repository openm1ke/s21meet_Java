package ru.izpz.edu.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.edu.client.GraphQLApiClient;
import ru.izpz.edu.dto.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphQLServiceTest {

    @Mock
    private GraphQLApiClient client;

    @InjectMocks
    private GraphQLService graphQLService;

    private GraphQLClusterDto testClusterDto;
    private GraphQLStudentCredentialsDataDto testCredentialsDto;

    @BeforeEach
    void setUp() {
        // Setup test data for cluster
        GraphQLPlaceDto testPlace = new GraphQLPlaceDto(
                "A",
                101,
                "Group1",
                "Stage1",
                null,
                new GraphQLUserDto("testuser", "testuser"),
                new GraphQLExpDto(1000, new GraphQLLevelDto(new GraphQLRangeDto(5)))
        );
        
        GraphQLClusterDataDto clusterData = new GraphQLClusterDataDto(List.of(testPlace));
        GraphQLStudentDto student = new GraphQLStudentDto(clusterData);
        testClusterDto = new GraphQLClusterDto(student);

        // Setup test data for credentials
        GraphQLStudentCredentialsDto credentials = new GraphQLStudentCredentialsDto(
                "student123", "user123", "school123", true, false
        );
        GraphQLSchool21Dto school21 = new GraphQLSchool21Dto(credentials);
        testCredentialsDto = new GraphQLStudentCredentialsDataDto(school21);
    }

    @Test
    void getOccupiedSeats_shouldReturnSeats_whenDataExists() {
        // Given
        String clusterId = "1";
        when(client.execute(eq("getCampusPlanOccupied"), eq(Map.of("clusterId", clusterId)), 
                anyString(), eq(GraphQLClusterDto.class)))
                .thenReturn(testClusterDto);

        // When
        List<GraphQLService.ClusterSeat> result = graphQLService.getOccupiedSeats(clusterId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        GraphQLService.ClusterSeat seat = result.get(0);
        assertEquals(clusterId, seat.clusterId());
        assertEquals("A", seat.row());
        assertEquals(101, seat.number());
        assertEquals("testuser", seat.login());
        assertEquals(1000, seat.expValue());
        assertEquals(Integer.valueOf(5), seat.levelCode());
        assertEquals("Group1", seat.stageGroupName());
        assertEquals("Stage1", seat.stageName());
        
        verify(client).execute(eq("getCampusPlanOccupied"), eq(Map.of("clusterId", clusterId)), 
                anyString(), eq(GraphQLClusterDto.class));
    }

    @Test
    void getOccupiedSeats_shouldReturnEmptyList_whenNoData() {
        // Given
        String clusterId = "999";
        GraphQLClusterDto emptyDto = new GraphQLClusterDto(null);
        when(client.execute(eq("getCampusPlanOccupied"), eq(Map.of("clusterId", clusterId)), 
                anyString(), eq(GraphQLClusterDto.class)))
                .thenReturn(emptyDto);

        // When
        List<GraphQLService.ClusterSeat> result = graphQLService.getOccupiedSeats(clusterId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(client).execute(eq("getCampusPlanOccupied"), eq(Map.of("clusterId", clusterId)), 
                anyString(), eq(GraphQLClusterDto.class));
    }

    @Test
    void getUserIdByLogin_shouldReturnUserId_whenFound() {
        // Given
        String login = "testuser";
        when(client.execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), 
                anyString(), eq(GraphQLStudentCredentialsDataDto.class)))
                .thenReturn(testCredentialsDto);

        // When
        String result = graphQLService.getUserIdByLogin(login);

        // Then
        assertEquals("user123", result);
        verify(client).execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), 
                anyString(), eq(GraphQLStudentCredentialsDataDto.class));
    }

    @Test
    void getUserIdByLogin_shouldReturnNull_whenNotFound() {
        // Given
        String login = "nonexistent";
        GraphQLStudentCredentialsDataDto emptyDto = new GraphQLStudentCredentialsDataDto(null);
        when(client.execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), 
                anyString(), eq(GraphQLStudentCredentialsDataDto.class)))
                .thenReturn(emptyDto);

        // When
        String result = graphQLService.getUserIdByLogin(login);

        // Then
        assertNull(result);
        verify(client).execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), 
                anyString(), eq(GraphQLStudentCredentialsDataDto.class));
    }

    @Test
    void getStudentProjectsByLogin_shouldReturnEmptyList_whenUserNotFound() {
        // Given
        String login = "nonexistent";
        GraphQLStudentCredentialsDataDto emptyDto = new GraphQLStudentCredentialsDataDto(null);
        when(client.execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), 
                anyString(), eq(GraphQLStudentCredentialsDataDto.class)))
                .thenReturn(emptyDto);

        // When
        List<GraphQLStudentProject> result = graphQLService.getStudentProjectsByLogin(login);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(client).execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), 
                anyString(), eq(GraphQLStudentCredentialsDataDto.class));
        verify(client, never()).execute(eq("getStudentCurrentProjects"), any(), any(), any());
    }

    @Test
    void getOccupiedSeats_shouldHandleNullNestedFields() {
        String clusterId = "2";
        GraphQLPlaceDto p1 = new GraphQLPlaceDto("B", null, "G1", "S1", null, null, null);
        GraphQLPlaceDto p2 = new GraphQLPlaceDto("C", 7, "G2", "S2", null, new GraphQLUserDto("id2", "u2"), new GraphQLExpDto(200, null));
        GraphQLPlaceDto p3 = new GraphQLPlaceDto("D", 8, "G3", "S3", null, new GraphQLUserDto("id3", "u3"), new GraphQLExpDto(300, new GraphQLLevelDto(null)));

        GraphQLClusterDto dto = new GraphQLClusterDto(new GraphQLStudentDto(new GraphQLClusterDataDto(List.of(p1, p2, p3))));
        when(client.execute(eq("getCampusPlanOccupied"), eq(Map.of("clusterId", clusterId)), anyString(), eq(GraphQLClusterDto.class)))
                .thenReturn(dto);

        List<GraphQLService.ClusterSeat> result = graphQLService.getOccupiedSeats(clusterId);

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).number());
        assertNull(result.get(0).login());
        assertNull(result.get(0).expValue());
        assertNull(result.get(0).levelCode());

        assertEquals("u2", result.get(1).login());
        assertEquals(Integer.valueOf(200), result.get(1).expValue());
        assertNull(result.get(1).levelCode());

        assertEquals("u3", result.get(2).login());
        assertEquals(Integer.valueOf(300), result.get(2).expValue());
        assertNull(result.get(2).levelCode());
    }

    @Test
    void getStudentProjectsByLogin_shouldReturnMappedProjects_whenUserExists() {
        String login = "exists";
        String userId = "user-42";
        GraphQLStudentCredentialsDataDto credentials = new GraphQLStudentCredentialsDataDto(
                new GraphQLSchool21Dto(new GraphQLStudentCredentialsDto("s", userId, "school", true, false))
        );
        GraphQLStudentProject source = new GraphQLStudentProject(
                "goal", "Project", "Desc", 10, "2026-01-01",
                99, 5, "ONLINE", "IN_PROGRESS", "CORE", "IN_PROGRESS",
                1, 2, 3, 4, 5, 6, "grp", 7
        );
        GraphQLStudentProjectsDataDto projects = new GraphQLStudentProjectsDataDto(new GraphQLStudentQueriesDto(List.of(source)));

        when(client.execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), anyString(), eq(GraphQLStudentCredentialsDataDto.class)))
                .thenReturn(credentials);
        when(client.execute(eq("getStudentCurrentProjects"), eq(Map.of("userId", userId)), anyString(), eq(GraphQLStudentProjectsDataDto.class)))
                .thenReturn(projects);

        List<GraphQLStudentProject> result = graphQLService.getStudentProjectsByLogin(login);

        assertEquals(1, result.size());
        GraphQLStudentProject mapped = result.getFirst();
        assertEquals("goal", mapped.goalId());
        assertEquals("Project", mapped.name());
        assertEquals("grp", mapped.groupName());
        assertEquals(7, mapped.localCourseId());
    }

    @Test
    void getStudentProjectsByLogin_shouldReturnEmptyList_whenProjectsDataMissing() {
        String login = "exists";
        String userId = "user-42";
        GraphQLStudentCredentialsDataDto credentials = new GraphQLStudentCredentialsDataDto(
                new GraphQLSchool21Dto(new GraphQLStudentCredentialsDto("s", userId, "school", true, false))
        );

        when(client.execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), anyString(), eq(GraphQLStudentCredentialsDataDto.class)))
                .thenReturn(credentials);
        when(client.execute(eq("getStudentCurrentProjects"), eq(Map.of("userId", userId)), anyString(), eq(GraphQLStudentProjectsDataDto.class)))
                .thenReturn(null);

        List<GraphQLStudentProject> result = graphQLService.getStudentProjectsByLogin(login);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
