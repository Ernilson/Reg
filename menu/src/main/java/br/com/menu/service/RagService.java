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

    @Value("${rag.top-k:5}")
    private int topK;

    @Value("${rag.threshold:0.6}")
    private double threshold;

    @Value("${rag.max-context-chars:4000}")
    private int maxContextChars;

    @Value("${rag.max-chunks-per-source:2}")
    private int maxChunksPerSource;


    public RagAnswer ask(String question) {

        // 1️⃣ Embedding
        var qVec = embeddingService.generateEmbedding(question);

        // 2️⃣ Busca vetorial
        var results = vectorRepository.searchTopK(qVec, topK, threshold);

        // 3️⃣ Ordena por relevância (distance)
        var sortedByRelevance = results.stream()
                .sorted(Comparator.comparingDouble(VectorRepository.SearchResult::distance))
                .toList();

        // 4️⃣ Filtra por threshold + limita por source
        var limitedPerSource = sortedByRelevance.stream()
                .filter(r -> r.distance() <= threshold)
                .collect(Collectors.groupingBy(
                        VectorRepository.SearchResult::source,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .limit(maxChunksPerSource)
                                        .toList()
                        )
                ))
                .values()
                .stream()
                .flatMap(List::stream)
                .toList();

        // 5️⃣ Remove duplicação por conteúdo
        var unique = limitedPerSource.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                VectorRepository.SearchResult::content,
                                r -> r,
                                (r1, r2) -> r1,
                                LinkedHashMap::new
                        ),
                        m -> new ArrayList<>(m.values())
                ));

        // 6️⃣ REORDENA POR CHUNK_INDEX (Para manter a ordem lógica do texto)
        unique.sort(Comparator.comparingInt(VectorRepository.SearchResult::chunkIndex));

        // 7️⃣ Monta contexto com limite de tamanho
        StringBuilder contextBuilder = new StringBuilder();

        for (var r : unique) {
            String chunk = """
                    [Documento: %s]
                    %s
                    
                    """.formatted(r.source(), r.content());

            if (contextBuilder.length() + chunk.length() > maxContextChars) {
                break;
            }

            contextBuilder.append(chunk);
        }

        String context = contextBuilder.toString();

        // 8️⃣ Prompt para TinyLlama
        String prompt = """
Você é um assistente útil. Responda à pergunta com base APENAS no contexto abaixo.

Contexto:
%s

Pergunta: %s

Resposta:
""".formatted(context, question);


        // 9️⃣ Geração
        String answer = ollamaService.generate(prompt);

        return new RagAnswer(answer, unique);
    }

    public record RagAnswer(String answer,
                            List<VectorRepository.SearchResult> sources) {}
}
