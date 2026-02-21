package ru.izpz.auth.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CryptoKeyTest {

    @InjectMocks
    private CryptoKey cryptoKey;

    @BeforeEach
    void setUp() {
        // Сброс статического поля перед каждым тестом
        ReflectionTestUtils.setField(CryptoKey.class, "key", null);
    }

    @Test
    void init_shouldSetKey_whenKeyBase64IsValid16Bytes() {
        String validKey16Bytes = Base64.getEncoder().encodeToString(new byte[16]);
        ReflectionTestUtils.setField(cryptoKey, "keyBase64", validKey16Bytes);

        assertDoesNotThrow(() -> cryptoKey.init());

        SecretKeySpec key = (SecretKeySpec) ReflectionTestUtils.getField(CryptoKey.class, "key");
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals(16, key.getEncoded().length);
    }

    @Test
    void init_shouldSetKey_whenKeyBase64IsValid24Bytes() {
        String validKey24Bytes = Base64.getEncoder().encodeToString(new byte[24]);
        ReflectionTestUtils.setField(cryptoKey, "keyBase64", validKey24Bytes);

        assertDoesNotThrow(() -> cryptoKey.init());

        SecretKeySpec key = (SecretKeySpec) ReflectionTestUtils.getField(CryptoKey.class, "key");
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals(24, key.getEncoded().length);
    }

    @Test
    void init_shouldSetKey_whenKeyBase64IsValid32Bytes() {
        String validKey32Bytes = Base64.getEncoder().encodeToString(new byte[32]);
        ReflectionTestUtils.setField(cryptoKey, "keyBase64", validKey32Bytes);

        assertDoesNotThrow(() -> cryptoKey.init());

        SecretKeySpec key = (SecretKeySpec) ReflectionTestUtils.getField(CryptoKey.class, "key");
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals(32, key.getEncoded().length);
    }

    @Test
    void init_shouldThrowException_whenKeyBase64IsInvalidLength() {
        String invalidKey12Bytes = Base64.getEncoder().encodeToString(new byte[12]);
        ReflectionTestUtils.setField(cryptoKey, "keyBase64", invalidKey12Bytes);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> cryptoKey.init());
        assertEquals("AES key must be 16/24/32 bytes", exception.getMessage());
    }

    @Test
    void init_shouldThrowException_whenKeyBase64IsInvalidBase64() {
        ReflectionTestUtils.setField(cryptoKey, "keyBase64", "invalid-base64-string");

        assertThrows(IllegalArgumentException.class, () -> cryptoKey.init());
    }

    @Test
    void init_shouldThrowException_whenKeyBase64IsNull() {
        ReflectionTestUtils.setField(cryptoKey, "keyBase64", null);

        assertThrows(Exception.class, () -> cryptoKey.init());
    }

    @Test
    void constants_shouldHaveCorrectValues() {
        assertEquals(12, CryptoKey.IV_LEN);
        assertEquals(128, CryptoKey.TAG_BITS);
    }
}
