package ru.izpz.edu.service.provider;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.edu.service.GraphQLService;

@ExtendWith(MockitoExtension.class)
class GraphQlCoalitionProviderTest {

  @Mock private GraphQLService graphQlService;

  @InjectMocks private GraphQLCoalitionProvider provider;

  @Test
  void refreshCoalitionByLogin_shouldDelegateToGraphQlService() {
    provider.refreshCoalitionByLogin("testuser");

    verify(graphQlService).refreshStudentCoalitionByLoginWithLimits("testuser");
  }
}
