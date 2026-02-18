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

    // ==============================
    // INSERT DOCUMENT
    // ==============================
    public void insertDocument(UUID id,
                               String source,
                               String content,
                               List<Double> embedding) {

        String sql = """
            INSERT INTO documents (id, source, content, embedding)
            VALUES (:id, :source, :content, :embedding::vector)
        """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("source", source);
        params.put("content", content);
        params.put("embedding", toVectorLiteral(embedding));

        jdbc.update(sql, params);
    }

    // ==============================
    // SEARCH TOP-K (COSINE DISTANCE)
    // ==============================
    public List<SearchResult> searchTopK(List<Double> queryEmbedding, int k) {

        String sql = """
            SELECT id,
                   source,
                   content,
                   (embedding <=> :qvec::vector) AS distance
            FROM documents
            ORDER BY embedding <=> :qvec::vector
            LIMIT :k
        """;

        Map<String, Object> params = new HashMap<>();
        params.put("qvec", toVectorLiteral(queryEmbedding));
        params.put("k", k);

        return jdbc.query(sql, params, (rs, rowNum) ->
                new SearchResult(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("source"),
                        rs.getString("content"),
                        rs.getDouble("distance")
                )
        );
    }

    // ==============================
    // VECTOR -> STRING FORMAT
    // ==============================
    private String toVectorLiteral(List<Double> vector) {
        return "[" +
                vector.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","))
                + "]";
    }

    // ==============================
    // RESULT RECORD
    // ==============================
    public record SearchResult(
            UUID id,
            String source,
            String content,
            double distance
    ) {}
}
