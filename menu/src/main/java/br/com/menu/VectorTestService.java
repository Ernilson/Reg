package br.com.menu;

import br.com.menu.repository.VectorRepository;
import br.com.menu.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VectorTestService {

    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;

    public void seed() {
        insert("doc1", "RAG combina busca vetorial com LLM para responder com contexto.");
        insert("doc2", "Spring Boot é um framework Java para APIs e microserviços.");
        insert("doc3", "pgvector permite armazenar e buscar embeddings no PostgreSQL.");
    }

    private void insert(String source, String content) {
        List<Double> emb = embeddingService.generateEmbedding(content);
        vectorRepository.insertDocument(UUID.randomUUID(), source, content, emb);
    }

    public List<VectorRepository.SearchResult> search(String query) {
        List<Double> qEmb = embeddingService.generateEmbedding(query);
        return vectorRepository.searchTopK(qEmb, 3);
    }
}

