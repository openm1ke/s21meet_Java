package ru.school21.edu.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@Sql(scripts = "/friends_import.sql")
class FriendsRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("test-db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    private FriendsRepository friendsRepository;

    @Test
    void findDistinctLogins() {
        List<String> distinctLogins = friendsRepository.findDistinctLogins();

        assertNotNull(distinctLogins, "Список логинов не должен быть null");
        assertFalse(distinctLogins.isEmpty(), "Список логинов не должен быть пустым");

        assertTrue(distinctLogins.contains("elevante"), "Логин 'elevante' должен присутствовать в списке");
        assertTrue(distinctLogins.contains("scrimgew"), "Логин 'scrimgew' должен присутствовать в списке");
        assertTrue(distinctLogins.contains("lucankri"), "Логин 'lucankri' должен присутствовать в списке");
        assertTrue(distinctLogins.contains("mjollror"), "Логин 'lucankri' должен присутствовать в списке");

        assertEquals(4, distinctLogins.size(), "Ожидается 4 уникальных логина");
    }
}