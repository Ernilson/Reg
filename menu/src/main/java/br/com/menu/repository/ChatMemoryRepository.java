package br.com.menu.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatMemoryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public void add(UUID id, String sessionId, String role, String content) {
        String sql = """
      INSERT INTO chat_memory (id, session_id, role, content)
      VALUES (:id, :sessionId, :role, :content)
    """;

        var params = new HashMap<String, Object>();
        params.put("id", id);
        params.put("sessionId", sessionId);
        params.put("role", role);
        params.put("content", content);

        jdbc.update(sql, params);
    }

    public List<ChatMessage> lastMessages(String sessionId, int limit) {
        String sql = """
      SELECT role, content
      FROM chat_memory
      WHERE session_id = :sessionId
      ORDER BY created_at DESC
      LIMIT :limit
    """;

        var params = Map.of("sessionId", sessionId, "limit", limit);

        return jdbc.query(sql, params, (rs, i) ->
                new ChatMessage(rs.getString("role"), rs.getString("content"))
        );
    }

    public record ChatMessage(String role, String content) {}
}
