package br.com.menu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
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

        WebClient client = webClientBuilder
                .baseUrl(baseUrl)
                .build();

        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
        );

        Map<String, Object> response = client.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)   // 🔥 ESSENCIAL
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (response == null || !response.containsKey("response")) {
            throw new IllegalStateException("Resposta inválida do Ollama: " + response);
        }

        return response.get("response").toString();
    }
}

