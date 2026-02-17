package br.com.menu.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.embedding-model}")
    private String model;

    private final WebClient webClient;

    public List<Double> generateEmbedding(String text) {

        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", text
        );

        JsonNode response = webClient.post()
                .uri(baseUrl + "/api/embeddings")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || response.get("embedding") == null) {
            throw new IllegalStateException("Resposta de embeddings inválida do Ollama: " + response);
        }

        List<Double> vector = new ArrayList<>();
        response.get("embedding").forEach(v -> vector.add(v.asDouble()));

        return vector;
    }
}

