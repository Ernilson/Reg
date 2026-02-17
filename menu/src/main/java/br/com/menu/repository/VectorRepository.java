package br.com.menu.repository;

import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class VectorRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public void insertDocument(UUID id, String source, String content, List<Double> embedding) {
        String sql = """
            INSERT INTO documents (id, source, content, embedding)
            VALUES (:id, :source, :content, :embedding)
        """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("source", source);
        params.put("content", content);
        params.put("embedding", toVectorPgObject(embedding));

        jdbc.update(sql, params);
    }

    public List<SearchResult> searchTopK(List<Double> queryEmbedding, int k) {
        // cosine distance: menor = mais similar
        String sql = """
            SELECT id, source, content,
                   (embedding <=> :qvec) AS distance
            FROM documents
            ORDER BY embedding <=> :qvec
            LIMIT :k
        """;

        Map<String, Object> params = new HashMap<>();
        params.put("qvec", toVectorPgObject(queryEmbedding));
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

    private PGobject toVectorPgObject(List<Double> vector) {
        try {
            PGobject pg = new PGobject();
            pg.setType("vector");
            pg.setValue(toVectorLiteral(vector));
            return pg;
        } catch (SQLException e) {
            throw new RuntimeException("Erro criando PGobject(vector)", e);
        }
    }

    private String toVectorLiteral(List<Double> vector) {
        // formato aceito pelo pgvector: [1,2,3]
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(vector.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    public record SearchResult(UUID id, String source, String content, double distance) {}
}
