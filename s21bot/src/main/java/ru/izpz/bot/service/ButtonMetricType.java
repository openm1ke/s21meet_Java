package ru.izpz.bot.service;

public enum ButtonMetricType {
    KEYBOARD("keyboard"),
    LAST_COMMAND("last_command"),
    INLINE("inline");

    private final String tagValue;

    ButtonMetricType(String tagValue) {
        this.tagValue = tagValue;
    }

    public String tagValue() {
        return tagValue;
    }
}
