
package com.ga.workspace_service.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.test.context.ContextConfiguration;

@CucumberContextConfiguration
@ContextConfiguration(classes = CucumberSpringConfiguration.TestConfig.class)
public class CucumberSpringConfiguration {

    @org.springframework.context.annotation.Configuration
    static class TestConfig {
    }
}
