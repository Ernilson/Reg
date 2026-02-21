package br.com.menu.service;

import br.com.menu.repository.VectorRepository;
import br.com.menu.repository.ChatMemoryRepository;
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

    // 🔹 NOVO: memória
    private final ChatMemoryRepository chatMemoryRepository;

    @Value("${rag.top-k:5}")
    private int topK;

    @Value("${rag.threshold:0.6}")
    private double threshold;

    @Value("${rag.max-context-chars:4000}")
    private int maxContextChars;

    @Value("${rag.max-chunks-per-source:2}")
    private int maxChunksPerSource;

    // 🔹 NOVO: quantas mensagens puxar da memória
    @Value("${rag.memory.last-n:10}")
    private int memoryLastN;

    public RagAnswer ask(String sessionId, String question) {

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "default";
        }

        // 0️⃣ Salva pergunta do usuário na memória
        chatMemoryRepository.add(UUID.randomUUID(), sessionId, "user", question);

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
                    [Documento: %s | pág: %s | chunk: %s]
                    %s

                    """.formatted(
                    r.source(),
                    r.page(),
                    r.content(),       // se seu SearchResult ainda não tem page, remove aqui
                    r.chunkIndex(),
                    r.content()
            );

            if (contextBuilder.length() + chunk.length() > maxContextChars) {
                break;
            }

            contextBuilder.append(chunk);
        }

        String context = contextBuilder.toString();

        // 🔹 7.1️⃣ Carrega memória da conversa (últimas N mensagens)
        var memoryDesc = chatMemoryRepository.lastMessages(sessionId, memoryLastN);

        // vem DESC -> reverte pra ficar cronológico no prompt
        var memory = new ArrayList<>(memoryDesc);
        Collections.reverse(memory);

        String memoryText = memory.stream()
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n"));

        // 8️⃣ Prompt (RAG + memória)
        String prompt = """
Você é um assistente chamado "amigo".

Regras:
1) Se a PERGUNTA for sobre identidade do assistente, saudação, preferências, ou continuação de conversa (ex: "qual é seu nome?"), responda normalmente usando a MEMÓRIA, mesmo que o CONTEXTO esteja vazio.
2) Se a PERGUNTA exigir fatos dos DOCUMENTOS, use APENAS o CONTEXTO do RAG como fonte.
3) Se a informação não estiver no CONTEXTO para perguntas que dependem de documentos, responda: "Não encontrei essa informação no documento."
4) Seja claro e direto.

MEMÓRIA (conversa):
%s

CONTEXTO (documentos):
%s

Pergunta: %s

Resposta:
""".formatted(memoryText, context, question);

        // 9️⃣ Geração
        String answer = ollamaService.generate(prompt);

        // 🔟 Salva resposta do assistente na memória
        chatMemoryRepository.add(UUID.randomUUID(), sessionId, "assistant", answer);

        return new RagAnswer(answer, unique);
    }

    public record RagAnswer(String answer,
                            List<VectorRepository.SearchResult> sources) {}
}
