package ru.izpz.edu.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;
import ru.izpz.edu.repository.ClusterRepository;
import ru.izpz.edu.repository.WorkplaceRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampusPersistenceServiceTest {

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private WorkplaceRepository workplaceRepository;

    @InjectMocks
    private CampusPersistenceService campusPersistenceService;

    private Cluster testCluster;
    private Workplace testWorkplace;
    private String campusId;

    @BeforeEach
    void setUp() {
        campusId = UUID.randomUUID().toString();
        
        testCluster = new Cluster();
        testCluster.setClusterId(1L);
        testCluster.setCampusId(campusId);
        testCluster.setName("Test Cluster");
        testCluster.setFloor(1);
        testCluster.setCapacity(50);
        testCluster.setAvailableCapacity(25);

        testWorkplace = new Workplace();
        testWorkplace.setId(new WorkplaceId(1L, "A", 101));
        testWorkplace.setLogin("testuser");
        testWorkplace.setExpValue(1000);
        testWorkplace.setLevelCode(5);
        testWorkplace.setStageGroupName("Group1");
        testWorkplace.setStageName("Stage1");
    }

    @Test
    void replaceClusters_shouldDeleteAndSaveClusters() {
        // Given
        List<Cluster> clusters = List.of(testCluster);
        when(clusterRepository.findAllByCampusIdOrderByFloorAsc(campusId)).thenReturn(List.of());
        
        // When
        campusPersistenceService.replaceClusters(campusId, clusters);

        // Then
        verify(clusterRepository).deleteAllByCampusId(campusId);
        verify(clusterRepository).saveAll(clusters);
    }

    @Test
    void replaceClusters_shouldDeleteOldParticipants_whenExistingClustersPresent() {
        // Given
        Cluster existing = new Cluster();
        existing.setClusterId(42L);
        when(clusterRepository.findAllByCampusIdOrderByFloorAsc(campusId)).thenReturn(List.of(existing));

        // When
        campusPersistenceService.replaceClusters(campusId, List.of());

        // Then
        verify(workplaceRepository).deleteByIdClusterIdIn(java.util.Set.of(42L));
        verify(clusterRepository).deleteAllByCampusId(campusId);
    }

    @Test
    void replaceClusters_shouldOnlyDelete_whenClustersEmpty() {
        // Given
        List<Cluster> emptyClusters = List.of();
        
        // When
        campusPersistenceService.replaceClusters(campusId, emptyClusters);

        // Then
        verify(clusterRepository).deleteAllByCampusId(campusId);
        verify(clusterRepository, never()).saveAll(any());
    }

    @Test
    void replaceParticipants_shouldDeleteAndSaveWorkplaces() {
        // Given
        long clusterId = 1L;
        List<Workplace> workplaces = List.of(testWorkplace);
        
        // When
        campusPersistenceService.replaceParticipants(clusterId, workplaces);

        // Then
        verify(workplaceRepository).deleteByIdClusterId(clusterId);
        verify(workplaceRepository).saveAll(workplaces);
    }

    @Test
    void replaceParticipants_shouldOnlyDelete_whenWorkplacesEmpty() {
        // Given
        long clusterId = 1L;
        List<Workplace> emptyWorkplaces = List.of();
        
        // When
        campusPersistenceService.replaceParticipants(clusterId, emptyWorkplaces);

        // Then
        verify(workplaceRepository).deleteByIdClusterId(clusterId);
        verify(workplaceRepository, never()).saveAll(any());
    }

    @Test
    void findAllByCampusIdOrderByFloorAsc_shouldReturnClusters() {
        // Given
        List<Cluster> expectedClusters = List.of(testCluster);
        when(clusterRepository.findAllByCampusIdOrderByFloorAsc(campusId))
                .thenReturn(expectedClusters);

        // When
        List<Cluster> result = campusPersistenceService.findAllByCampusIdOrderByFloorAsc(campusId);

        // Then
        assertNotNull(result);
        assertEquals(expectedClusters, result);
        verify(clusterRepository).findAllByCampusIdOrderByFloorAsc(campusId);
    }

    @Test
    void findAllByCampusIdOrderByFloorAsc_shouldReturnEmptyList_whenNoClusters() {
        // Given
        when(clusterRepository.findAllByCampusIdOrderByFloorAsc(campusId))
                .thenReturn(List.of());

        // When
        List<Cluster> result = campusPersistenceService.findAllByCampusIdOrderByFloorAsc(campusId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(clusterRepository).findAllByCampusIdOrderByFloorAsc(campusId);
    }

    @Test
    void findAllByOrderByCampusIdAsc_shouldReturnClusters() {
        // Given
        List<Cluster> expectedClusters = List.of(testCluster);
        when(clusterRepository.findAllByOrderByCampusIdAsc())
                .thenReturn(expectedClusters);

        // When
        List<Cluster> result = campusPersistenceService.findAllByOrderByCampusIdAsc();

        // Then
        assertNotNull(result);
        assertEquals(expectedClusters, result);
        verify(clusterRepository).findAllByOrderByCampusIdAsc();
    }

    @Test
    void findAllByOrderByCampusIdAsc_shouldReturnEmptyList_whenNoClusters() {
        // Given
        when(clusterRepository.findAllByOrderByCampusIdAsc())
                .thenReturn(List.of());

        // When
        List<Cluster> result = campusPersistenceService.findAllByOrderByCampusIdAsc();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(clusterRepository).findAllByOrderByCampusIdAsc();
    }
}
