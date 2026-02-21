package ru.izpz.auth.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PasswordConverterTest {

    private PasswordConverter passwordConverter;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAA=="); // 16 bytes key
        CryptoKey cryptoKey = new CryptoKey();
        ReflectionTestUtils.setField(cryptoKey, "keyBase64", Base64.getEncoder().encodeToString(keyBytes));
        cryptoKey.init();
        
        passwordConverter = new PasswordConverter(cryptoKey);
    }

    @Test
    void convertToDatabaseColumn_shouldReturnNull_whenInputIsNull() {
        String result = passwordConverter.convertToDatabaseColumn(null);
        assertNull(result);
    }

    @Test
    void convertToDatabaseColumn_shouldReturnBlank_whenInputIsBlank() {
        String result = passwordConverter.convertToDatabaseColumn("");
        assertEquals("", result);
        result = passwordConverter.convertToDatabaseColumn("   ");
        assertEquals("   ", result);
    }

    @Test
    void convertToDatabaseColumn_shouldEncryptPassword() {
        String password = "testPassword123";
        String encrypted = passwordConverter.convertToDatabaseColumn(password);

        assertNotNull(encrypted);
        assertNotEquals(password, encrypted);
        assertTrue(encrypted.startsWith("enc:"));
    }

    @Test
    void convertToEntityAttribute_shouldReturnNull_whenInputIsNull() {
        String result = passwordConverter.convertToEntityAttribute(null);
        assertNull(result);
    }

    @Test
    void convertToEntityAttribute_shouldReturnBlank_whenInputIsBlank() {
        String result = passwordConverter.convertToEntityAttribute("");
        assertEquals("", result);
        result = passwordConverter.convertToEntityAttribute("   ");
        assertEquals("   ", result);
    }

    @Test
    void convertToEntityAttribute_shouldReturnUnencrypted_whenNotEncrypted() {
        String plainText = "plainText";
        String result = passwordConverter.convertToEntityAttribute(plainText);

        assertEquals(plainText, result);
    }

    @Test
    void convertToEntityAttribute_shouldDecryptPassword() {
        String password = "testPassword123";
        String encrypted = passwordConverter.convertToDatabaseColumn(password);
        String decrypted = passwordConverter.convertToEntityAttribute(encrypted);

        assertEquals(password, decrypted);
    }

    @Test
    void encryptionDecryption_shouldBeConsistent() {
        String[] passwords = {
            "simple",
            "password123",
            "veryLongPasswordWithSpecialChars!@#$%^&*()",
            "паролькириллицей",
            "🔐🔑🔒"
        };

        for (String password : passwords) {
            String encrypted = passwordConverter.convertToDatabaseColumn(password);
            String decrypted = passwordConverter.convertToEntityAttribute(encrypted);
            assertEquals(password, decrypted, "Failed for password: " + password);
        }
    }

    @Test
    void encryption_shouldProduceDifferentResultsEachTime() {
        String password = "testPassword";
        String encrypted1 = passwordConverter.convertToDatabaseColumn(password);
        String encrypted2 = passwordConverter.convertToDatabaseColumn(password);

        assertNotEquals(encrypted1, encrypted2, "Each encryption should produce different result due to random IV");
    }

    @Test
    void convertToDatabaseColumn_shouldThrowException_onEncryptionError() {
        // Set cryptoKey to null using reflection
        try {
            var cryptoKeyField = PasswordConverter.class.getDeclaredField("cryptoKey");
            cryptoKeyField.setAccessible(true);
            cryptoKeyField.set(passwordConverter, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        assertThrows(IllegalStateException.class, () -> 
            passwordConverter.convertToDatabaseColumn("password")
        );
    }

    @Test
    void convertToEntityAttribute_shouldThrowException_onDecryptionError() {
        String invalidEncrypted = "enc:invalidBase64Data";
        
        assertThrows(IllegalStateException.class, () -> 
            passwordConverter.convertToEntityAttribute(invalidEncrypted)
        );
    }

    @Test
    void convertToEntityAttribute_shouldThrowException_whenDataIsCorrupted() {
        String corruptedEncrypted = "enc:" + Base64.getEncoder().encodeToString("corruptedData".getBytes());
        
        assertThrows(IllegalStateException.class, () -> 
            passwordConverter.convertToEntityAttribute(corruptedEncrypted)
        );
    }
}
