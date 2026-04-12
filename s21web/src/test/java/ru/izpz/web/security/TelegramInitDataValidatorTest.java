package ru.izpz.web.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramInitDataValidatorTest {

    private static final String BOT_TOKEN = "123456:TEST_BOT_TOKEN";

    @Test
    void isValid_returnsTrueForProperlySignedData() {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofHours(1));
        String initData = sign(Map.of(
            "auth_date", String.valueOf(Instant.now().getEpochSecond()),
            "query_id", "AAGAAQ",
            "user", "{\"id\":42,\"first_name\":\"Mike\"}"
        ));

        assertTrue(validator.isValid(initData));
    }

    @Test
    void isValid_returnsFalseWhenSignatureIsTampered() {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofHours(1));
        String initData = sign(Map.of(
            "auth_date", String.valueOf(Instant.now().getEpochSecond()),
            "query_id", "AAGAAQ",
            "user", "{\"id\":42}"
        ));

        assertFalse(validator.isValid(initData + "00"));
    }

    @Test
    void isValid_returnsFalseWhenAuthDateIsExpired() {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofMinutes(1));
        String initData = sign(Map.of(
            "auth_date", String.valueOf(Instant.now().minusSeconds(120).getEpochSecond()),
            "query_id", "AAGAAQ",
            "user", "{\"id\":42}"
        ));

        assertFalse(validator.isValid(initData));
    }

    @Test
    void isValid_returnsFalseWhenInitDataEmptyOrBotTokenMissing() {
        TelegramInitDataValidator withToken = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofHours(1));
        TelegramInitDataValidator withoutToken = new TelegramInitDataValidator("", Duration.ofHours(1));

        assertFalse(withToken.isValid(""));
        assertFalse(withToken.isValid("   "));
        assertFalse(withoutToken.isValid("auth_date=1&hash=abc"));
    }

    @Test
    void isValid_returnsFalseWhenHashMissing() {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofHours(1));
        String initDataWithoutHash = "auth_date=" + Instant.now().getEpochSecond() + "&query_id=AAGAAQ";

        assertFalse(validator.isValid(initDataWithoutHash));
    }

    @Test
    void isValid_returnsFalseWhenAuthDateIsInvalidOrInFuture() {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofHours(1));
        String nonNumeric = sign(Map.of(
            "auth_date", "not-a-number",
            "query_id", "AAGAAQ",
            "user", "{\"id\":42}"
        ));
        String futureDate = sign(Map.of(
            "auth_date", String.valueOf(Instant.now().plusSeconds(60).getEpochSecond()),
            "query_id", "AAGAAQ",
            "user", "{\"id\":42}"
        ));

        assertFalse(validator.isValid(nonNumeric));
        assertFalse(validator.isValid(futureDate));
    }

    @Test
    void isValid_returnsFalseWhenQueryContainsKeyWithoutValueSeparator() {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofHours(1));
        String initData = sign(Map.of(
            "auth_date", String.valueOf(Instant.now().getEpochSecond()),
            "query_id", "AAGAAQ",
            "user", "{\"id\":42}"
        ));
        String malformed = initData + "&novaluekey";

        assertFalse(validator.isValid(malformed));
    }

    @Test
    void isValid_returnsFalseWhenAuthDateMissingEvenWithValidHash() {
        TelegramInitDataValidator validator = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofHours(1));
        String initDataWithoutAuthDate = sign(Map.of(
            "query_id", "AAGAAQ",
            "user", "{\"id\":42,\"first_name\":\"Mike\"}"
        ));

        assertFalse(validator.isValid(initDataWithoutAuthDate));
    }

    @Test
    void isValid_throwsWhenHmacProviderUnavailable() {
        String initData = sign(Map.of(
            "auth_date", String.valueOf(Instant.now().getEpochSecond()),
            "query_id", "AAGAAQ",
            "user", "{\"id\":42,\"first_name\":\"Mike\"}"
        ));
        TelegramInitDataValidator validator = new TelegramInitDataValidator(BOT_TOKEN, Duration.ofHours(1));
        Provider[] originalProviders = Security.getProviders();

        try {
            for (Provider provider : originalProviders) {
                Security.removeProvider(provider.getName());
            }
            assertThrows(IllegalStateException.class, () -> validator.isValid(initData));
        } finally {
            for (Provider provider : Security.getProviders()) {
                Security.removeProvider(provider.getName());
            }
            for (Provider provider : originalProviders) {
                Security.addProvider(provider);
            }
        }
    }

    private String sign(Map<String, String> params) {
        String dataCheckString = params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("\n"));

        byte[] secretKey = hmacSha256("WebAppData".getBytes(StandardCharsets.UTF_8), BOT_TOKEN.getBytes(StandardCharsets.UTF_8));
        String hash = toHex(hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

        return params.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&")) + "&hash=" + hash;
    }

    private byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
