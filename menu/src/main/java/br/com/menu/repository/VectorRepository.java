package br.com.menu.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class VectorRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public void insertDocument(UUID id,
                               String source,
                               String documentName,
                               int page,
                               int chunkIndex,
                               String content,
                               List<Double> embedding) {

        String sql = """
            INSERT INTO documents
            (id, document_name, source, page, chunk_index, content, embedding)
            VALUES (:id, :documentName, :source, :page, :chunkIndex, :content, :embedding::vector)
        """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("documentName", documentName);
        params.put("source", source);
        params.put("page", page);
        params.put("chunkIndex", chunkIndex);
        params.put("content", content);
        params.put("embedding", toVectorLiteral(embedding));

        jdbc.update(sql, params);
    }

    public List<SearchResult> searchTopK(List<Double> queryEmbedding, int k, double threshold) {

        String sql = """
            SELECT id,
                   source,
                   content,
                   (embedding <=> :qvec::vector) AS distance
            FROM documents
            ORDER BY distance ASC
            LIMIT :k
        """;

        Map<String, Object> params = new HashMap<>();
        params.put("qvec", toVectorLiteral(queryEmbedding));
        params.put("k", k);
        params.put("threshold", threshold);

        return jdbc.query(sql, params, (rs, rowNum) ->
                new SearchResult(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("source"),
                        rs.getString("content"),
                        rs.getDouble("distance")
                )
        );
    }

    private String toVectorLiteral(List<Double> vector) {
        return "[" +
                vector.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","))
                + "]";
    }

    public record SearchResult(
            UUID id,
            String source,
            String content,
            double distance
    ) {}
}
