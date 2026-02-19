package br.com.menu.service;

import br.com.menu.repository.VectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;
    private final OllamaService ollamaService;

    @Value("${rag.top-k:3}")
    private int topK;

    @Value("${rag.threshold:0.7}")
    private double threshold;

    @Value("${rag.max-context-chars:4000}")
    private int maxContextChars;

    public RagAnswer ask(String question) {

        // 1) Embedding da pergunta
        var qVec = embeddingService.generateEmbedding(question);

        // 2) Busca vetorial
        var results = vectorRepository.searchTopK(qVec, topK, threshold);

        // 3) Remove duplicação + ordena + aplica threshold
        var filtered = results.stream()
                .sorted(Comparator.comparingDouble(VectorRepository.SearchResult::distance))
                .filter(r -> r.distance() <= threshold)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                VectorRepository.SearchResult::content,
                                r -> r,
                                (r1, r2) -> r1,
                                LinkedHashMap::new
                        ),
                        m -> new ArrayList<>(m.values())
                ));

        // 4) Se nada passou no threshold → resposta segura
        if (filtered.isEmpty()) {
            return new RagAnswer(
                    "Não encontrei essa informação no contexto.",
                    Collections.emptyList()
            );
        }

        // 5) Construção segura do contexto com limite de tamanho
        StringBuilder contextBuilder = new StringBuilder();
        int totalChars = 0;

        for (var r : filtered) {

            String chunk = """
                    Fonte: %s
                    %s

                    """.formatted(r.source(), r.content());

            if (totalChars + chunk.length() > maxContextChars) {
                break;
            }

            contextBuilder.append(chunk);
            totalChars += chunk.length();
        }

        String context = contextBuilder.toString();

        // 6) Prompt anti-alucinação
        String prompt = """
                Você é um assistente técnico.
                Responda APENAS usando o CONTEXTO.
                Se não houver informação suficiente no contexto, diga:
                "Não encontrei essa informação no contexto."

                CONTEXTO:
                %s

                PERGUNTA:
                %s

                RESPOSTA:
                """.formatted(context, question);

        // 7) Geração
        String answer = ollamaService.generate(prompt);

        return new RagAnswer(answer, filtered);
    }

    public record RagAnswer(
            String answer,
            List<VectorRepository.SearchResult> sources
    ) {}
}
