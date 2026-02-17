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
    private String chatModel;

    private final WebClient webClient;

    public String generate(String prompt) {
        Map<String, Object> body = Map.of(
                "model", chatModel,
                "prompt", prompt,
                "stream", false
        );

        JsonNode response = webClient.post()
                .uri(baseUrl + "/api/generate")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || response.get("response") == null) {
            throw new IllegalStateException("Resposta inválida do Ollama: " + response);
        }

        return response.get("response").asText();
    }
}
