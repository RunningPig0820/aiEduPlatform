package com.ai.edu.application.service.learning;

import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import com.ai.edu.domain.shared.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        archiver.setObjectMapper(new ObjectMapper());
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
}
