package ru.izpz.edu.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.model.WorkplaceId;
import ru.izpz.edu.repository.WorkplaceRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkplaceServiceTest {

    @Mock
    private WorkplaceRepository workplaceRepository;

    @InjectMocks
    private WorkplaceService workplaceService;

    private Workplace testWorkplace;

    @BeforeEach
    void setUp() {
        testWorkplace = new Workplace();
        testWorkplace.setId(new WorkplaceId(1L, "A", 101));
        testWorkplace.setLogin("testuser");
        testWorkplace.setExpValue(1000);
        testWorkplace.setLevelCode(5);
        testWorkplace.setStageGroupName("Group1");
        testWorkplace.setStageName("Stage1");
    }

    @Test
    void getWorkplace_shouldReturnWorkplace_whenFound() {
        // Given
        when(workplaceRepository.findByLogin("testuser"))
                .thenReturn(Optional.of(testWorkplace));

        // When
        Optional<Workplace> result = workplaceService.getWorkplace("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testWorkplace, result.get());
        verify(workplaceRepository).findByLogin("testuser");
    }

    @Test
    void getWorkplace_shouldReturnEmpty_whenNotFound() {
        // Given
        when(workplaceRepository.findByLogin("nonexistent"))
                .thenReturn(Optional.empty());

        // When
        Optional<Workplace> result = workplaceService.getWorkplace("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(workplaceRepository).findByLogin("nonexistent");
    }

    @Test
    void getWorkplaces_shouldReturnAllWorkplaces() {
        // Given
        List<Workplace> expectedWorkplaces = List.of(testWorkplace);
        when(workplaceRepository.findAll()).thenReturn(expectedWorkplaces);

        // When
        List<Workplace> result = workplaceService.getWorkplaces();

        // Then
        assertNotNull(result);
        assertEquals(expectedWorkplaces, result);
        verify(workplaceRepository).findAll();
    }

    @Test
    void getWorkplaces_shouldReturnEmptyList_whenNoWorkplaces() {
        // Given
        when(workplaceRepository.findAll()).thenReturn(List.of());

        // When
        List<Workplace> result = workplaceService.getWorkplaces();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(workplaceRepository).findAll();
    }

    @Test
    void findAllByLoginIn_shouldReturnWorkplaces() {
        // Given
        List<String> logins = List.of("testuser", "user2", "user3");
        List<Workplace> expectedWorkplaces = List.of(testWorkplace);
        when(workplaceRepository.findAllByLoginIn(logins))
                .thenReturn(expectedWorkplaces);

        // When
        List<Workplace> result = workplaceService.findAllByLoginIn(logins);

        // Then
        assertNotNull(result);
        assertEquals(expectedWorkplaces, result);
        verify(workplaceRepository).findAllByLoginIn(logins);
    }

    @Test
    void findAllByLoginIn_shouldReturnEmptyList_whenNoMatches() {
        // Given
        List<String> logins = List.of("nonexistent1", "nonexistent2");
        when(workplaceRepository.findAllByLoginIn(logins))
                .thenReturn(List.of());

        // When
        List<Workplace> result = workplaceService.findAllByLoginIn(logins);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(workplaceRepository).findAllByLoginIn(logins);
    }

    @Test
    void findAllByLoginIn_shouldHandleEmptyLoginList() {
        // Given
        List<String> emptyLogins = List.of();
        when(workplaceRepository.findAllByLoginIn(emptyLogins))
                .thenReturn(List.of());

        // When
        List<Workplace> result = workplaceService.findAllByLoginIn(emptyLogins);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(workplaceRepository).findAllByLoginIn(emptyLogins);
    }
}
