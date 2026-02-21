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
}
