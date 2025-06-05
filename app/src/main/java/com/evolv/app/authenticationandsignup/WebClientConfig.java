package com.evolv.app.authenticationandsignup;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Exposes a WebClient bean preconfigured for Ollama API.
     */
    @Bean
    public WebClient ollamaClient(WebClient.Builder builder) {
        return builder
            .baseUrl("http://localhost:11434")  // your Ollama URL
            .build();
    }
}
