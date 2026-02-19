package br.com.menu.repository;

import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
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
                               int page,
                               int chunkIndex,
                               String content,
                               List<Double> embedding) throws SQLException {

        String sql = """
        INSERT INTO documents (id, source, page, chunk_index, content, embedding)
        VALUES (:id, :source, :page, :chunkIndex, :content, :embedding)
    """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("source", source);
        params.put("page", page);
        params.put("chunkIndex", chunkIndex);
        params.put("content", content);
        params.put("embedding", toVectorLiteral(embedding));

        // 👇 AQUI FICA O PGvector
        PGvector pgVector = new PGvector(
                Arrays.toString(embedding.stream()
                        .mapToDouble(Double::doubleValue)
                        .toArray())
        );
        ;

        params.put("embedding", pgVector);

        jdbc.update(sql, params);
    }

    // ==============================
    // SEARCH TOP-K (COSINE DISTANCE)
    // ==============================
    public List<SearchResult> searchTopK(List<Double> queryEmbedding, int k, double threshold) {

        String sql = """
            SELECT id,
                   source,
                   content,
                   (embedding <=> :qvec::vector) AS distance
            FROM documents
            WHERE (embedding <=> :qvec::vector) < :threshold
            ORDER BY embedding <=> :qvec::vector
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
