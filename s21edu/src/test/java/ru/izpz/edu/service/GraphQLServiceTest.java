package ru.izpz.edu.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.edu.client.GraphQLApiClient;
import ru.izpz.edu.dto.*;
import ru.izpz.edu.model.StudentCoalition;
import ru.izpz.edu.model.StudentCredentials;
import ru.izpz.edu.model.StudentProject;
import ru.izpz.edu.repository.StudentCoalitionRepository;
import ru.izpz.edu.repository.StudentCredentialsRepository;
import ru.izpz.edu.repository.StudentProjectRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphQLServiceTest {

    @Mock
    private GraphQLApiClient client;
    @Mock
    private StudentCredentialsRepository studentCredentialsRepository;
    @Mock
    private StudentCoalitionRepository studentCoalitionRepository;
    @Mock
    private StudentProjectRepository studentProjectRepository;
    @Mock
    private StudentProjectRefreshService studentProjectRefreshService;

    private GraphQLService graphQLService;

    private GraphQLClusterDto testClusterDto;
    private GraphQLStudentCredentialsDataDto testCredentialsDto;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        graphQLService = new GraphQLService(
                client,
                studentCredentialsRepository,
                studentCoalitionRepository,
                studentProjectRepository,
                studentProjectRefreshService,
                meterRegistry
        );

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
        
        GraphQLService.ClusterSeat seat = result.getFirst();
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
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.empty());
        when(client.execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), 
                anyString(), eq(GraphQLStudentCredentialsDataDto.class)))
                .thenReturn(testCredentialsDto);

        // When
        String result = graphQLService.getUserIdByLogin(login);

        // Then
        assertEquals("user123", result);
        verify(studentCredentialsRepository).save(any(StudentCredentials.class));
        verify(client).execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), 
                anyString(), eq(GraphQLStudentCredentialsDataDto.class));
    }

    @Test
    void getUserIdByLogin_shouldReturnNull_whenNotFound() {
        // Given
        String login = "nonexistent";
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.empty());
        GraphQLStudentCredentialsDataDto emptyDto = new GraphQLStudentCredentialsDataDto(null);
        when(client.execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), 
                anyString(), eq(GraphQLStudentCredentialsDataDto.class)))
                .thenReturn(emptyDto);

        // When
        String result = graphQLService.getUserIdByLogin(login);

        // Then
        assertNull(result);
        verify(studentCredentialsRepository, never()).save(any(StudentCredentials.class));
        verify(client).execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), 
                anyString(), eq(GraphQLStudentCredentialsDataDto.class));
    }

    @Test
    void getStudentProjectsByLogin_shouldReturnEmptyList_whenUserNotFound() {
        // Given
        String login = "nonexistent";
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.empty());
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
        assertEquals(0, result.getFirst().number());
        assertNull(result.getFirst().login());
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
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.empty());
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
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.empty());
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

    @Test
    void getStudentCredentialsByLogin_shouldReturnFromDb_whenCached() {
        String login = "cached";
        StudentCredentials cached = new StudentCredentials();
        cached.setLogin(login);
        cached.setStudentId("student-cached");
        cached.setUserId("user-cached");
        cached.setSchoolId("school-cached");
        cached.setIsActive(true);
        cached.setIsGraduate(false);
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.of(cached));

        GraphQLStudentCredentialsDto result = graphQLService.getStudentCredentialsByLogin(login);

        assertNotNull(result);
        assertEquals("user-cached", result.userId());
        verify(client, never()).execute(eq("getCredentialsByLogin"), anyMap(), anyString(), eq(GraphQLStudentCredentialsDataDto.class));
        verify(studentCredentialsRepository, never()).save(any(StudentCredentials.class));
    }

    @Test
    void refreshStudentCoalitionByLogin_shouldFetchAndStoreFreshData() {
        String login = "elevante";
        String userId = "02e9891a-fb02-4783-9b67-ab5265d0c684";

        StudentCredentials cached = new StudentCredentials();
        cached.setLogin(login);
        cached.setUserId(userId);
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.of(cached));

        GraphQLStudentCoalitionDataDto coalitionData = new GraphQLStudentCoalitionDataDto(
                new GraphQLStudentTournamentDataDto(
                        new GraphQLUserTournamentWidgetDto(
                                new GraphQLCoalitionMemberDto(
                                        new GraphQLCoalitionDataDto("Capybaras", 1085),
                                        new GraphQLCurrentTournamentPowerRankDto(271)
                                )
                        )
                )
        );

        when(client.execute(eq("publicProfileGetCoalition"), eq(Map.of("userId", userId)),
                anyString(), eq(GraphQLStudentCoalitionDataDto.class)))
                .thenReturn(coalitionData);
        when(studentCoalitionRepository.findById(login)).thenReturn(Optional.empty());

        graphQLService.refreshStudentCoalitionByLogin(login);

        verify(client).execute(eq("publicProfileGetCoalition"), eq(Map.of("userId", userId)),
                anyString(), eq(GraphQLStudentCoalitionDataDto.class));
        verify(studentCoalitionRepository).save(argThat(entity ->
                login.equals(entity.getLogin())
                        && userId.equals(entity.getUserId())
                        && "Capybaras".equals(entity.getCoalitionName())
                        && Integer.valueOf(1085).equals(entity.getMemberCount())
                        && Integer.valueOf(271).equals(entity.getRank())
        ));
        assertEquals(1.0, meterRegistry.find("edu_graphql_coalition_refresh_total")
                .tag("outcome", "success")
                .counter()
                .count());
        assertNotNull(meterRegistry.find("edu_graphql_coalition_refresh_duration_seconds")
                .tag("outcome", "success")
                .timer());
    }

    @Test
    void refreshStudentCoalitionByLogin_shouldSkip_whenUserIdMissing() {
        String login = "unknown";
        when(studentCoalitionRepository.findById(login)).thenReturn(Optional.empty());
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.empty());
        when(client.execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)),
                anyString(), eq(GraphQLStudentCredentialsDataDto.class)))
                .thenReturn(new GraphQLStudentCredentialsDataDto(null));

        graphQLService.refreshStudentCoalitionByLogin(login);

        verify(client, never()).execute(eq("publicProfileGetCoalition"), anyMap(), anyString(), eq(GraphQLStudentCoalitionDataDto.class));
        verify(studentCoalitionRepository, never()).save(any(StudentCoalition.class));
    }

    @Test
    void refreshStudentCoalitionByLogin_shouldSkip_whenFreshDataExists() {
        String login = "fresh";
        StudentCoalition fresh = new StudentCoalition();
        fresh.setLogin(login);
        fresh.setUpdatedAt(OffsetDateTime.now());
        when(studentCoalitionRepository.findById(login)).thenReturn(Optional.of(fresh));

        graphQLService.refreshStudentCoalitionByLogin(login);

        verifyNoInteractions(client);
        verify(studentCoalitionRepository, never()).save(any(StudentCoalition.class));
        assertEquals(1.0, meterRegistry.find("edu_graphql_coalition_refresh_total")
                .tag("outcome", "skipped_ttl")
                .counter()
                .count());
        assertNotNull(meterRegistry.find("edu_graphql_coalition_refresh_duration_seconds")
                .tag("outcome", "skipped_ttl")
                .timer());
    }

    @Test
    void refreshStudentCoalitionByLogin_shouldRecordErrorMetrics_whenGraphQlFails() {
        String login = "broken";
        String userId = "u-1";
        when(studentCoalitionRepository.findById(login)).thenReturn(Optional.empty(), Optional.empty());
        StudentCredentials cached = new StudentCredentials();
        cached.setLogin(login);
        cached.setUserId(userId);
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.of(cached));
        when(client.execute(eq("publicProfileGetCoalition"), eq(Map.of("userId", userId)),
                anyString(), eq(GraphQLStudentCoalitionDataDto.class)))
                .thenThrow(new RuntimeException("boom"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> graphQLService.refreshStudentCoalitionByLogin(login));

        assertEquals("boom", ex.getMessage());
        assertEquals(1.0, meterRegistry.find("edu_graphql_coalition_refresh_total")
                .tag("outcome", "error")
                .counter()
                .count());
        assertNotNull(meterRegistry.find("edu_graphql_coalition_refresh_duration_seconds")
                .tag("outcome", "error")
                .timer());
    }

    @Test
    void refreshStudentProjectsByLogin_shouldFetchAndStoreFreshProjects() {
        String login = "quarkron";
        String userId = "u-42";
        when(studentProjectRepository.findMaxUpdatedAtByLogin(login)).thenReturn(null);

        StudentCredentials cached = new StudentCredentials();
        cached.setLogin(login);
        cached.setUserId(userId);
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.of(cached));

        GraphQLStudentProject p1 = new GraphQLStudentProject(
                "g1", "Project1", "d1", 100, "2026-01-01", 10, 1,
                "INDIVIDUAL", "IN_PROGRESS", "CORE", "IN_PROGRESS",
                1, 2, 3, 4, 5, 6, "grp1", 7
        );
        GraphQLStudentProject p2 = new GraphQLStudentProject(
                "g2", "Project2", "d2", 200, "2026-01-02", 20, 2,
                "GROUP", "WAITING_FOR_START", "CORE", "WAITING_FOR_START",
                2, 3, 4, 5, 6, 7, "grp2", 8
        );
        GraphQLStudentProjectsDataDto projectsData =
                new GraphQLStudentProjectsDataDto(new GraphQLStudentQueriesDto(List.of(p1, p2)));
        when(client.execute(eq("getStudentCurrentProjects"), eq(Map.of("userId", userId)), anyString(), eq(GraphQLStudentProjectsDataDto.class)))
                .thenReturn(projectsData);

        graphQLService.refreshStudentProjectsByLogin(login);

        verify(studentProjectRefreshService).replaceProjects(eq(login), eq(userId), argThat(list ->
                list.size() == 2
                        && "g1".equals(list.getFirst().goalId())
                        && "g2".equals(list.get(1).goalId())
        ));
        assertEquals(1.0, meterRegistry.find("edu_graphql_projects_refresh_total")
                .tag("outcome", "success")
                .counter()
                .count());
    }

    @Test
    void refreshStudentProjectsByLogin_shouldSaveSnapshot_whenProjectsAreEmpty() {
        String login = "no-projects";
        String userId = "u-empty";
        when(studentProjectRepository.findMaxUpdatedAtByLogin(login)).thenReturn(null);

        StudentCredentials cached = new StudentCredentials();
        cached.setLogin(login);
        cached.setUserId(userId);
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.of(cached));
        when(client.execute(eq("getStudentCurrentProjects"), eq(Map.of("userId", userId)), anyString(), eq(GraphQLStudentProjectsDataDto.class)))
                .thenReturn(new GraphQLStudentProjectsDataDto(new GraphQLStudentQueriesDto(List.of())));

        graphQLService.refreshStudentProjectsByLogin(login);

        verify(studentProjectRefreshService).replaceProjects(eq(login), eq(userId), argThat(List::isEmpty));
    }

    @Test
    void refreshStudentProjectsByLogin_shouldSkip_whenFreshDataExists() {
        String login = "fresh-projects";
        when(studentProjectRepository.findMaxUpdatedAtByLogin(login)).thenReturn(OffsetDateTime.now());

        graphQLService.refreshStudentProjectsByLogin(login);

        verifyNoInteractions(client);
        verifyNoInteractions(studentProjectRefreshService);
        assertEquals(1.0, meterRegistry.find("edu_graphql_projects_refresh_total")
                .tag("outcome", "skipped_ttl")
                .counter()
                .count());
    }

    @Test
    void refreshStudentProjectsByLogin_shouldSkip_whenUserIdMissing() {
        String login = "unknown-projects";
        when(studentProjectRepository.findMaxUpdatedAtByLogin(login)).thenReturn(null);
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.empty());
        when(client.execute(eq("getCredentialsByLogin"), eq(Map.of("login", login)), anyString(), eq(GraphQLStudentCredentialsDataDto.class)))
                .thenReturn(new GraphQLStudentCredentialsDataDto(null));

        graphQLService.refreshStudentProjectsByLogin(login);

        verify(client, never()).execute(eq("getStudentCurrentProjects"), anyMap(), anyString(), eq(GraphQLStudentProjectsDataDto.class));
        verifyNoInteractions(studentProjectRefreshService);
        assertEquals(1.0, meterRegistry.find("edu_graphql_projects_refresh_total")
                .tag("outcome", "skipped_no_user_id")
                .counter()
                .count());
    }

    @Test
    void refreshStudentProjectsByLogin_shouldRecordErrorMetrics_whenGraphQlFails() {
        String login = "broken-projects";
        String userId = "u-broken";
        when(studentProjectRepository.findMaxUpdatedAtByLogin(login)).thenReturn(null);
        StudentCredentials cached = new StudentCredentials();
        cached.setLogin(login);
        cached.setUserId(userId);
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.of(cached));
        when(client.execute(eq("getStudentCurrentProjects"), eq(Map.of("userId", userId)), anyString(), eq(GraphQLStudentProjectsDataDto.class)))
                .thenThrow(new RuntimeException("projects-boom"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> graphQLService.refreshStudentProjectsByLogin(login));

        assertEquals("projects-boom", ex.getMessage());
        verifyNoInteractions(studentProjectRefreshService);
        assertEquals(1.0, meterRegistry.find("edu_graphql_projects_refresh_total")
                .tag("outcome", "error")
                .counter()
                .count());
    }

    @Test
    void getCachedStudentProjectsByLogin_shouldReturnStoredProjects_whenRefreshFails() {
        String login = "fallback";
        String userId = "u-fallback";
        when(studentProjectRepository.findMaxUpdatedAtByLogin(login)).thenReturn(null);
        StudentCredentials cached = new StudentCredentials();
        cached.setLogin(login);
        cached.setUserId(userId);
        when(studentCredentialsRepository.findById(login)).thenReturn(Optional.of(cached));
        when(client.execute(eq("getStudentCurrentProjects"), eq(Map.of("userId", userId)), anyString(), eq(GraphQLStudentProjectsDataDto.class)))
                .thenThrow(new RuntimeException("boom"));

        StudentProject stored = new StudentProject();
        stored.setLogin(login);
        stored.setGoalId("g-stored");
        stored.setName("Stored Project");
        stored.setExperience(321);
        stored.setExecutionType("INDIVIDUAL");
        stored.setGoalStatus("IN_PROGRESS");
        stored.setSortOrder(0);
        stored.setSnapshot(false);
        when(studentProjectRepository.findAllByLoginAndSnapshotFalseOrderBySortOrderAsc(login)).thenReturn(List.of(stored));

        List<GraphQLStudentProject> result = graphQLService.getCachedStudentProjectsByLogin(login);

        assertEquals(1, result.size());
        assertEquals("g-stored", result.getFirst().goalId());
        assertEquals("Stored Project", result.getFirst().name());
        assertEquals(Integer.valueOf(321), result.getFirst().experience());
    }
}
