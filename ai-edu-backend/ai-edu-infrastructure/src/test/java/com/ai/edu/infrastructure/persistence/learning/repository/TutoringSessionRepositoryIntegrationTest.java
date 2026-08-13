package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.EndReason;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import com.ai.edu.domain.learning.repository.TutoringSessionRepository;
import com.ai.edu.infrastructure.persistence.learning.mapper.TutoringSessionMapper;
import com.ai.edu.infrastructure.persistence.learning.po.TutoringSessionPo;
import com.ai.edu.infrastructure.test.TutoringInfrastructureConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 答疑会话仓储 H2 集成测试（真实 SQL 跑 H2，learning 数据源）。
 *
 * <p>覆盖任务组 4.1 的 SQL 级语义——单测 mock Mapper 无法验证的部分：
 * 列表的「排除软删 / 按用户隔离 / 倒序」、软删 = 逻辑删（is_deleted=1）后 findById 不可见、
 * 聚合根持久化 round-trip（title/计数/状态）。
 *
 * <p>逻辑删除配置经 {@code @TestPropertySource} 注入本测试上下文，共享 application-h2.yml
 * 及现有 Kg* H2 测试不受影响。learning 表结构由 TutoringInfrastructureConfig 的
 * ApplicationRunner 建在 testdb_learning。
 */
