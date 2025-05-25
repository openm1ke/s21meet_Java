package ru.izpz.edu.utils;

public class StringUtils {

    private StringUtils() {}

    public static String extractLogin(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        int atIndex = input.indexOf('@');
        return (atIndex > 0) ? input.substring(0, atIndex) : input;
    }
}
