package ru.izpz.edu.service.provider;

import org.junit.jupiter.api.Test;
import ru.izpz.dto.ApiException;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.dto.model.ParticipantProjectsV1DTO;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestProjectsApiFacadeTest {

    @Test
    void getParticipantProjectsByLogin_delegatesToParticipantApi() throws ApiException {
        ParticipantApi participantApi = mock(ParticipantApi.class);
        RestProjectsApiFacade facade = new RestProjectsApiFacade(participantApi);
        ParticipantProjectsV1DTO expected = new ParticipantProjectsV1DTO();

        when(participantApi.getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS"))
            .thenReturn(expected);

        ParticipantProjectsV1DTO actual = facade.getParticipantProjectsByLogin("login", 1000L, "IN_PROGRESS");

        assertSame(expected, actual);
        verify(participantApi).getParticipantProjectsByLogin("login", 1000L, 0L, "IN_PROGRESS");
    }
}
