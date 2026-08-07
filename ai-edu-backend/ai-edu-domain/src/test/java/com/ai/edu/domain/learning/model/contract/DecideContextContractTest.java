package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java → Python decide 请求契约测试（snake_case 序列化：换题信号 is_new_question、消息 image_url）。
 */
class DecideContextContractTest {

    /** 域模块未引 jsr310 依赖，LocalDateTime 用 ToString 序列化即可（测试只断言键名）。 */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new SimpleModule().addSerializer(LocalDateTime.class, ToStringSerializer.instance));

    @Test
    @DisplayName("decide 请求序列化：is_new_question + history 消息 image_url，零 current_question")
    void serialize_isNewQuestionAndImageUrl() throws Exception {
        DecideContext ctx = DecideContext.builder()
                .history(List.of(
                        TutoringChatMessage.userWithImage("", "https://cos/tutoring/questions/501/1001/abc.jpg"),
                        TutoringChatMessage.user("设鸡x只"),
                        TutoringChatMessage.ai("先找已知条件")))
                .roundCount(3)
                .answerRequestCount(0)
                .masterySnapshot(List.of())
                .subjectHint("math")
                .isNewQuestion(true)
                .build();

        String json = objectMapper.writeValueAsString(ctx);

        assertTrue(json.contains("\"is_new_question\":true"), "换题信号应序列化为 is_new_question: " + json);
        assertTrue(json.contains("\"image_url\":\"https://cos/tutoring/questions/501/1001/abc.jpg\""),
                "图片消息应序列化为 image_url: " + json);
        assertTrue(json.contains("\"round_count\":3"), json);
        assertFalse(json.contains("current_question"), "Java 零题目状态，不得出现 current_question: " + json);
    }

    @Test
    @DisplayName("缺省 is_new_question=false：不传信号 = 正常轮，向后兼容")
    void serialize_defaultIsNewQuestionFalse() throws Exception {
        DecideContext ctx = DecideContext.builder()
                .history(List.of(TutoringChatMessage.user("鸡兔同笼")))
                .roundCount(1)
                .answerRequestCount(0)
                .masterySnapshot(List.of())
                .subjectHint("math")
                .build();

        String json = objectMapper.writeValueAsString(ctx);

        assertTrue(json.contains("\"is_new_question\":false"), json);
    }
}