@SpringBootTest(
        classes = TutoringInfrastructureConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("h2")
@TestPropertySource(properties = {
        "mybatis-plus.global-config.db-config.logic-delete-field=deleted",
        "mybatis-plus.global-config.db-config.logic-delete-value=1",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=0"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TutoringSessionRepositoryIntegrationTest {

    private static final long STUDENT_A = 501L;
    private static final long STUDENT_B = 502L;

    @Resource
    private TutoringSessionRepository tutoringSessionRepository;

    @Resource
    private TutoringSessionMapper tutoringSessionMapper;

    /** 每例前逻辑清空：delete(null) → 逻辑删全部可见行（is_deleted=1），新例只见本次插入的 is_deleted=0 行。 */
    @BeforeEach
    void reset() {
        tutoringSessionMapper.delete(null);
    }

    // ==================== save：insert 新会话 ====================

    @Test
    @Order(1)
    @DisplayName("save 新会话：INSERT 生成 id，is_deleted 默认 0")
    void save_new_insertsAndDefaultsVisible() {
        TutoringSession saved = tutoringSessionRepository.save(TutoringSession.start(STUDENT_A, "math"));

        assertNotNull(saved.getId());
        assertTrue(saved.isActive());

        TutoringSessionPo po = tutoringSessionMapper.selectById(saved.getId());
        assertNotNull(po);
        assertEquals(STUDENT_A, po.getStudentId());
        assertEquals("math", po.getSubject());
        assertEquals("ACTIVE", po.getStatus());
        assertFalse(po.getDeleted());
    }

    // ==================== save：update 已存在 ====================

    @Test
    @Order(2)
    @DisplayName("save 已存在会话：UPDATE，title/roundCount 落库并可读回")
    void save_existing_updatesRow() {
        TutoringSession saved = tutoringSessionRepository.save(TutoringSession.start(STUDENT_A, "math"));
        saved.setTitle("鸡兔同笼怎么做");
        saved.recordRound();

        TutoringSession returned = tutoringSessionRepository.save(saved);
        TutoringSession found = tutoringSessionRepository.findById(returned.getId()).orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertEquals("鸡兔同笼怎么做", found.getTitle());
        assertEquals(1, found.getRoundCount());
    }

    // ==================== findById：聚合根复原 ====================

    @Test
    @Order(3)
    @DisplayName("findById：title/round/answerRequest/status/endReason/archivedAt 完整复原")
    void findById_restoresAggregate() {
        TutoringSession session = TutoringSession.start(STUDENT_A, "math");
        session.setTitle("二次函数");
        session.recordRound();
        session.recordRound();
        session.requestAnswer();
        session.complete(EndReason.COMPLETED);   // → ARCHIVED + archivedAt
        TutoringSession saved = tutoringSessionRepository.save(session);

        TutoringSession found = tutoringSessionRepository.findById(saved.getId()).orElseThrow();

        assertEquals(TutoringState.ARCHIVED, found.getStatus());
        assertEquals(2, found.getRoundCount());
        assertEquals(1, found.getAnswerRequestCount());
        assertEquals("二次函数", found.getTitle());
        assertEquals(EndReason.COMPLETED, found.getEndReason());
        assertNotNull(found.getArchivedAt());
        assertFalse(found.isActive());
    }

    // ==================== findListByStudentId：SQL 级语义（4.1 核心缺口） ====================

    @Test
    @Order(4)
    @DisplayName("列表：全状态 + 排除软删 + 按用户隔离 + updated_at 倒序")
    void findListByStudentId_filtersAndOrders() {
        LocalDateTime base = LocalDateTime.of(2026, 8, 1, 10, 0);
        // 学生 A 三个会话，updated_at 递增，状态覆盖 3 态
        TutoringSession a1 = tutoringSessionRepository.save(
                restoredSession(STUDENT_A, "会话A1", TutoringState.ACTIVE, 0, base));
        TutoringSession a2 = tutoringSessionRepository.save(
                restoredSession(STUDENT_A, "会话A2", TutoringState.ARCHIVED, 3, base.plusMinutes(1)));
        TutoringSession a3 = tutoringSessionRepository.save(
                restoredSession(STUDENT_A, "会话A3", TutoringState.TERMINATED, 1, base.plusMinutes(2)));
        // 学生 B 一个会话，updated_at 夹在 A2/A3 之间（验证用户隔离，B 不进 A 列表）
        TutoringSession b1 = tutoringSessionRepository.save(
                restoredSession(STUDENT_B, "会话B1", TutoringState.ACTIVE, 0, base.plusMinutes(1).plusSeconds(30)));

        // 软删 A2（中间时间）
        tutoringSessionRepository.softDelete(a2.getId());

        List<TutoringSession> listA = tutoringSessionRepository.findListByStudentId(STUDENT_A);
        // 全状态 + 排除软删：剩 a3(TERMINATED)、a1(ACTIVE)；倒序：a3 在前
        assertEquals(2, listA.size());
        assertEquals(a3.getId(), listA.get(0).getId());
        assertEquals(a1.getId(), listA.get(1).getId());

        List<TutoringSession> listB = tutoringSessionRepository.findListByStudentId(STUDENT_B);
        assertEquals(1, listB.size());
        assertEquals(b1.getId(), listB.get(0).getId());
    }

    // ==================== softDelete：逻辑删语义 ====================

    @Test
    @Order(5)
    @DisplayName("软删：deleteById → is_deleted=1，findById/selectById/列表均不可见")
    void softDelete_marksDeleted_andExcludesEverywhere() {
        TutoringSession saved = tutoringSessionRepository.save(TutoringSession.start(STUDENT_A, "math"));

        tutoringSessionRepository.softDelete(saved.getId());

        // MyBatis-Plus BaseMapper 自动过滤逻辑删
        assertNull(tutoringSessionMapper.selectById(saved.getId()));
        assertTrue(tutoringSessionRepository.findById(saved.getId()).isEmpty());
        // 原生 @Select 列表同样排除
        assertTrue(tutoringSessionRepository.findListByStudentId(STUDENT_A).isEmpty());
    }

    // ==================== updateTranscriptUrl ====================

    @Test
    @Order(6)
    @DisplayName("updateTranscriptUrl：回填 COS 归档 objectKey")
    void updateTranscriptUrl_fillsColumn() {
        TutoringSession saved = tutoringSessionRepository.save(TutoringSession.start(STUDENT_A, "math"));

        tutoringSessionRepository.updateTranscriptUrl(saved.getId(), "tutoring/transcripts/501/1.json");

        TutoringSessionPo po = tutoringSessionMapper.selectById(saved.getId());
        assertNotNull(po);
        assertEquals("tutoring/transcripts/501/1.json", po.getTranscriptUrl());
    }

    // ==================== findActiveByStudentId ====================

    @Test
    @Order(7)
    @DisplayName("活跃会话：仅返回 ACTIVE（ARCHIVED 排除）")
    void findActiveByStudentId_onlyActive() {
        TutoringSession active = tutoringSessionRepository.save(TutoringSession.start(STUDENT_A, "math"));
        TutoringSession archived = TutoringSession.start(STUDENT_A, "math");
        archived.complete(EndReason.ABANDONED);
        tutoringSessionRepository.save(archived);

        List<TutoringSession> actives = tutoringSessionRepository.findActiveByStudentId(STUDENT_A);

        assertEquals(1, actives.size());
        assertEquals(active.getId(), actives.get(0).getId());
    }

    // ==================== helpers ====================

    /** 构造指定状态/计数/更新时间的会话（restore 工厂，绕过领域状态机便于直接测持久化语义）。 */
    private TutoringSession restoredSession(Long studentId, String title, TutoringState status,
                                            int roundCount, LocalDateTime updatedAt) {
        return TutoringSession.restore(null, studentId, "math", title,
                null, null, null, null, status, roundCount, 0,
                null, null, null, updatedAt, null);
    }
}
