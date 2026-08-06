package com.ai.edu.application.service.learning;

import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import com.ai.edu.domain.shared.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 答疑对话归档器（任务 7.10）——每轮对话实时整写 COS，恒为完整对话。
 *
 * <p>约定：objectKey = {@code tutoring/transcripts/{studentId}/{sessionId}.json}（按学生分目录便于整理/清理），
 * <b>幂等整写</b>（同 key 覆盖，每轮写入最新完整对话）；首次实时写即回填 {@code transcript_url}（由编排服务调用）。
 * 会话结束（收尾/归档）终态写一次。
 *
 * <p>时间信息：根节点 {@code created_at}=会话开始、{@code updated_at}=最近一次整写；每条消息带 {@code created_at}——
 * 供回放展示与按时间清理（清理建议由 DB {@code t_tutoring_session.archived_at} 驱动，transcript 时间戳作校验）。
 *
 * <p>脱敏：当前消息字段仅 role/content/createdAt，无显式 PII；后续如有敏感字段在此挂钩脱敏。
 */
@Slf4j
@Service
public class TutoringTranscriptArchiver {

    public static final String TRANSCRIPT_DIR = "tutoring/transcripts";

    @Resource
    private FileStorageService fileStorageService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 整写完整对话到 COS。
     *
     * @param studentId   学生 ID（归档路径按学生分目录）
     * @param sessionId   会话 ID
     * @param createdAt   会话开始时间（根节点 created_at，回放/清理用）
     * @param messages    完整消息列表（学生 + AI，按时间顺序，含每条 created_at）
     * @param status      当前会话状态
     * @param summaryText 收尾总结（可空）
     * @return COS objectKey（如 {@code tutoring/transcripts/501/1001.json}，首次写由编排服务回填 transcript_url）
     */
    public String archive(Long studentId, Long sessionId, LocalDateTime createdAt,
                          List<TutoringChatMessage> messages, TutoringState status, String summaryText) {
        String objectKey = TRANSCRIPT_DIR + "/" + studentId + "/" + sessionId + ".json";
        byte[] content = buildTranscriptJson(studentId, sessionId, createdAt, messages, status, summaryText);
        fileStorageService.uploadToObjectKey(objectKey, content, "application/json");
        log.info("[tutoring] 对话整写 COS: objectKey={}, msgs={}, status={}",
                objectKey, messages == null ? 0 : messages.size(), status);
        return objectKey;
    }

    private byte[] buildTranscriptJson(Long studentId, Long sessionId, LocalDateTime createdAt,
                                       List<TutoringChatMessage> messages, TutoringState status, String summaryText) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("session_id", sessionId);
        root.put("student_id", studentId);
        root.put("status", status == null ? null : status.name());
        root.put("subject", "math");
        root.put("created_at", createdAt == null ? LocalDateTime.now().toString() : createdAt.toString());
        root.put("updated_at", LocalDateTime.now().toString());
        if (summaryText != null && !summaryText.isBlank()) {
            root.put("summary", summaryText);
        }
        root.put("messages", messages == null ? List.of() : messages);
        try {
            return objectMapper.writeValueAsBytes(root);
        } catch (Exception e) {
            throw new IllegalStateException("对话归档 JSON 序列化失败: " + e.getMessage(), e);
        }
    }

    /** 测试用：注入 FileStorageService（Spring 默认 @Resource 注入）。 */
    void setFileStorageService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /** 测试用：注入 ObjectMapper（Spring 默认 @Resource 注入）。 */
    void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
