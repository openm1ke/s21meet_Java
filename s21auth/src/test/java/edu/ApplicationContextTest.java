package edu;

import edu.config.WebClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
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
        WebClientConfig myConfig = context.getBean(WebClientConfig.class);
        assertNotNull(myConfig, "Бин WebClientConfig должен быть загружен в контексте");
    }
}
