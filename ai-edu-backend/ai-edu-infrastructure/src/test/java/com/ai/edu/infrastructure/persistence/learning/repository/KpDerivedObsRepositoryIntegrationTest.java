package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpSource;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;
import com.ai.edu.domain.learning.model.valueobject.QuestionTypeStatus;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.infrastructure.persistence.learning.mapper.DerivedKpObsMapper;
import com.ai.edu.infrastructure.test.TutoringInfrastructureConfig;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 知识点派生层仓储 H2 集成测试（真实 SQL 跑 H2，learning 数据源）。
 *
 * <p>覆盖单测 mock 无法验证的 SQL 级语义：UNIQUE 去重（occurrence_count 递增）、
 * PENDING（kp_uri 为空）去重、confirm 落库、去重学生计数、题型/分布桶 UPSERT。
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
class KpDerivedObsRepositoryIntegrationTest {

    private static final Long STUDENT_A = 501L;
    private static final Long STUDENT_B = 502L;
    private static final String KP_URI = "http://edukg.org/knowledge/3.1/kp/math#jsfa";

    @Resource
    private DerivedKpObsRepository obsRepository;
    @Resource
    private QuestionTypeRepository questionTypeRepository;
    @Resource
    private QuestionTypeKpRepository questionTypeKpRepository;
    @Resource
    private DerivedKpObsMapper obsMapper;
    @Resource
    private DataSource dataSource;

    @BeforeEach
    void reset() throws Exception {
        // 派生表有 UNIQUE 约束，逻辑删会挡重新插入，故物理清空（路由到 learning 库）
        DynamicDataSourceContextHolder.push("learning");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM t_kp_question_type_kp");
            stmt.execute("DELETE FROM t_kp_question_type");
            stmt.execute("DELETE FROM t_kp_derived_obs");
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

    @Test
    @DisplayName("upsert：同生同题型同 kp 去重，occurrence_count 1→2，仅一行")
    void upsert_dedupByUniqueKey() {
        DerivedKpObs first = obs(1L, KP_URI, DerivedKpStatus.RESOLVED);
        obsRepository.upsert(first);
        obsRepository.upsert(obs(1L, KP_URI, DerivedKpStatus.RESOLVED));

        List<DerivedKpObs> all = obsRepository.findByStudentId(1L);
        assertEquals(1, all.size());
        assertEquals(2, all.get(0).getOccurrenceCount());
    }

    @Test
    @DisplayName("PENDING 去重：kp_uri 为空不参与 UNIQUE，先查后插，仅一行")
    void upsert_pendingDedup() {
        obsRepository.upsert(obs(1L, null, DerivedKpStatus.PENDING));
        obsRepository.upsert(obs(1L, null, DerivedKpStatus.PENDING));

        List<DerivedKpObs> pending = obsRepository.findByStatus(DerivedKpStatus.PENDING);
        assertEquals(1, pending.size());
        assertEquals(2, pending.get(0).getOccurrenceCount());
    }

    @Test
    @DisplayName("findResolved：只返回 kp_uri 非空的观测")
    void findResolved_excludesPending() {
        obsRepository.upsert(obs(1L, KP_URI, DerivedKpStatus.RESOLVED));
        obsRepository.upsert(obs(1L, null, DerivedKpStatus.PENDING));

        List<DerivedKpObs> resolved = obsRepository.findResolved();
        assertEquals(1, resolved.size());
        assertEquals(KP_URI, resolved.get(0).getKpUri());
    }

    @Test
    @DisplayName("confirm：更新 kp_uri + source=curated + status=RESOLVED")
    void confirm_updatesResolution() {
        DerivedKpObs saved = obsRepository.upsert(obs(1L, null, DerivedKpStatus.PENDING));

        int updated = obsRepository.confirm(saved.getId(), KP_URI);

        assertEquals(1, updated);
        List<DerivedKpObs> resolved = obsRepository.findByStatus(DerivedKpStatus.RESOLVED);
        assertEquals(1, resolved.size());
        assertEquals(KP_URI, resolved.get(0).getKpUri());
    }

    @Test
    @DisplayName("countDistinctStudents：同题型同 kp 去重学生数")
    void countDistinctStudents_dedup() {
        obsRepository.upsert(obs(1L, KP_URI, DerivedKpStatus.RESOLVED));
        obsRepository.upsert(obs(2L, KP_URI, DerivedKpStatus.RESOLVED));
        obsRepository.upsert(obs(2L, KP_URI, DerivedKpStatus.RESOLVED)); // 同生重复

        assertEquals(2, obsRepository.countDistinctStudentsByTopicAndKp("鸡兔同笼", KP_URI));
    }

    @Test
    @DisplayName("题型 + 分布桶 UPSERT：topic_label 唯一 + 分布桶同 kp 唯一")
    void questionTypeUpsert() {
        QuestionType qt = QuestionType.create("鸡兔同笼", QuestionTypeStatus.CANDIDATE, STUDENT_A);
        qt.updateStats(3, 5);
        QuestionType savedQt = questionTypeRepository.upsert(qt);
        assertNotNull(savedQt.getId());

        QuestionTypeKp kp = QuestionTypeKp.create(savedQt.getId(), KP_URI, "4-6");
        kp.updateStats(3, 5, 0.6, "4-6");
        QuestionTypeKp savedKp = questionTypeKpRepository.upsert(kp);
        assertNotNull(savedKp.getId());

        Optional<QuestionType> foundQt = questionTypeRepository.findByTopicLabel("鸡兔同笼");
        assertTrue(foundQt.isPresent());
        assertEquals(3, foundQt.get().getHitStudents());

        List<QuestionTypeKp> buckets = questionTypeKpRepository.findByQuestionTypeId(savedQt.getId());
        assertEquals(1, buckets.size());
        assertEquals(0.6, buckets.get(0).getRatio(), 0.001);
    }

    private DerivedKpObs obs(Long studentId, String kpUri, DerivedKpStatus status) {
        return DerivedKpObs.create(studentId, "鸡兔同笼", kpUri, 4, 80, DerivedKpSource.LLM, status);
    }
}
