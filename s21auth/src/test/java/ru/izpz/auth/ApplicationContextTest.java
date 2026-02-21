package ru.izpz.auth;

import ru.izpz.auth.config.RestTemplateConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest {

    @Autowired
    private ApplicationContext context;

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
