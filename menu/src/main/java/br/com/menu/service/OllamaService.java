package br.com.menu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OllamaService {

    private final WebClient.Builder webClientBuilder;

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.chat-model}")
    private String model;

    public String generate(String prompt) {

        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
        );

        Map response = client.post()
                .uri("/api/generate") // ← AQUI ESTÁ A CORREÇÃO
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response.get("response").toString();
    }
}
