package ru.izpz.edu.utils;

import java.security.SecureRandom;

public class StringUtils {

    private static final SecureRandom RNG = new SecureRandom();

    private StringUtils() {}

    public static String extractLogin(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        int atIndex = input.indexOf('@');
        return (atIndex > 0) ? input.substring(0, atIndex) : input;
    }

    public static String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(RNG.nextInt(10));
        }
        return sb.toString();
    }
}
