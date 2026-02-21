package ru.izpz.edu.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    void extractLogin_shouldReturnNull_whenNull() {
        assertNull(StringUtils.extractLogin(null));
    }

    @Test
    void extractLogin_shouldReturnEmpty_whenEmpty() {
        assertEquals("", StringUtils.extractLogin(""));
    }

    @Test
    void extractLogin_shouldReturnSame_whenNoAt() {
        assertEquals("user", StringUtils.extractLogin("user"));
    }

    @Test
    void extractLogin_shouldStripDomain_whenAtPresent() {
        assertEquals("user", StringUtils.extractLogin("user@example.com"));
    }

    @Test
    void extractLogin_shouldNotStrip_whenAtFirstChar() {
        assertEquals("@user", StringUtils.extractLogin("@user"));
    }

    @Test
    void generateCode_shouldReturnDigitsWithRequestedLength() {
        String code = StringUtils.generateCode(6);
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }
}
