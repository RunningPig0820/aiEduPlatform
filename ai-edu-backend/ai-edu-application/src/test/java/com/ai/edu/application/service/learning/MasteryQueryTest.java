package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.MasteryQueryRequest;
import com.ai.edu.application.dto.learning.StudentMasteryDTO;
import com.ai.edu.application.dto.learning.StudentTopicQuestionsDTO;
import com.ai.edu.domain.learning.model.entity.StudentQuestionRecord;
import com.ai.edu.domain.learning.model.entity.StudentTopicMastery;
import com.ai.edu.domain.learning.model.valueobject.MasteryLevel;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 掌握度分页查询测试（tasks 4.1 分页改造，MST-001~007 / QST-001~002）。
 *
 * <p>MST：POST 分页查询——默认 updatedAt 倒序 + status 分桶筛选（consolidate&lt;25 / learning 25-50 /
 * steady 50-75 / mastered ≥75）+ keyword 题型名模糊 + sortBy 切 masteryLevel 排序 + 内存分页；
 * 掌握表无行 → total=0 空列表。QST：按题型查题目（含 score/session_id），空态返回空数组。
 */
class MasteryQueryTest {

    private static final Long STUDENT_ID = 1001L;

    // 6 个题型：level 覆盖四档，updatedAt 递减（最新在前）
    private static final LocalDateTime T_方程 = LocalDateTime.of(2026, 8, 18, 10, 0);  // 一元二次方程 64
    private static final LocalDateTime T_鸡兔 = LocalDateTime.of(2026, 8, 17, 9, 0);   // 鸡兔同笼 30
    private static final LocalDateTime T_相遇 = LocalDateTime.of(2026, 8, 16, 8, 0);   // 相遇问题 80
    private static final LocalDateTime T_假设 = LocalDateTime.of(2026, 8, 15, 7, 0);   // 假设法 45
    private static final LocalDateTime T_植树 = LocalDateTime.of(2026, 8, 14, 6, 0);   // 植树问题 20
    private static final LocalDateTime T_行程 = LocalDateTime.of(2026, 8, 13, 5, 0);   // 行程问题 90

    private TutoringAppService service;
    private StudentTopicMasteryRepository studentTopicMasteryRepository;
    private StudentQuestionRecordRepository questionRecordRepository;

    @BeforeEach
    void setUp() {
        studentTopicMasteryRepository = mock(StudentTopicMasteryRepository.class);
        questionRecordRepository = mock(StudentQuestionRecordRepository.class);
        service = new TutoringAppService();
        service.setStudentTopicMasteryRepository(studentTopicMasteryRepository);
        service.setQuestionRecordRepository(questionRecordRepository);
        when(studentTopicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of());
    }

    private List<StudentTopicMastery> all() {
        return List.of(
                mastery(1L, "一元二次方程", 64, T_方程),
                mastery(2L, "鸡兔同笼", 30, T_鸡兔),
                mastery(3L, "相遇问题", 80, T_相遇),
                mastery(4L, "假设法", 45, T_假设),
                mastery(5L, "植树问题", 20, T_植树),
                mastery(6L, "行程问题", 90, T_行程));
    }

    private StudentTopicMastery mastery(long id, String label, int level, LocalDateTime updatedAt) {
        return StudentTopicMastery.restore(id, STUDENT_ID, TopicKey.of(label), label,
                MasteryLevel.of(level), "ai", 10, updatedAt);
    }

    private MasteryQueryRequest req(Integer pageNum, Integer pageSize, String keyword, String status,
                                    String sortBy, String order) {
        return MasteryQueryRequest.builder().pageNum(pageNum).pageSize(pageSize).keyword(keyword)
                .masteryStatus(status).sortBy(sortBy).order(order).build();
    }

    // ---------- MST ----------

    @Test
    @DisplayName("MST-001: 默认排序 updatedAt 倒序 + 分页（pageSize=2 → 最新两题）")
    void mastery_defaultSortAndPage() {
        when(studentTopicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(all());

        StudentMasteryDTO dto = service.queryStudentMastery(STUDENT_ID, req(1, 2, null, null, null, null));

        assertEquals(2, dto.getItems().size());
        assertEquals(6, dto.getTotal());
        assertEquals(1, dto.getPageNum());
        assertEquals(2, dto.getPageSize());
        assertEquals("一元二次方程", dto.getItems().get(0).getTopicLabel(), "默认 updatedAt desc 最新在前");
        assertEquals("鸡兔同笼", dto.getItems().get(1).getTopicLabel());
    }

    @Test
    @DisplayName("MST-002: 分页切页（pageNum=2 → 下一页两条）")
    void mastery_pageNext() {
        when(studentTopicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(all());

        StudentMasteryDTO dto = service.queryStudentMastery(STUDENT_ID, req(2, 2, null, null, null, null));

        assertEquals(2, dto.getItems().size());
        assertEquals("相遇问题", dto.getItems().get(0).getTopicLabel());
        assertEquals("假设法", dto.getItems().get(1).getTopicLabel());
    }

    @Test
    @DisplayName("MST-003: status=learning（25≤x<50）→ 练习中两题")
    void mastery_filterLearning() {
        when(studentTopicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(all());

        StudentMasteryDTO dto = service.queryStudentMastery(STUDENT_ID, req(null, 100, null, "learning", null, null));

        assertEquals(2, dto.getTotal());
        assertTrue(dto.getItems().stream().map(i -> i.getTopicLabel()).toList()
                .containsAll(List.of("鸡兔同笼", "假设法")));
    }

    @Test
    @DisplayName("MST-004: status=consolidate（<25）→ 待巩固一题")
    void mastery_filterConsolidate() {
        when(studentTopicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(all());

        StudentMasteryDTO dto = service.queryStudentMastery(STUDENT_ID, req(null, 100, null, "consolidate", null, null));

        assertEquals(1, dto.getTotal());
        assertEquals("植树问题", dto.getItems().get(0).getTopicLabel());
    }

    @Test
    @DisplayName("MST-005: keyword=方程 模糊 → 一元二次方程")
    void mastery_filterKeyword() {
        when(studentTopicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(all());

        StudentMasteryDTO dto = service.queryStudentMastery(STUDENT_ID, req(null, 100, "方程", null, null, null));

        assertEquals(1, dto.getTotal());
        assertEquals("一元二次方程", dto.getItems().get(0).getTopicLabel());
    }

    @Test
    @DisplayName("MST-006: sortBy=masteryLevel asc → 从最薄弱到最强")
    void mastery_sortByMasteryLevel() {
        when(studentTopicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(all());

        StudentMasteryDTO dto = service.queryStudentMastery(STUDENT_ID, req(null, 100, null, null, "masteryLevel", "asc"));

        assertEquals(List.of("植树问题", "鸡兔同笼", "假设法", "一元二次方程", "相遇问题", "行程问题"),
                dto.getItems().stream().map(i -> i.getTopicLabel()).toList());
    }

    @Test
    @DisplayName("MST-007: 未开始（掌握表无行）→ total=0 空列表")
    void mastery_notStarted_absent() {
        StudentMasteryDTO dto = service.queryStudentMastery(STUDENT_ID, req(1, 20, null, null, null, null));
        assertTrue(dto.getItems().isEmpty());
        assertEquals(0, dto.getTotal());
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
                score, 0, 0, sessionId, LocalDateTime.of(2026, 8, 18, 10, 0));
    }
}
