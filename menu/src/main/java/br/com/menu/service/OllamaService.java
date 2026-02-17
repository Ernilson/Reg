package br.com.menu.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OllamaService {

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.chat-model}")
    private String model;

    private final WebClient webClient;

    public String generate(String prompt) {

        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
        );

        return webClient.post()
                .uri(baseUrl + "/api/generate")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> response.get("response").asText())
                .block();
    }
}

