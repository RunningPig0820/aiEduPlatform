package com.ai.edu.application.service.learning;

import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import com.ai.edu.domain.shared.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 答疑对话归档器测试（task 7.10：幂等整写 tutoring/transcripts/{studentId}/{sessionId}.json + 时间戳）。
 */
class TutoringTranscriptArchiverTest {

    private static final Long STUDENT_ID = 501L;
    private static final Long SESSION_ID = 1001L;

    private FileStorageService fileStorageService;
    private TutoringTranscriptArchiver archiver;

    @BeforeEach
    void setUp() {
        fileStorageService = mock(FileStorageService.class);
        archiver = new TutoringTranscriptArchiver();
        archiver.setFileStorageService(fileStorageService);
        // 生产用 Spring ObjectMapper（含 JSR310）；测试需注册 JavaTimeModule 才能序列化消息 createdAt
        archiver.setObjectMapper(new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Test
    @DisplayName("整写到确定性 objectKey（按学生分目录），JSON 含完整消息 + 时间戳")
    void archive_writesIdempotentObjectKey() {
        when(fileStorageService.uploadToObjectKey(anyString(), any(), anyString()))
                .thenReturn("tutoring/transcripts/501/1001.json");

        String key = archiver.archive(STUDENT_ID, SESSION_ID, LocalDateTime.of(2026, 8, 5, 10, 0),
                List.of(TutoringChatMessage.user("鸡兔同笼，共35头94脚"), TutoringChatMessage.ai("先找已知条件")),
                TutoringState.ACTIVE, null);

        assertEquals("tutoring/transcripts/501/1001.json", key);
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(fileStorageService).uploadToObjectKey(
                eq("tutoring/transcripts/501/1001.json"), captor.capture(), eq("application/json"));
        String json = new String(captor.getValue());
        assertTrue(json.contains("\"session_id\":1001"), json);
        assertTrue(json.contains("\"student_id\":501"), json);
        assertTrue(json.contains("鸡兔同笼，共35头94脚"), json);
        assertTrue(json.contains("先找已知条件"), json);
        assertTrue(json.contains("\"status\":\"ACTIVE\""), json);
        assertTrue(json.contains("\"created_at\":\"2026-08-05T10:00"), json); // 会话开始时间
        assertTrue(json.contains("\"updated_at\":\""), json);                  // 最近整写时间
        assertTrue(json.contains("\"created_at\":\""), json);                  // 消息时间（含 AI/学生）
    }

    @Test
    @DisplayName("AI 消息的 thinking 推理过程一并整写进 transcript JSON")
    void archive_writesAiThinking() {
        when(fileStorageService.uploadToObjectKey(anyString(), any(), anyString()))
                .thenReturn("tutoring/transcripts/501/1001.json");

        archiver.archive(STUDENT_ID, SESSION_ID, LocalDateTime.now(),
                List.of(TutoringChatMessage.user("鸡兔同笼"),
                        TutoringChatMessage.ai("先找已知条件", "先考虑头数再算脚数差值")),
                TutoringState.ACTIVE, null);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(fileStorageService).uploadToObjectKey(anyString(), captor.capture(), anyString());
        String json = new String(captor.getValue());
        assertTrue(json.contains("\"thinking\":\"先考虑头数再算脚数差值\""), json); // thinking 落 transcript
        assertTrue(json.contains("\"role\":\"ai\""), json);
    }

    @Test
    @DisplayName("summary 可空——有则写入，无则省略")
    void archive_optionalSummary() {
        when(fileStorageService.uploadToObjectKey(anyString(), any(), anyString()))
                .thenReturn("tutoring/transcripts/501/1001.json");

        archiver.archive(STUDENT_ID, SESSION_ID, LocalDateTime.now(),
                List.of(TutoringChatMessage.user("设鸡x只")),
                TutoringState.ARCHIVED, "{\"knowledgePoints\":[\"二元一次方程组\"]}");

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(fileStorageService).uploadToObjectKey(anyString(), captor.capture(), anyString());
        String json = new String(captor.getValue());
        assertTrue(json.contains("二元一次方程组"), json);
        assertTrue(json.contains("\"status\":\"ARCHIVED\""), json);
    }

    // ==================== readMessages（COS 后端代理透传） ====================

    @Test
    @DisplayName("readMessages: 读 COS 反序列化完整消息（含 meta），key 与归档路径一致")
    void readMessages_parsesMessagesWithMeta() {
        when(fileStorageService.download("tutoring/transcripts/501/1001.json"))
                .thenReturn(("{\"session_id\":1001,\"student_id\":501,\"messages\":["
                        + "{\"role\":\"user\",\"content\":\"鸡兔同笼\",\"thinking\":null,\"created_at\":\"2026-08-05T10:00:00\"},"
                        + "{\"role\":\"ai\",\"content\":\"先找已知条件\",\"thinking\":\"先考虑头数再算脚数差值\","
                        + "\"created_at\":\"2026-08-05T10:00:10\",\"type\":\"hint\",\"decide_reason\":\"给分步引导\","
                        + "\"round\":1,\"question_kps\":[\"鸡兔同笼\"],\"status\":\"ACTIVE\"}]}").getBytes());

        List<TutoringChatMessage> messages = archiver.readMessages(STUDENT_ID, SESSION_ID);

        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).getRole());
        assertEquals("ai", messages.get(1).getRole());
        // meta 往返不丢（transcript 后端透传的核心）：type/decide_reason/round/question_kps/status/thinking
        assertEquals("hint", messages.get(1).getType());
        assertEquals("给分步引导", messages.get(1).getDecideReason());
        assertEquals(1, messages.get(1).getRound());
        assertEquals(List.of("鸡兔同笼"), messages.get(1).getQuestionKps());
        assertEquals("ACTIVE", messages.get(1).getStatus());
        assertEquals("先考虑头数再算脚数差值", messages.get(1).getThinking());
    }

    @Test
    @DisplayName("readMessages: COS 对象缺失（download=null）→ 空列表，不抛错")
    void readMessages_objectMissing_returnsEmpty() {
        when(fileStorageService.download("tutoring/transcripts/501/1001.json")).thenReturn(null);

        assertTrue(archiver.readMessages(STUDENT_ID, SESSION_ID).isEmpty());
    }

    @Test
    @DisplayName("readMessages: 反序列化失败（损坏 JSON）→ 空列表，不抛错")
    void readMessages_corruptJson_returnsEmpty() {
        when(fileStorageService.download("tutoring/transcripts/501/1001.json"))
                .thenReturn("{\"session_id\":".getBytes());

        assertTrue(archiver.readMessages(STUDENT_ID, SESSION_ID).isEmpty());
    }
}
