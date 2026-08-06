package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Python decide 输出 ActionMeta 契约测试（snake_case 解析、未知字段容忍、默认值）。
 */
class ActionMetaContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    @DisplayName("解析 Python snake_case 完整输出")
    void parse_snakeCaseFull() throws Exception {
        String json = """
                {
                  "type": "approach",
                  "eval": {
                    "correct": false,
                    "error_type": "COMPUTATION",
                    "emotion": "CONFUSED",
                    "exercise_complete": false
                  },
                  "mastery_signals": [{"kp_label": "二元一次方程组", "signal": "practicing"}],
                  "new_question": null,
                  "end_reason": null,
                  "summary": null,
                  "safety_flag": false,
                  "degraded": false
                }
                """;

        ActionMeta meta = objectMapper.readValue(json, ActionMeta.class);

        assertEquals("approach", meta.getType());
        assertEquals(Boolean.FALSE, meta.getEval().getCorrect());
        assertEquals("COMPUTATION", meta.getEval().getErrorType());
        assertEquals("CONFUSED", meta.getEval().getEmotion());
        assertEquals(Boolean.FALSE, meta.getEval().getExerciseComplete());
        assertEquals(1, meta.getMasterySignals().size());
        assertEquals("二元一次方程组", meta.getMasterySignals().get(0).getKpLabel());
        assertEquals("practicing", meta.getMasterySignals().get(0).getSignal());
        assertNull(meta.getNewQuestion());
        assertNull(meta.getEndReason());
        assertEquals(Boolean.FALSE, meta.getSafetyFlag());
        assertEquals(Boolean.FALSE, meta.getDegraded());
    }

    @Test
    @DisplayName("reason 为 Python 可选调试字段，Java 不建模——未知字段容忍")
    void parse_unknownReasonTolerated() throws Exception {
        String json = """
                {"type": "hint", "reason": "学生已列出方程，继续引导代入", "eval": {"correct": true}}
                """;

        ActionMeta meta = objectMapper.readValue(json, ActionMeta.class);

        assertEquals("hint", meta.getType());
        assertEquals(Boolean.TRUE, meta.getEval().getCorrect());
        assertNull(meta.getEval().getErrorType());
    }

    @Test
    @DisplayName("缺省字段有默认值：safety_flag=false、degraded=false")
    void parse_defaults() throws Exception {
        String json = """
                {"type": "concept", "eval": {}}
                """;

        ActionMeta meta = objectMapper.readValue(json, ActionMeta.class);

        assertEquals("concept", meta.getType());
        assertEquals(Boolean.FALSE, meta.getSafetyFlag());
        assertEquals(Boolean.FALSE, meta.getDegraded());
        assertNull(meta.getEval().getCorrect());
        assertTrue(meta.getMasterySignals() == null || meta.getMasterySignals().isEmpty());
    }

    @Test
    @DisplayName("decide 上下文序列化为 snake_case（不含 current_question）")
    void serialize_decideContext_snakeCase() throws Exception {
        DecideContext context = DecideContext.builder()
                .history(List.of(TutoringChatMessage.user("鸡兔同笼，共35头94脚")))
                .roundCount(2)
                .answerRequestCount(1)
                .masterySnapshot(List.of(KpSnapshot.builder().kpKey("http://edukg.org/kp/1").label("二元一次方程组").masteryLevel(50).build()))
                .subjectHint("math")
                .build();

        String json = objectMapper.writeValueAsString(context);

        assertTrue(json.contains("\"round_count\":2"), json);
        assertTrue(json.contains("\"answer_request_count\":1"), json);
        assertTrue(json.contains("\"mastery_snapshot\""), json);
        assertTrue(json.contains("\"subject_hint\":\"math\""), json);
        assertFalse(json.contains("current_question"), "Java 零题目状态：不发送 current_question");
    }
}
