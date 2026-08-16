package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.KpCoverageDTO;
import com.ai.edu.application.dto.learning.StudentMasteryDTO;
import com.ai.edu.application.service.learning.KpCoverageAppService;
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
import static org.mockito.Mockito.*;

/**
 * StudentMasteryController 单元测试（mock AppService + MockHttpSession）。
 */
class StudentMasteryControllerTest {

    private static final Long STUDENT_ID = 501L;

    private StudentMasteryController controller;
    private TutoringAppService appService;
    private KpCoverageAppService kpCoverageAppService;

    @BeforeEach
    void setUp() {
        appService = mock(TutoringAppService.class);
        kpCoverageAppService = mock(KpCoverageAppService.class);
        controller = new StudentMasteryController();
        setField(controller, "tutoringAppService", appService);
        setField(controller, "kpCoverageAppService", kpCoverageAppService);
    }

    @Test
    @DisplayName("getMastery：路径 studentId 与会话一致 → 返回掌握度")
    void getMastery_success() {
        when(appService.getStudentMastery(STUDENT_ID))
                .thenReturn(StudentMasteryDTO.builder().studentId(STUDENT_ID).items(List.of()).build());

        ApiResponse<StudentMasteryDTO> response = controller.getMastery(STUDENT_ID, loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals(STUDENT_ID, response.getData().getStudentId());
        verify(appService).getStudentMastery(STUDENT_ID);
    }

    @Test
    @DisplayName("getMastery：路径与他人 → 抛 20004 越权")
    void getMastery_crossStudentDenied() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.getMastery(999L, loginSession()));
        assertEquals(ErrorCode.PERMISSION_DENIED, ex.getCode());
        verify(appService, never()).getStudentMastery(anyLong());
    }

    @Test
    @DisplayName("getMastery：未登录 → 抛 10004")
    void getMastery_notLoggedIn() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.getMastery(STUDENT_ID, new MockHttpSession()));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    @Test
    @DisplayName("getKpCoverage：路径 studentId 与会话一致 → 返回覆盖度")
    void getKpCoverage_success() {
        when(kpCoverageAppService.getKpCoverage(STUDENT_ID))
                .thenReturn(KpCoverageDTO.builder().studentId(STUDENT_ID).items(List.of()).build());

        ApiResponse<KpCoverageDTO> response = controller.getKpCoverage(STUDENT_ID, loginSession());

        assertEquals(ErrorCode.SUCCESS, response.getCode());
        assertEquals(STUDENT_ID, response.getData().getStudentId());
        verify(kpCoverageAppService).getKpCoverage(STUDENT_ID);
    }

    @Test
    @DisplayName("getKpCoverage：路径与他人 → 抛 20004 越权")
    void getKpCoverage_crossStudentDenied() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.getKpCoverage(999L, loginSession()));
        assertEquals(ErrorCode.PERMISSION_DENIED, ex.getCode());
        verify(kpCoverageAppService, never()).getKpCoverage(anyLong());
    }

    @Test
    @DisplayName("getKpCoverage：未登录 → 抛 10004")
    void getKpCoverage_notLoggedIn() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.getKpCoverage(STUDENT_ID, new MockHttpSession()));
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
            var field = StudentMasteryController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
