package com.ai.edu.application.service.learning;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeAlias;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.repository.QuestionTypeAliasRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
 * 域 B 独立化维护接口持久化闭环验证（tasks 2.0.5）：独立维护接口写入 → 题型库可查（canonical/别名/分布）。
 *
 * <p>题型分析（analyze）已简化为纯 Python 直通小工具（不再消费题型库），维护接口独立生效：
 * 维护配「鸡兔同笼 + 分布 + 别名」→ 题型库 findByTopicLabel/findByTopicLabelOrAlias 可查 → 分布可读。
 */
class KpQuestionTypeMaintenanceFlowTest {

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
        // kg 镜像反查
        when(kgRepo.findByUri(anyString()))
                .thenAnswer(inv -> Optional.of(KgKnowledgePoint.create(inv.getArgument(0), inv.getArgument(0))));
    }

    @Test
    @DisplayName("维护接口持久化闭环：写题型+分布+别名 → 题型库可查（独立于 analyze）")
    void maintenanceWrite_persistsAndQueryable() {
        KpQuestionTypeMaintenanceAppService maintenance = new KpQuestionTypeMaintenanceAppService();
        maintenance.setQuestionTypeRepository(qtRepo);
        maintenance.setQuestionTypeKpRepository(qtKpRepo);
        maintenance.setQuestionTypeAliasRepository(aliasRepo);

        // ① 独立维护：建题型 + 绑分布 + 加别名
        QuestionType canonical = maintenance.upsertType("鸡兔同笼");
        maintenance.bindKp(canonical.getId(), "uri-1", 0.6, "4-6");
        maintenance.addAlias(canonical.getId(), "鸡兔同笼问题");

        // ② 题型库可查：canonical + 别名命中 + 分布可读（知识点覆盖率派生的桥）
        assertEquals("鸡兔同笼", qtRepo.findByTopicLabel("鸡兔同笼").orElseThrow().getTopicLabel());
        assertEquals("鸡兔同笼", qtRepo.findByTopicLabelOrAlias("鸡兔同笼问题").orElseThrow().getTopicLabel());
        List<QuestionTypeKp> kps = qtKpRepo.findByQuestionTypeId(canonical.getId());
        assertEquals(1, kps.size());
        assertEquals("uri-1", kps.get(0).getKpUri());
        assertEquals(0.6, kps.get(0).getRatio());
        assertTrue(byLabel.containsKey("鸡兔同笼"), "维护数据已落库，非自动聚合产生");
    }
}
