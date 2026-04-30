package ru.izpz.edu;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Базовый класс для JPA-тестов на H2. */
@DataJpaTest
@ActiveProfiles("test")
public abstract class BaseTestH2 {}
