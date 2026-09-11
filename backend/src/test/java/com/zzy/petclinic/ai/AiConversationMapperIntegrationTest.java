package com.zzy.petclinic.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

/** 使用 H2 真实执行 AI Mapper XML，防止 namespace、字段名和排序错误只在运行期暴露。 */
@SpringBootTest
@Sql(
    statements = {
      "DROP TABLE IF EXISTS ai_message",
      "DROP TABLE IF EXISTS ai_conversation",
      "CREATE TABLE ai_conversation (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,"
          + " title VARCHAR(120), status VARCHAR(20) NOT NULL, created_at TIMESTAMP, updated_at TIMESTAMP)",
      "CREATE TABLE ai_message (id BIGINT AUTO_INCREMENT PRIMARY KEY, conversation_id BIGINT NOT NULL,"
          + " role VARCHAR(20) NOT NULL, content VARCHAR(4000) NOT NULL, tool_name VARCHAR(100),"
          + " tool_payload VARCHAR(4000), created_at TIMESTAMP, updated_at TIMESTAMP)",
      "INSERT INTO ai_conversation(id,user_id,title,status,created_at,updated_at) VALUES"
          + " (1,7,'较早会话','ACTIVE',TIMESTAMP '2026-09-10 09:00:00',TIMESTAMP '2026-09-10 09:00:00'),"
          + " (2,7,'最近会话','ACTIVE',TIMESTAMP '2026-09-10 10:00:00',TIMESTAMP '2026-09-10 10:00:00'),"
          + " (3,8,'他人会话','ACTIVE',TIMESTAMP '2026-09-10 11:00:00',TIMESTAMP '2026-09-10 11:00:00')",
      "INSERT INTO ai_message(id,conversation_id,role,content,created_at,updated_at) VALUES"
          + " (1,1,'USER','第一问',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),"
          + " (2,1,'TOOL','工具调用成功',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),"
          + " (3,1,'ASSISTANT','第一答',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),"
          + " (4,1,'USER','第二问',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),"
          + " (5,1,'ASSISTANT','第二答',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"
    })
class AiConversationMapperIntegrationTest {

  @Autowired private AiConversationMapper conversationMapper;
  @Autowired private AiMessageMapper messageMapper;

  @Test
  void conversationQueriesEnforceOwnershipAndUseUpdatedAt() {
    assertEquals(1L, conversationMapper.selectOwnedActive(1L, 7L).getId());
    assertNull(conversationMapper.selectOwnedActive(1L, 8L));
    assertEquals(
        List.of(2L, 1L),
        conversationMapper.selectActiveByUserId(7L).stream()
            .map(AiConversation::getId)
            .toList());

    LocalDateTime touchedAt = LocalDateTime.of(2026, 9, 11, 12, 0);
    assertEquals(1, conversationMapper.touchOwned(1L, 7L, touchedAt));
    assertEquals(touchedAt, conversationMapper.selectById(1L).getUpdatedAt());
  }

  @Test
  void softDeleteOnlyChangesAnOwnedActiveConversation() {
    assertEquals(0, conversationMapper.softDeleteOwned(1L, 8L, LocalDateTime.now()));
    assertEquals(1, conversationMapper.softDeleteOwned(1L, 7L, LocalDateTime.now()));
    assertEquals("DELETED", conversationMapper.selectById(1L).getStatus());
    assertNull(conversationMapper.selectOwnedActive(1L, 7L));
  }

  @Test
  void messagesAreChronologicalAndContextKeepsTheLatestEntries() {
    assertEquals(
        List.of(1L, 2L, 3L, 4L, 5L),
        messageMapper.selectOwnedMessages(1L, 7L).stream().map(AiMessage::getId).toList());
    assertEquals(
        List.of(4L, 5L),
        messageMapper.selectOwnedContext(1L, 7L, 2).stream().map(AiMessage::getId).toList());
    assertEquals(List.of(), messageMapper.selectOwnedMessages(1L, 8L));
  }
}
