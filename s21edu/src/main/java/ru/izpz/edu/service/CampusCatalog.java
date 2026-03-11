package ru.izpz.edu.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CampusCatalog {

    private static final Map<String, String> KNOWN_CAMPUSES = new LinkedHashMap<>();

    static {
        KNOWN_CAMPUSES.put("6bfe3c56-0211-4fe1-9e59-51616caac4dd", "MSK");
        KNOWN_CAMPUSES.put("7c293c9c-f28c-4b10-be29-560e4b000a34", "KZN");
        KNOWN_CAMPUSES.put("46e7d965-21e9-4936-bea9-f5ea0d1fddf2", "NSK");
    }

    public List<String> targetCampusIds() {
        return List.copyOf(KNOWN_CAMPUSES.keySet());
    }

    public String campusName(String campusId) {
        return KNOWN_CAMPUSES.getOrDefault(campusId, "UNKNOWN");
    }
}
