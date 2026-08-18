package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.MasteryQueryRequest;
import com.ai.edu.application.dto.learning.StudentMasteryDTO;
import com.ai.edu.application.dto.learning.StudentTopicQuestionsDTO;
import com.ai.edu.application.service.learning.TutoringAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 掌握度/按题型查题目 controller 测试（tasks 4.4，test.md MST-004/005 + QST-003）。
 *
 * <p>身份以会话为准：未登录 → 10004；路径 studentId ≠ 会话 userId → 10005（服务端不信任客户端身份）。
 */
class StudentMasteryControllerTest {

    private static final Long STUDENT_ID = 1001L;

    private StudentMasteryController controller;
    private TutoringAppService tutoringAppService;

    @BeforeEach
    void setUp() {
        tutoringAppService = mock(TutoringAppService.class);
        controller = new StudentMasteryController();
        setField(controller, "tutoringAppService", tutoringAppService);
    }

    // ---------- queryMastery ----------

    @Test
    @DisplayName("MST-001 正常：登录学生 POST 分页查自己掌握度 → 返回分页 items")
    void queryMastery_success() {
        StudentMasteryDTO dto = StudentMasteryDTO.builder().studentId(STUDENT_ID).items(List.of())
                .total(0).pageNum(1).pageSize(20).build();
        MasteryQueryRequest request = MasteryQueryRequest.builder().pageNum(1).pageSize(20).build();
        when(tutoringAppService.queryStudentMastery(STUDENT_ID, request)).thenReturn(dto);

        ApiResponse<StudentMasteryDTO> response = controller.queryMastery(STUDENT_ID, request, loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals(STUDENT_ID, response.getData().getStudentId());
        assertEquals(1, response.getData().getPageNum());
        verify(tutoringAppService).queryStudentMastery(STUDENT_ID, request);
    }

    @Test
    @DisplayName("MST-005 未登录：无会话 → 10004")
    void queryMastery_notLoggedIn() {
        MasteryQueryRequest request = MasteryQueryRequest.builder().build();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.queryMastery(STUDENT_ID, request, new MockHttpSession()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    @Test
    @DisplayName("MST-004 越权：会话 userId ≠ 路径 studentId → 10005")
    void queryMastery_forbidden() {
        MockHttpSession other = loginSession();
        other.setAttribute("userId", 2002L); // 另一个学生

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.queryMastery(STUDENT_ID, MasteryQueryRequest.builder().build(), other));
        assertEquals(ErrorCode.PERMISSION_DENIED, ex.getCode());
    }

    // ---------- getTopicQuestions ----------

    @Test
    @DisplayName("QST-001 正常：登录学生查自己题型题目 → 返回 questions")
    void getTopicQuestions_success() {
        StudentTopicQuestionsDTO dto = StudentTopicQuestionsDTO.builder()
                .studentId(STUDENT_ID).topicLabel("鸡兔同笼").questions(List.of()).build();
        when(tutoringAppService.getStudentTopicQuestions(STUDENT_ID, "鸡兔同笼")).thenReturn(dto);

        ApiResponse<StudentTopicQuestionsDTO> response = controller.getTopicQuestions(STUDENT_ID, "鸡兔同笼", loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals("鸡兔同笼", response.getData().getTopicLabel());
        verify(tutoringAppService).getStudentTopicQuestions(STUDENT_ID, "鸡兔同笼");
    }

    @Test
    @DisplayName("QST-003 越权：会话 userId ≠ 路径 studentId → 10005")
    void getTopicQuestions_forbidden() {
        MockHttpSession other = loginSession();
        other.setAttribute("userId", 2002L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.getTopicQuestions(STUDENT_ID, "鸡兔同笼", other));
        assertEquals(ErrorCode.PERMISSION_DENIED, ex.getCode());
    }

    @Test
    @DisplayName("QST-004 未登录：无会话 → 10004")
    void getTopicQuestions_notLoggedIn() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.getTopicQuestions(STUDENT_ID, "鸡兔同笼", new MockHttpSession()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    private MockHttpSession loginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", STUDENT_ID);
        session.setAttribute("role", "STUDENT");
        return session;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
