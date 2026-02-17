package br.com.menu.service;

import br.com.menu.repository.VectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;
    private final OllamaService ollamaService;

    @Value("${rag.top-k:3}")
    private int topK;

    public RagAnswer ask(String question) {
        // 1) embedding da pergunta
        var qVec = embeddingService.generateEmbedding(question);

        // 2) busca vetorial
        var results = vectorRepository.searchTopK(qVec, topK);

        // 3) monta contexto (topK chunks)
        String context = results.stream()
                .map(r -> "- (" + r.source() + ") " + r.content())
                .collect(Collectors.joining("\n"));

        // 4) prompt “anti-alucinação”
        String prompt = """
                Você é um assistente técnico. Responda APENAS usando o CONTEXTO.
                Se não houver informação suficiente no contexto, diga: "Não encontrei essa informação no contexto."

                CONTEXTO:
                %s

                PERGUNTA:
                %s

                RESPOSTA:
                """.formatted(context, question);

        // 5) geração
        String answer = ollamaService.generate(prompt);

        return new RagAnswer(answer, results);
    }

    public record RagAnswer(String answer, List<VectorRepository.SearchResult> sources) {}
}
