package ru.izpz.rocket.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rocketchat")
@Data
public class RocketChatProperties {
    
    private String websocketUri;
    private String botUsername;
    private String token;
    private long qrTimeout = 30;
    private long messageTimeout = 15;
}
