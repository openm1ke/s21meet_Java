package ru.izpz.edu.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.ApiException;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.mapper.CampusMapper;
import ru.izpz.edu.mapper.ProjectsMapper;
import ru.izpz.edu.service.provider.WorkplaceProvider;

import java.util.UUID;

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
}
