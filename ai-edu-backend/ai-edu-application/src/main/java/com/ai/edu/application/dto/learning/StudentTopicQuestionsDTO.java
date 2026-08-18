package com.ai.edu.application.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 按题型查题目响应（tasks 4.2，api.md 接口 2）——掌握度页「查看题目」。
 *
 * <p>包装 studentId + topicLabel + questions（题目证据列表，含 session_id 原题链接）；
 * 无记录返回空数组，不报错。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentTopicQuestionsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 学生 ID */
    private Long studentId;

    /** 题型名（canonical） */
    private String topicLabel;

    /** 该题型下题目记录列表（空态 = 空数组） */
    private List<StudentQuestionItemDTO> questions;
}
