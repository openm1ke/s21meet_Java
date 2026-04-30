package ru.izpz.edu.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import ru.izpz.edu.BaseTestH2;

@DataJpaTest
@Transactional
@Sql(scripts = {"/friends_import.sql", "/online_import.sql"})
class FriendsRepositoryTest extends BaseTestH2 {

  @Autowired private FriendsRepository friendsRepository;

  @Test
  void findDistinctLogins() {
    List<String> distinctLogins = friendsRepository.findDistinctLogins();

    assertNotNull(distinctLogins, "Список логинов не должен быть null");
    assertFalse(distinctLogins.isEmpty(), "Список логинов не должен быть пустым");

    assertTrue(
        distinctLogins.contains("elevante"), "Логин 'elevante' должен присутствовать в списке");
    assertTrue(
        distinctLogins.contains("scrimgew"), "Логин 'scrimgew' должен присутствовать в списке");
    assertTrue(
        distinctLogins.contains("lucankri"), "Логин 'lucankri' должен присутствовать в списке");
    assertTrue(
        distinctLogins.contains("mjollror"), "Логин 'mjollror' должен присутствовать в списке");

    assertEquals(4, distinctLogins.size(), "Ожидается 4 уникальных логина");
  }
}
