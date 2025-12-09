package com.example.demo.ia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class IAConfig {

    @Value("${ia.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ia.ollama.model:llama3.2:1b}")
    private String modelName;
    
    @Value("${ia.ollama.timeout:300000}")
    private int timeout;

    @Bean
    public RestTemplate restTemplate() {
        System.out.println("⚙️ Configurando RestTemplate con timeout de: " + timeout + "ms");
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        return restTemplate;
    }
    
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    }

    // Configuración simple - Spring AI se auto-configura
    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public String getChatEndpoint() {
        return ollamaBaseUrl + "/api/chat";
    }
    
    public String getGenerateEndpoint() {
        return ollamaBaseUrl + "/api/generate";
    }
}
