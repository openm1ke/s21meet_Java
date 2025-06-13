package ru.izpz.edu.utils;

import java.security.SecureRandom;

public class StringUtils {

    private StringUtils() {}

    public static String extractLogin(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        int atIndex = input.indexOf('@');
        return (atIndex > 0) ? input.substring(0, atIndex) : input;
    }

    public static String generateCode(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
