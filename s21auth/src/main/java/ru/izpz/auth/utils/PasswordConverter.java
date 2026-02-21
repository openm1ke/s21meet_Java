package ru.izpz.auth.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Converter
public class PasswordConverter implements AttributeConverter<String, String> {

    private static final SecureRandom RNG = new SecureRandom();
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final String PREFIX = "ENC$";

    @Override
    public String convertToDatabaseColumn(String attr) {
        if (attr == null || attr.isBlank()) return attr;
        try {
            byte[] iv = new byte[CryptoKey.IV_LEN];
            RNG.nextBytes(iv);

            Cipher c = Cipher.getInstance(TRANSFORM);
            c.init(Cipher.ENCRYPT_MODE, CryptoKey.key, new GCMParameterSpec(CryptoKey.TAG_BITS, iv));
            byte[] ct = c.doFinal(attr.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Encrypt error", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String db) {
        if (db == null || db.isBlank()) return db;
        if (!db.startsWith(PREFIX)) return db;
        try {
            byte[] all = Base64.getDecoder().decode(db.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(all, 0, CryptoKey.IV_LEN);
            byte[] ct = Arrays.copyOfRange(all, CryptoKey.IV_LEN, all.length);

            Cipher c = Cipher.getInstance(TRANSFORM);
            c.init(Cipher.DECRYPT_MODE, CryptoKey.key, new GCMParameterSpec(CryptoKey.TAG_BITS, iv));
            byte[] pt = c.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decrypt error", e);
        }
    }
}
