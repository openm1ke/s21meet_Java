package ru.izpz.auth.utils;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class CryptoKey {

    @Value("${security.crypto.key-base64}")
    private String keyBase64;

    @Getter
    private SecretKey key;
    public static final int IV_LEN = 12;
    public static final int TAG_BITS = 128;

    @PostConstruct
    void init() {
        byte[] k = Base64.getDecoder().decode(keyBase64);
        if (k.length != 16 && k.length != 24 && k.length != 32)
            throw new IllegalStateException("AES key must be 16/24/32 bytes");
        this.key = new SecretKeySpec(k, "AES");
    }

}

