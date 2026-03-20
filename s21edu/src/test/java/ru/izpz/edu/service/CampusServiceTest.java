package ru.izpz.edu.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.*;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.mapper.ProjectsMapper;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.repository.WorkplaceRepository;
import ru.izpz.edu.service.provider.WorkplaceProvider;
import ru.izpz.edu.dto.GraphQLStudentProject;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampusServiceTest {

    @InjectMocks
    private CampusService campusService;

    @Mock
    private CampusPersistenceService persistenceService;

    @Mock
    private CampusMapper campusMapper;

    @Mock
    private CampusClient campusClient;

    @Mock
    private ProjectsMapper projectsMapper;

    @Mock
    private WorkplaceProvider workplaceProvider;

    @Mock
    private SchedulerMetricsService schedulerMetricsService;
    
    @Mock
    private WorkplaceRepository workplaceRepository;

    private static final UUID CAMPUS_ID = UUID.fromString("6bfe3c56-0211-4fe1-9e59-51616caac4dd");
    private static final Long CLUSTER_ID = 123L;

    @Test
    void replaceParticipantsByClusterIdWithProvider_shouldUseConfiguredProvider() throws ApiException {
        // Arrange
        doNothing().when(workplaceProvider).updateParticipantsByCluster(CLUSTER_ID);

        // Act
        campusService.replaceParticipantsByClusterIdWithProvider(CLUSTER_ID);

        // Assert
        verify(workplaceProvider).updateParticipantsByCluster(CLUSTER_ID);
    }

    @Test
    void replaceClustersByCampusId_shouldCallPersistenceService() {
        // Arrange
        List<ClusterV1DTO> clustersDto = List.of(new ClusterV1DTO());
        Cluster expectedCluster = new Cluster();
        expectedCluster.setName("cluster-1");
        expectedCluster.setCapacity(20);
        expectedCluster.setAvailableCapacity(7);
        when(campusMapper.toClusterEntity(any(ClusterV1DTO.class), anyString())).thenReturn(expectedCluster);

        // Act
        campusService.replaceClustersByCampusId(CAMPUS_ID.toString(), clustersDto);

        // Assert
        verify(persistenceService).replaceClusters(eq(CAMPUS_ID.toString()), argThat(list -> list.size() == 1));
        verify(campusMapper).toClusterEntity(any(ClusterV1DTO.class), eq(CAMPUS_ID.toString()));
        verify(schedulerMetricsService).recordClusterPlaces(CAMPUS_ID.toString(), "cluster-1", 7, 13);
    }

    @Test
    void replaceClustersByCampusId_shouldNormalizeNullAndNegativeCapacities() {
        List<ClusterV1DTO> clustersDto = List.of(new ClusterV1DTO());
        Cluster expectedCluster = new Cluster();
        expectedCluster.setName("cluster-2");
        expectedCluster.setCapacity(null);
        expectedCluster.setAvailableCapacity(-1);
        when(campusMapper.toClusterEntity(any(ClusterV1DTO.class), anyString())).thenReturn(expectedCluster);

        campusService.replaceClustersByCampusId(CAMPUS_ID.toString(), clustersDto);

        verify(schedulerMetricsService).recordClusterPlaces(CAMPUS_ID.toString(), "cluster-2", 0, 0);
    }

    @Test
    void replaceClustersByCampusId_shouldHandleEmptyList() {
        // Arrange
        List<ClusterV1DTO> emptyClustersDto = List.of();

        // Act
        campusService.replaceClustersByCampusId(CAMPUS_ID.toString(), emptyClustersDto);

        // Assert
        verify(persistenceService, never()).replaceClusters(anyString(), anyList());
        verify(campusMapper, never()).toClusterEntity(any(ClusterV1DTO.class), anyString());
        verifyNoInteractions(schedulerMetricsService);
    }

    @Test
    void getClusters_shouldMapToClustersDto() {
        // Arrange
        Cluster c = new Cluster();
        c.setName("c");
        c.setCapacity(10);
        c.setAvailableCapacity(5);
        c.setFloor(2);
        when(persistenceService.findAllByCampusIdOrderByFloorAsc(CAMPUS_ID.toString()))
                .thenReturn(List.of(c));

        CampusDto campus = new CampusDto("name", CAMPUS_ID.toString());

        // Act
        List<Clusters> result = campusService.getClusters(campus);

        // Assert
        assertEquals(1, result.size());
        Clusters dto = result.getFirst();
        assertEquals("c", dto.getName());
        assertEquals(10, dto.getCapacity());
        assertEquals(5, dto.getAvailableCapacity());
        assertEquals(2, dto.getFloor());
    }

    @Test
    void findAllByOrderByCampusIdAsc_shouldDelegate() {
        // Arrange
        Cluster c = new Cluster();
        when(persistenceService.findAllByOrderByCampusIdAsc()).thenReturn(List.of(c));

        // Act
        List<Cluster> result = campusService.findAllByOrderByCampusIdAsc();

        // Assert
        assertEquals(1, result.size());
        org.junit.jupiter.api.Assertions.assertSame(c, result.getFirst());
    }

    @Test
    void getStudentProjectsByLogin_shouldMapViaProjectsMapper() {
        // Arrange
        GraphQLStudentProject src = new GraphQLStudentProject("g", "n", "d", 1, "dt", 1, 1, "e", "gs", "ct", "ds", 1, 1, 1, 1, 1, 1, "grp", 1);
        ProjectsDto dto = new ProjectsDto("g","n","d",1,"dt",1,1,"e","gs","ct","ds",1,1,1,1,1,1,"grp",1);
        when(campusClient.getStudentProjectsByLogin("login")).thenReturn(List.of(src));
        when(projectsMapper.toDto(src)).thenReturn(dto);

        // Act
        List<ProjectsDto> result = campusService.getStudentProjectsByLogin("login");

        // Assert
        assertEquals(1, result.size());
        org.junit.jupiter.api.Assertions.assertSame(dto, result.getFirst());
    }

    @Test
    void getProgramStatsByCampusId_shouldNormalizeAndSort() {
        WorkplaceRepository.StageNameCountView ap4 = mock(WorkplaceRepository.StageNameCountView.class);
        when(ap4.getStageName()).thenReturn("AP4");
        when(ap4.getCount()).thenReturn(39L);

        WorkplaceRepository.StageNameCountView intensive = mock(WorkplaceRepository.StageNameCountView.class);
        when(intensive.getStageName()).thenReturn("👽 Intensive Parallel 76");
        when(intensive.getCount()).thenReturn(13L);

        WorkplaceRepository.StageNameCountView noData = mock(WorkplaceRepository.StageNameCountView.class);
        when(noData.getStageName()).thenReturn(" ");
        when(noData.getCount()).thenReturn(1L);

        when(workplaceRepository.countParticipantsByCampusIdAndStageName("campus-1"))
                .thenReturn(List.of(noData, ap4, intensive));

        Map<String, Long> result = campusService.getProgramStatsByCampusId("campus-1");

        assertEquals(3, result.size());
        assertEquals(39L, result.get("AP4"));
        assertEquals(13L, result.get("👽 Intensive Parallel 76"));
        assertEquals(1L, result.get("No data"));
    }

    @Test
    void getProgramStatsByCampusId_shouldNormalizeNullStageName() {
        WorkplaceRepository.StageNameCountView nullName = mock(WorkplaceRepository.StageNameCountView.class);
        when(nullName.getStageName()).thenReturn(null);
        when(nullName.getCount()).thenReturn(2L);

        when(workplaceRepository.countParticipantsByCampusIdAndStageName("campus-2"))
                .thenReturn(List.of(nullName));

        Map<String, Long> result = campusService.getProgramStatsByCampusId("campus-2");

        assertEquals(1, result.size());
        assertEquals(2L, result.get("No data"));
    }

    @Test
    void refreshParticipantMetrics_shouldDelegateAllRepositoryCounters() {
        WorkplaceRepository.CampusCountView campusRow = mock(WorkplaceRepository.CampusCountView.class);
        when(campusRow.getCampusId()).thenReturn("campus-1");
        when(campusRow.getCount()).thenReturn(7L);

        WorkplaceRepository.CampusStageGroupCountView groupRow = mock(WorkplaceRepository.CampusStageGroupCountView.class);
        when(groupRow.getCampusId()).thenReturn("campus-1");
        when(groupRow.getStageGroupName()).thenReturn("Data Science");
        when(groupRow.getCount()).thenReturn(5L);

        WorkplaceRepository.CampusStageNameCountView stageRow = mock(WorkplaceRepository.CampusStageNameCountView.class);
        when(stageRow.getCampusId()).thenReturn("campus-1");
        when(stageRow.getStageName()).thenReturn("AP4");
        when(stageRow.getCount()).thenReturn(2L);

        when(workplaceRepository.countParticipantsByCampus()).thenReturn(List.of(campusRow));
        when(workplaceRepository.countParticipantsByCampusAndStageGroup()).thenReturn(List.of(groupRow));
        when(workplaceRepository.countParticipantsByCampusAndStageName()).thenReturn(List.of(stageRow));

        campusService.refreshParticipantMetrics();

        verify(schedulerMetricsService).resetParticipantMetrics();
        verify(schedulerMetricsService).recordParticipantsByCampus("campus-1", 7L);
        verify(schedulerMetricsService).recordParticipantsByCampusAndStageGroup("campus-1", "Data Science", 5L);
        verify(schedulerMetricsService).recordParticipantsByCampusAndStageName("campus-1", "AP4", 2L);
    }
}
