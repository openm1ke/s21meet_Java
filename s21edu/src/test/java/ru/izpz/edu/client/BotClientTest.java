package ru.izpz.edu.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.NotifyRequest;
import ru.izpz.dto.StatusChange;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BotClientTest {

    @Mock
    private BotClient botClient;

    @Test
    void notify_shouldCallEndpoint() {
        NotifyRequest req = NotifyRequest.builder()
                .changes(List.of(new StatusChange("login", true, List.of("123"))))
                .build();

        botClient.notify(req);

        verify(botClient).notify(req);
    }
}
