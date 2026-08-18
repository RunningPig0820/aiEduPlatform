package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.StudentMasteryDTO;
import com.ai.edu.application.dto.learning.StudentTopicQuestionsDTO;
import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.entity.StudentQuestionRecord;
import com.ai.edu.domain.learning.model.entity.StudentTopicMastery;
import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.StudentQuestionRecordRepository;
import com.ai.edu.domain.learning.repository.StudentTopicMasteryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 掌握度查询测试（tasks 4.1/4.2，test.md MST-001~003 / QST-001~002）。
 *
 * <p>MST：masteryLevel 连续百分比 + source + trainCount；未开始不出现；PENDING = 题目记录有但 canonical
 * 未归属（域 B 独立化 Decision 10，不再来自 obs）。QST：按题型查题目（含 score/session_id），空态返回空数组。
 */
class MasteryQueryTest {

    private static final Long STUDENT_ID = 1001L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 18, 10, 0);

    private TutoringAppService service;
    private StudentTopicMasteryRepository studentTopicMasteryRepository;
    private DerivedKpObsRepository derivedKpObsRepository;
    private StudentQuestionRecordRepository questionRecordRepository;

    @BeforeEach
    void setUp() {
        studentTopicMasteryRepository = mock(StudentTopicMasteryRepository.class);
        derivedKpObsRepository = mock(DerivedKpObsRepository.class);
        questionRecordRepository = mock(StudentQuestionRecordRepository.class);
        service = new TutoringAppService();
        service.setStudentTopicMasteryRepository(studentTopicMasteryRepository);
        service.setDerivedKpObsRepository(derivedKpObsRepository);
        service.setQuestionRecordRepository(questionRecordRepository);
        when(studentTopicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of());
        when(derivedKpObsRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of());
        when(questionRecordRepository.findPendingTopicLabelsByStudent(STUDENT_ID)).thenReturn(List.of());
    }

    private StudentTopicMastery mastery(long trainCount, int level, String source) {
        return StudentTopicMastery.restore(1L, STUDENT_ID, TopicKey.of("一元二次方程"), "一元二次方程",
                MasteryLevel.of(level), null, 888L, source, trainCount, NOW);
    }

    // ---------- MST ----------

    @Test
    @DisplayName("MST-001: 连续百分比 + source + trainCount（练 10 题对 6 题 = 64%）")
    void mastery_continuousPercentWithSourceAndTrainCount() {
        when(studentTopicMasteryRepository.findByStudentId(STUDENT_ID))
                .thenReturn(List.of(mastery(10, 64, "ai")));

        StudentMasteryDTO dto = service.getStudentMastery(STUDENT_ID);

        assertEquals(1, dto.getItems().size());
        assertEquals(64, dto.getItems().get(0).getMasteryLevel(), "连续百分比");
        assertEquals("ai", dto.getItems().get(0).getSource());
        assertEquals(10, dto.getItems().get(0).getTrainCount());
        assertEquals("RESOLVED", dto.getItems().get(0).getStatus());
        assertEquals("一元二次方程", dto.getItems().get(0).getTopicLabel());
    }

    @Test
    @DisplayName("MST-002: 未开始题型不出现（无掌握记录 + 无 PENDING 题目 → items 空）")
    void mastery_notStarted_absent() {
        StudentMasteryDTO dto = service.getStudentMastery(STUDENT_ID);
        assertTrue(dto.getItems().isEmpty(), "未开始题型不在 items[]");
    }

    @Test
    @DisplayName("MST-003: PENDING 项 = 题目记录有但 canonical 未归属（masteryLevel=0）")
    void mastery_pendingFromQuestionRecords() {
        when(questionRecordRepository.findPendingTopicLabelsByStudent(STUDENT_ID))
                .thenReturn(List.of("鸡兔同笼问题"));

        StudentMasteryDTO dto = service.getStudentMastery(STUDENT_ID);

        assertEquals(1, dto.getItems().size());
        assertEquals("PENDING", dto.getItems().get(0).getStatus());
        assertEquals(0, dto.getItems().get(0).getMasteryLevel());
        assertEquals("鸡兔同笼问题", dto.getItems().get(0).getTopicLabel());
    }

    // ---------- QST ----------

    @Test
    @DisplayName("QST-001: 按题型查题目列表（content/score/session_id/时间，含包装 studentId/topicLabel）")
    void questions_byTopic() {
        when(questionRecordRepository.findByStudentAndCanonical(STUDENT_ID, "一元二次方程")).thenReturn(List.of(
                record("题目1", new BigDecimal("0.70"), 888L),
                record("题目2", new BigDecimal("1.00"), 888L),
                record("题目3", new BigDecimal("0.00"), 888L)));

        StudentTopicQuestionsDTO dto = service.getStudentTopicQuestions(STUDENT_ID, "一元二次方程");

        assertEquals(3, dto.getQuestions().size());
        assertEquals(STUDENT_ID, dto.getStudentId());
        assertEquals("一元二次方程", dto.getTopicLabel());
        assertEquals("题目1", dto.getQuestions().get(0).getContent());
        assertEquals(0, new BigDecimal("0.70").compareTo(dto.getQuestions().get(0).getScore()));
        assertEquals(888L, dto.getQuestions().get(0).getSessionId());
        assertEquals("ai", dto.getQuestions().get(0).getSource());
    }

    @Test
    @DisplayName("QST-002: 空态 → 空数组不报错")
    void questions_empty() {
        when(questionRecordRepository.findByStudentAndCanonical(STUDENT_ID, "未练题型"))
                .thenReturn(List.of());

        StudentTopicQuestionsDTO dto = service.getStudentTopicQuestions(STUDENT_ID, "未练题型");

        assertTrue(dto.getQuestions().isEmpty());
    }

    private StudentQuestionRecord record(String content, BigDecimal score, Long sessionId) {
        return StudentQuestionRecord.create("ai", STUDENT_ID, content, "一元二次方程", "一元二次方程",
                score, 0, 0, sessionId, NOW);
    }
}
