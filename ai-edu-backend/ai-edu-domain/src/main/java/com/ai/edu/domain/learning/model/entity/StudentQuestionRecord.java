package com.ai.edu.domain.learning.model.entity;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 学生题目记录实体（掌握度**事实源**：每道题一条记录，可追溯「为什么是 64%」）。
 *
 * <p>AI 答疑/题型分析页题目全量落库（{@code source=ai}，题库 {@code bank} 预留），
 * 记录题目文本、LLM 原始题型名（弱标注，可能飘）、聚集后 canonical、对错信号（生效分值）、引导轮数。
 * 掌握表（{@link StudentTopicMastery}）是聚合结果，本实体是题目证据——后续改折扣系数/信号映射，
 * 基于本表重算聚合即可，证据不丢。
 *
 * <p>信号映射：直接答对（hintCount=0 且 answerRequestCount=0）→ score=1.0；引导后答对（≥1）→ 0.5；
 * 答错/未完成 → 0.0；× per-题型前几题打折（第 1 题 70% / 第 2 题 80% / 第 3 题起 100%，可配置）。
 * score 为**生效分值**（含打折后），与掌握表累计平均同源。
 *
 * <p>{@code canonicalLabel} 为空 = 题型未识别（PENDING）——信号照常采集，归属后回填聚合（掌握信号跟题走）。
 * {@code sessionId} = 原题链接（AI 答疑会话 ID，可跳回看原题；无会话记录为 null 显示题目原文）。
 */
@Getter
public class StudentQuestionRecord {

    private Long id;
    private Long studentId;
    private String content;
    private String source;
    private String topicLabel;
    private String canonicalLabel;
    private BigDecimal score;
    private int hintCount;
    private int answerRequestCount;
    private Long sessionId;
    private LocalDateTime createdAt;

    private StudentQuestionRecord() {
    }

    /** 从持久化状态恢复（重新水合），供仓储实现经 PO.toEntity 调用。 */
    public static StudentQuestionRecord restore(Long id, Long studentId, String content, String source,
                                                String topicLabel, String canonicalLabel, BigDecimal score,
                                                int hintCount, int answerRequestCount, Long sessionId,
                                                LocalDateTime createdAt) {
        StudentQuestionRecord record = new StudentQuestionRecord();
        record.id = id;
        record.studentId = studentId;
        record.content = content;
        record.source = source;
        record.topicLabel = topicLabel;
        record.canonicalLabel = canonicalLabel;
        record.score = normalizeScore(score);
        record.hintCount = hintCount;
        record.answerRequestCount = answerRequestCount;
        record.sessionId = sessionId;
        record.createdAt = createdAt;
        return record;
    }

    /** 新建题目记录（AI 答疑入口，默认 source=ai；canonical 待聚集，先为 null=PENDING）。 */
    public static StudentQuestionRecord create(Long studentId, String content, String topicLabel, BigDecimal score,
                                               int hintCount, int answerRequestCount, Long sessionId) {
        return restore(null, studentId, content, "ai", topicLabel, null, score,
                hintCount, answerRequestCount, sessionId, LocalDateTime.now());
    }

    /** 新建题目记录（全显式，bank 题库/题型分析页入口用；createdAt 显式便于测试与按时间追溯）。 */
    public static StudentQuestionRecord create(String source, Long studentId, String content, String topicLabel,
                                               String canonicalLabel, BigDecimal score, int hintCount,
                                               int answerRequestCount, Long sessionId, LocalDateTime createdAt) {
        return restore(null, studentId, content, source, topicLabel, canonicalLabel, score,
                hintCount, answerRequestCount, sessionId, createdAt);
    }

    /** 恢复持久化生成的主键（仓储实现经 PO.toEntity / insert 调用，沿用全项目 setId 约定）。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 聚集后回填 canonical（PENDING 归属时）。 */
    public void assignCanonical(String canonicalLabel) {
        this.canonicalLabel = canonicalLabel;
    }

    private static BigDecimal normalizeScore(BigDecimal score) {
        return score == null ? BigDecimal.ZERO.setScale(2) : score.setScale(2, RoundingMode.HALF_UP);
    }
}