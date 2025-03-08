package ru.school21.edu.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import ru.school21.edu.service.MessageSender;

@ActiveProfiles("test")
@TestConfiguration
public class NotifyServiceConfig {

    @Bean
    public MessageSender messageSender() {
        return Mockito.spy(new MessageSender());
    }
}
