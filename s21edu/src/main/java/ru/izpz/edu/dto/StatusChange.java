package ru.izpz.edu.dto;

import java.util.List;

public record StatusChange(String login, boolean newStatus, List<String> telegramIds) {}

