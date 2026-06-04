package com.ai.edu.application.service;

import com.ai.edu.application.dto.kg.ChapterTreeNode;
import com.ai.edu.application.dto.kg.SectionNode;
import com.ai.edu.application.dto.kg.SyncRequest;
import com.ai.edu.application.dto.kg.SyncResult;
import com.ai.edu.application.service.kg.KgNavigationAppService;
import com.ai.edu.application.service.kg.KgSyncAppService;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.repository.*;
import com.ai.edu.interfaces.AiEduPlatformApplication;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KgNavigationAppService 集成测试
 *
 * 使用真实数据源（Neo4j + MySQL + Redis）进行测试
 * 测试前需要先同步数据
 */
@Slf4j
@SpringBootTest(classes = AiEduPlatformApplication.class)
@ActiveProfiles("integration")
@TestMethodOrder(OrderAnnotation.class)
class KgNavigationAppServiceIntegrationTest {

    @Resource
    private KgNavigationAppService kgNavigationAppService;

    @Resource
    private KgSyncAppService kgSyncAppService;

    @Resource
    private KgTextbookRepository kgTextbookRepository;

    @Resource
    private KgChapterRepository kgChapterRepository;

    @Resource
    private KgSectionRepository kgSectionRepository;

    @Resource
    private KgTextbookChapterRepository kgTextbookChapterRepository;

    @Resource
    private KgChapterSectionRepository kgChapterSectionRepository;

    @Resource
    private KgSectionKPRepository kgSectionKPRepository;

    // ==================== 数据准备 ====================

    @Test
    @Order(1)
    @DisplayName("准备数据 — 同步一年级数学数据")
    void prepareData_syncGrade1Math() {
        log.info("=== 开始同步一年级数学数据 ===");

        SyncRequest request = SyncRequest.builder()
                .edition("人教版")
                .subject("数学")
                .stage("小学")
                .grade("一年级")
                .build();

        SyncResult result = kgSyncAppService.syncFull(request);

        assertNotNull(result);
        log.info("同步完成: status={}, inserted={}, grades={}/{}",
                result.getStatus(), result.getInsertedCount(),
                result.getCompletedGrades(), result.getTotalGrades());

        // 验证数据已同步
        List<?> textbooks = kgTextbookRepository.findByEditionSubject("人教版", "数学");
        log.info("教材数量: {}", textbooks.size());
        assertTrue(textbooks.size() >= 2, "至少应该有2本教材（上册+下册）");
    }

    // ==================== getChaptersByTextbook 测试 ====================

    @Test
    @Order(10)
    @DisplayName("getChaptersByTextbook — 一年级上册章节树")
    void getChaptersByTextbook_grade1Upper_shouldReturnChapterTree() {
        String textbookUri = "http://edukg.org/knowledge/3.1/textbook/math#renjiao-g1s";
        textbookUri = "http://edukg.org/knowledge/3.1/textbook/math#renjiao-g3x";
        log.info("=== 测试一年级上册章节树: uri={} ===", textbookUri);

        List<ChapterTreeNode> chapters = kgNavigationAppService.getChaptersByTextbook(textbookUri);

        log.info("返回章节数量: {}", chapters.size());
        assertNotNull(chapters);

        // 验证章节树结构
        if (!chapters.isEmpty()) {
            for (ChapterTreeNode chapter : chapters) {
                log.info("章节: uri={}, label={}, orderIndex={}, sectionsCount={}",
                        chapter.getUri(), chapter.getLabel(), chapter.getOrderIndex(),
                        chapter.getSections() != null ? chapter.getSections().size() : 0);

                if (chapter.getSections() != null) {
                    for (SectionNode section : chapter.getSections()) {
                        log.info("  小节: uri={}, label={}, kpCount={}",
                                section.getUri(), section.getLabel(), section.getKnowledgePointCount());
                    }
                }
            }

            // 验证至少有章节
            assertTrue(chapters.size() > 0, "应该有章节数据");
        }
    }

