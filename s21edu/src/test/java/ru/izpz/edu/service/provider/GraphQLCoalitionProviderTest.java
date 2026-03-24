package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.edu.service.GraphQLService;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GraphQLCoalitionProviderTest {

    @Mock
    private GraphQLService graphQLService;

    @InjectMocks
    private GraphQLCoalitionProvider provider;

    @Test
    void refreshCoalitionByLogin_shouldDelegateToGraphQlService() {
        provider.refreshCoalitionByLogin("testuser");

        verify(graphQLService).refreshStudentCoalitionByLogin("testuser");
    }
}
