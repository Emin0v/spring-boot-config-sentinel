package io.github.emin0v.configsentinel.rule;

public record ConfigurationViolation(String ruleId, String message) {

    public ConfigurationViolation {
        requireText(ruleId, "ruleId");
        requireText(message, "message");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
