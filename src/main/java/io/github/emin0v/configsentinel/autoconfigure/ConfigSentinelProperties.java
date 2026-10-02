package io.github.emin0v.configsentinel.autoconfigure;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("config-sentinel")
final class ConfigSentinelProperties {

    private boolean enabled = true;
    private List<String> profiles = List.of("prod", "production");
    private Action action = Action.FAIL;
    private List<CustomRule> customRules = List.of();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<String> profiles) {
        this.profiles = profiles;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public List<CustomRule> getCustomRules() {
        return customRules;
    }

    public void setCustomRules(List<CustomRule> customRules) {
        this.customRules = customRules;
    }

    void validate() {
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalArgumentException("config-sentinel.profiles must contain at least one profile");
        }
        if (profiles.stream().anyMatch(profile -> profile == null || profile.isBlank())) {
            throw new IllegalArgumentException("config-sentinel.profiles must not contain blank profiles");
        }
        if (action == null) {
            throw new IllegalArgumentException("config-sentinel.action must not be null");
        }
    }

    enum Action {
        FAIL,
        WARN
    }

    static final class CustomRule {

        private String property;
        private List<String> forbiddenValues = List.of();

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public List<String> getForbiddenValues() {
            return forbiddenValues;
        }

        public void setForbiddenValues(List<String> forbiddenValues) {
            this.forbiddenValues = forbiddenValues;
        }
    }
}
