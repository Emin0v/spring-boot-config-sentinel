package io.github.emin0v.configsentinel.autoconfigure;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("config-sentinel")
final class ConfigSentinelProperties {

    private boolean enabled = true;
    private List<String> profiles = List.of("prod", "production");
    private Action action = Action.FAIL;

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
}
