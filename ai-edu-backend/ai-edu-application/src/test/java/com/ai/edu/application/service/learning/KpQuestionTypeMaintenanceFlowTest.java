package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.QuestionAnalysisDTO;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeAlias;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.repository.QuestionTypeAliasRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.service.QuestionUnderstandingPort;
import com.ai.edu.domain.learning.service.TutoringKpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 域 B 独立化闭环验证（tasks 2.0.7③）：独立维护接口写入 → 入口 analyze 查表命中 → 权威分布。
 *
 * <p>用共享内存存储桩联通「维护 service 写入」与「分析 service 读取」，验证编排层闭环：
 * 维护配「鸡兔同笼 + 分布 + 别名」→ analyze 识别「鸡兔同笼问题」→ findByTopicLabelOrAlias 命中
 * canonical → 返回权威分布（不再依赖 obs 共现自动聚合）。
 */
class KpQuestionTypeMaintenanceFlowTest {

    private static final Long STUDENT_ID = 1001L;

    private QuestionTypeRepository qtRepo;
    private QuestionTypeKpRepository qtKpRepo;
    private QuestionTypeAliasRepository aliasRepo;
    private KgKnowledgePointRepository kgRepo;

    // 共享内存存储（维护写 → 入口读）
    private final Map<String, QuestionType> byLabel = new HashMap<>();
    private final Map<Long, QuestionType> byId = new HashMap<>();
    private final Map<String, Long> aliasToTypeId = new HashMap<>();
    private final Map<Long, List<QuestionTypeKp>> kpsByType = new HashMap<>();

    @BeforeEach
    void setUp() {
        qtRepo = mock(QuestionTypeRepository.class);
        qtKpRepo = mock(QuestionTypeKpRepository.class);
        aliasRepo = mock(QuestionTypeAliasRepository.class);
        kgRepo = mock(KgKnowledgePointRepository.class);

        // 维护读路径
        when(qtRepo.findByTopicLabel(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(byLabel.get(inv.getArgument(0))));
        when(qtRepo.findById(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(byId.get(inv.getArgument(0))));
        when(qtRepo.upsert(any())).thenAnswer(inv -> {
            QuestionType qt = inv.getArgument(0);
            if (qt.getId() == null) {
                qt.setId(1L); // 模拟 DB 主键回填
            }
            byLabel.put(qt.getTopicLabel(), qt);
            byId.put(qt.getId(), qt);
            return qt;
        });
        // 入口读路径（canonical 直接命中 / 别名 → canonical）
        when(qtRepo.findByTopicLabelOrAlias(anyString())).thenAnswer(inv -> {
            String label = inv.getArgument(0);
            QuestionType direct = byLabel.get(label);
            if (direct != null) {
                return Optional.of(direct);
            }
            Long typeId = aliasToTypeId.get(label);
            return typeId == null ? Optional.empty() : Optional.ofNullable(byId.get(typeId));
        });
        // 维护写分布桶 + 入口读分布
        when(qtKpRepo.upsert(any())).thenAnswer(inv -> {
            QuestionTypeKp kp = inv.getArgument(0);
            kpsByType.computeIfAbsent(kp.getQuestionTypeId(), k -> new ArrayList<>()).add(kp);
            return kp;
        });
        when(qtKpRepo.findByQuestionTypeId(anyLong()))
                .thenAnswer(inv -> kpsByType.getOrDefault(inv.getArgument(0), List.of()));
        // 维护写别名
        when(aliasRepo.upsert(any())).thenAnswer(inv -> {
            QuestionTypeAlias alias = inv.getArgument(0);
            aliasToTypeId.put(alias.getAliasLabel(), alias.getQuestionTypeId());
            return alias;
        });
        // kg 镜像反查（catalogResult 的 kpLabel）
        when(kgRepo.findByUri(anyString()))
                .thenAnswer(inv -> Optional.of(KgKnowledgePoint.create(inv.getArgument(0), inv.getArgument(0))));
    }

    @Test
    @DisplayName("闭环：维护配题型+分布+别名 → analyze 入口命中返回权威分布（不依赖自动聚合）")
    void maintenanceWrite_thenAnalyzeHits() {
        KpQuestionTypeMaintenanceAppService maintenance = new KpQuestionTypeMaintenanceAppService();
        maintenance.setQuestionTypeRepository(qtRepo);
        maintenance.setQuestionTypeKpRepository(qtKpRepo);
        maintenance.setQuestionTypeAliasRepository(aliasRepo);

        // ① 独立维护：建题型 + 绑分布 + 加别名
        QuestionType canonical = maintenance.upsertType("鸡兔同笼");
        maintenance.bindKp(canonical.getId(), "uri-1", 0.6, "4-6");
        maintenance.addAlias(canonical.getId(), "鸡兔同笼问题");

        // ② 入口 analyze：识别「鸡兔同笼问题」→ 别名命中 canonical → 权威分布
        QuestionUnderstandingPort understandingPort = mock(QuestionUnderstandingPort.class);
        when(understandingPort.understand(anyString(), any())).thenReturn(List.of("鸡兔同笼问题"));
        TutoringKpResolver kpResolver = mock(TutoringKpResolver.class);

        KpQuestionAnalysisAppService analysis = new KpQuestionAnalysisAppService();
        inject(analysis, "questionUnderstandingPort", understandingPort);
        inject(analysis, "tutoringKpResolver", kpResolver);
        inject(analysis, "questionTypeRepository", qtRepo);
        inject(analysis, "questionTypeKpRepository", qtKpRepo);
        inject(analysis, "kgKnowledgePointRepository", kgRepo);

        QuestionAnalysisDTO dto = analysis.analyze("笼子里有鸡和兔", STUDENT_ID);

        assertEquals("RESOLVED", dto.getStatus());
        assertEquals("鸡兔同笼", dto.getTopicLabel(), "返回 canonical 而非变体名");
        assertEquals(1, dto.getKnowledgePoints().size());
        assertEquals("uri-1", dto.getKnowledgePoints().get(0).getKpUri());
        assertEquals(0.6, dto.getKnowledgePoints().get(0).getRatio());
        assertEquals(60, dto.getConfidence(), "confidence = max ratio × 100");
        assertTrue(byLabel.containsKey("鸡兔同笼"), "维护数据已落库，非自动聚合产生");
    }

    private void inject(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