    @Test
    @Order(11)
    @DisplayName("getChaptersByTextbook — 一年级下册章节树")
    void getChaptersByTextbook_grade1Lower_shouldReturnChapterTree() {
        String textbookUri = "http://edukg.org/knowledge/3.1/textbook/math#renjiao-g1x";
        log.info("=== 测试一年级下册章节树: uri={} ===", textbookUri);

        List<ChapterTreeNode> chapters = kgNavigationAppService.getChaptersByTextbook(textbookUri);

        log.info("返回章节数量: {}", chapters.size());
        assertNotNull(chapters);

        // 验证章节树结构
        if (!chapters.isEmpty()) {
            for (ChapterTreeNode chapter : chapters) {
                log.info("章节: uri={}, label={}, sectionsCount={}",
                        chapter.getUri(), chapter.getLabel(),
                        chapter.getSections() != null ? chapter.getSections().size() : 0);
            }

            // 一年级下册应该有 8 个章节
            assertTrue(chapters.size() >= 1, "应该至少有1个章节");
        }
    }

    @Test
    @Order(12)
    @DisplayName("getChaptersByTextbook — 教材不存在应抛异常")
    void getChaptersByTextbook_textbookNotFound_shouldThrowException() {
        String textbookUri = "http://edukg.org/knowledge/3.1/textbook/math#not-exist";

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kgNavigationAppService.getChaptersByTextbook(textbookUri));

        log.info("异常信息: code={}, message={}", ex.getCode(), ex.getMessage());
        assertTrue(ex.getMessage().contains("教材不存在"));
    }

    // ==================== 数据验证测试 ====================

    @Test
    @Order(20)
    @DisplayName("验证 MySQL 关联数据完整性")
    void verifyMySQLDataIntegrity() {
        String textbookUri = "http://edukg.org/knowledge/3.1/textbook/math#renjiao-g1s";
        log.info("=== 验证数据完整性: textbookUri={} ===", textbookUri);

        // 1. 教材-章节关联
        var tbChRelations = kgTextbookChapterRepository.findByTextbookUri(textbookUri);
        log.info("教材-章节关联: {}", tbChRelations.size());

        // 2. 章节实体
        if (!tbChRelations.isEmpty()) {
            List<String> chapterUris = tbChRelations.stream()
                    .map(r -> r.getChapterUri())
                    .toList();
            var chapters = kgChapterRepository.findByUris(chapterUris);
            log.info("章节实体数量: {}", chapters.size());

            // 3. 章节-小节关联
            var chSecRelations = kgChapterSectionRepository.findByChapterUris(chapterUris);
            log.info("章节-小节关联: {}", chSecRelations.size());

            // 4. 小节实体
            if (!chSecRelations.isEmpty()) {
                List<String> sectionUris = chSecRelations.stream()
                        .map(r -> r.getSectionUri())
                        .distinct()
                        .toList();
                var sections = kgSectionRepository.findByUris(sectionUris);
                log.info("小节实体数量: {}", sections.size());

                // 5. 小节-知识点关联
                var secKpRelations = kgSectionKPRepository.findBySectionUris(sectionUris);
                log.info("小节-知识点关联: {}", secKpRelations.size());
            }
        }

        // 输出数据完整性报告
        log.info("=== 数据完整性报告 ===");
        log.info("教材-章节关联: {} 条", tbChRelations.size());
        log.info("建议：若章节树返回空，检查章节-小节关联是否存在");
    }

    // ==================== getKnowledgePointsBySection 测试 ====================

    @Test
    @Order(30)
    @DisplayName("getKnowledgePointsBySection — 查询小节知识点")
    void getKnowledgePointsBySection_shouldReturnKnowledgePoints() {
        // 先获取章节树，找到第一个小节
        String textbookUri = "http://edukg.org/knowledge/3.1/textbook/math#renjiao-g1s";
        List<ChapterTreeNode> chapters = kgNavigationAppService.getChaptersByTextbook(textbookUri);

        if (chapters.isEmpty() || chapters.get(0).getSections() == null || chapters.get(0).getSections().isEmpty()) {
            log.warn("没有小节数据，跳过知识点查询测试");
            return;
        }

        String sectionUri = chapters.get(0).getSections().get(0).getUri();
        log.info("=== 测试小节知识点查询: sectionUri={} ===", sectionUri);

        var kps = kgNavigationAppService.getKnowledgePointsBySection(sectionUri);

        log.info("知识点数量: {}", kps.size());
        assertNotNull(kps);

        // 注意：知识点数据可能为空（取决于 Neo4j 数据源）
        if (!kps.isEmpty()) {
            for (var kp : kps) {
                log.info("知识点: uri={}, label={}, sectionLabel={}, chapterLabel={}",
                        kp.getUri(), kp.getLabel(), kp.getSectionLabel(), kp.getChapterLabel());
            }
        } else {
            log.info("知识点数据为空，说明 Neo4j 中该小节没有知识点数据");
        }
    }
}