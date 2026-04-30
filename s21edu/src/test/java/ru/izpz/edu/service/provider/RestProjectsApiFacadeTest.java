package ru.izpz.edu.service.provider;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import ru.izpz.dto.model.ParticipantProjectsV1DTO;
import ru.izpz.edu.client.PlatformApiFacade;

class RestProjectsApiFacadeTest {

  @Test
  void getParticipantProjectsByLogin_delegatesToPlatformApiFacade() {
    PlatformApiFacade platformApi = mock(PlatformApiFacade.class);
    RestProjectsApiFacade facade = new RestProjectsApiFacade(platformApi);
    ParticipantProjectsV1DTO expected = new ParticipantProjectsV1DTO();

    when(platformApi.getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS"))
        .thenReturn(expected);

    ParticipantProjectsV1DTO actual =
        facade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS");

    assertSame(expected, actual);
    verify(platformApi).getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS");
  }
}
