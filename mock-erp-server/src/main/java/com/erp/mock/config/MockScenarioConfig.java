package com.erp.mock.config;

import com.erp.mock.model.ErpSimulationScenario;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mock.erp")
public class MockScenarioConfig {

    private boolean csrfEnabled = true;
    private ErpSimulationScenario defaultScenario = ErpSimulationScenario.SUCCESS;
    private String authUsername = "SAP_COMMERCE_USER";
    private String authPassword = "SecretPassword123";

    public boolean isCsrfEnabled() {
        return csrfEnabled;
    }

    public void setCsrfEnabled(boolean csrfEnabled) {
        this.csrfEnabled = csrfEnabled;
    }

    public ErpSimulationScenario getDefaultScenario() {
        return defaultScenario;
    }

    public void setDefaultScenario(ErpSimulationScenario defaultScenario) {
        this.defaultScenario = defaultScenario;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }
}
