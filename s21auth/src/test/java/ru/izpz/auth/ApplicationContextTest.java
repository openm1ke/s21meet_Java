package ru.izpz.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import ru.izpz.auth.config.RestTemplateConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ApplicationContextTest {

    @Autowired
    private ApplicationContext context;
    
    @Autowired
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // TokenClient will be created manually when needed
    }

    @Test
    void contextLoads() {
        // Если контекст не загрузится, тест упадёт.
    }

    @Test
    void myConfigBeanIsLoaded() {
        // Пытаемся получить бин конфигурации из контекста
        RestTemplateConfig myConfig = context.getBean(RestTemplateConfig.class);
        assertNotNull(myConfig, "Бин RestTemplateConfig должен быть загружен в контексте");
    }
}