package ru.dnechoroshev.simplecommunicator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConfigurationProperties(prefix = "general.debug")
@Data
public class DebugConfiguration {

    private boolean enableAutoResponse;

    private String autoResponseCallee;

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

}
