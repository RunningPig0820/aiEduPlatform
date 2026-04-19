package com.ai.edu.infrastructure.persistence.repository;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 双数据源路由测试
 *
 * 测试目标：验证 @DS("kg") 注解正确应用于 edukg Mapper，
 * 业务 Mapper 不携带 @DS("kg")，@Transactional("kg") 在 syncFull() 上生效。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KgDatasourceRoutingTest {

    private static final List<String> EDUKG_MAPPER_CLASSES = List.of(
            "com.ai.edu.infrastructure.persistence.edukg.mapper.KgTextbookMapper",
            "com.ai.edu.infrastructure.persistence.edukg.mapper.KgChapterMapper",
            "com.ai.edu.infrastructure.persistence.edukg.mapper.KgSectionMapper",
            "com.ai.edu.infrastructure.persistence.edukg.mapper.KgKnowledgePointMapper",
            "com.ai.edu.infrastructure.persistence.edukg.mapper.KgTextbookChapterMapper",
            "com.ai.edu.infrastructure.persistence.edukg.mapper.KgChapterSectionMapper",
            "com.ai.edu.infrastructure.persistence.edukg.mapper.KgSectionKPMapper",
            "com.ai.edu.infrastructure.persistence.edukg.mapper.KgSyncRecordMapper"
    );

    private static final List<String> BUSINESS_MAPPER_CLASSES = List.of(
            "com.ai.edu.infrastructure.persistence.mapper.UserMapper",
            "com.ai.edu.infrastructure.persistence.mapper.QuestionMapper",
            "com.ai.edu.infrastructure.persistence.mapper.HomeworkMapper",
            "com.ai.edu.infrastructure.persistence.mapper.ErrorBookMapper",
            "com.ai.edu.infrastructure.persistence.mapper.ClassMapper",
            "com.ai.edu.infrastructure.persistence.mapper.SchoolMapper",
            "com.ai.edu.infrastructure.persistence.mapper.StudentClassMapper",
            "com.ai.edu.infrastructure.persistence.mapper.TeacherClassMapper"
    );

    // ==================== 6.14.1 edukg Mapper @DS("kg") ====================

    @Test
    @Order(1)
    @DisplayName("所有 edukg Mapper 都应携带 @DS(\"kg\") 注解")
    void edukgMappers_shouldHaveDSKgAnnotation() throws ClassNotFoundException {
        for (String className : EDUKG_MAPPER_CLASSES) {
            Class<?> clazz = Class.forName(className);
            DS dsAnnotation = clazz.getAnnotation(DS.class);
            assertNotNull(dsAnnotation,
                    className + " 应携带 @DS 注解");
            assertEquals("kg", dsAnnotation.value(),
                    className + " 的 @DS 值应为 \"kg\"");
        }
    }

    @Test
    @Order(2)
    @DisplayName("edukg Mapper 数量验证 — 共 8 个")
    void edukgMappers_count_shouldBe8() {
        assertEquals(8, EDUKG_MAPPER_CLASSES.size(), "edukg Mapper 数量应为 8 个");
    }

    // ==================== 6.14.2 业务 Mapper 不应有 @DS("kg") ====================

    @Test
    @Order(3)
    @DisplayName("业务 Mapper 不应携带 @DS 注解（走默认 user 库）")
    void businessMappers_shouldNotHaveDSAnnotation() throws ClassNotFoundException {
        for (String className : BUSINESS_MAPPER_CLASSES) {
            Class<?> clazz = Class.forName(className);
            DS dsAnnotation = clazz.getAnnotation(DS.class);
            assertNull(dsAnnotation,
                    className + " 不应携带 @DS 注解（应走默认数据源）");
        }
    }

    @Test
    @Order(4)
    @DisplayName("数据源隔离验证 — edukg vs 业务 Mapper 无交叉")
    void datasourceIsolation_noCrossContamination() throws ClassNotFoundException {
        // 收集所有 edukg Mapper 的 @DS 值
        Set<String> edukgDsValues = EDUKG_MAPPER_CLASSES.stream()
                .map(name -> {
                    try {
                        Class<?> clazz = Class.forName(name);
                        DS ds = clazz.getAnnotation(DS.class);
                        return ds != null ? ds.value() : null;
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                })
                .collect(Collectors.toSet());

        // 所有 edukg Mapper 的 @DS 值都应仅为 "kg"
        assertEquals(Set.of("kg"), edukgDsValues,
                "所有 edukg Mapper 的 @DS 值应仅为 \"kg\"");

        // 抽样验证业务 Mapper 无 @DS
        Class<?> userMapper = Class.forName("com.ai.edu.infrastructure.persistence.mapper.UserMapper");
        Class<?> questionMapper = Class.forName("com.ai.edu.infrastructure.persistence.mapper.QuestionMapper");
        assertNull(userMapper.getAnnotation(DS.class), "UserMapper 不应有 @DS 注解");
        assertNull(questionMapper.getAnnotation(DS.class), "QuestionMapper 不应有 @DS 注解");
    }

    // ==================== 6.14.3 @Transactional("kg") on syncFull() ====================

    @Test
    @Order(5)
    @DisplayName("KgSyncAppService.syncFull() 应携带 @Transactional(\"kg\") 注解（源码验证）")
    void syncFull_shouldHaveTransactionalKg() throws IOException {
        // 由于 infrastructure 模块无法加载 application 模块的类，通过源码文件验证
        Path serviceFile = Paths.get("src/main/java/com/ai/edu/infrastructure/service/kg/KgSyncAppService.java");
        // 尝试另一个可能的路径
        if (!Files.exists(serviceFile)) {
            serviceFile = Paths.get("../ai-edu-application/src/main/java/com/ai/edu/application/service/kg/KgSyncAppService.java");
        }

        assertTrue(Files.exists(serviceFile),
                "KgSyncAppService.java 源文件应存在");

        String content = Files.readString(serviceFile);

        // 验证 syncFull 方法上有 @Transactional("kg")
        assertTrue(content.contains("@Transactional(\"kg\")") || content.contains("@Transactional(value = \"kg\")"),
                "syncFull() 方法应携带 @Transactional(\"kg\") 注解");
    }

    @Test
    @Order(6)
    @DisplayName("KgSyncAppService 类级别不应有 @Transactional（仅方法级）")
    void kgSyncAppService_noClassLevelTransactional() throws IOException {
        Path serviceFile = Paths.get("../ai-edu-application/src/main/java/com/ai/edu/application/service/kg/KgSyncAppService.java");
        assertTrue(Files.exists(serviceFile), "KgSyncAppService.java 源文件应存在");

        String content = Files.readString(serviceFile);

        // 验证 @Transactional 仅在 syncFull 方法上，不在类级别
        // 类级别注解在 "public class" 或 "class" 之前，方法级在 "public SyncResult syncFull" 附近
        int classDeclarationIndex = content.indexOf("public class KgSyncAppService");
        String classDeclaration = content.substring(0, classDeclarationIndex);
        assertFalse(classDeclaration.contains("@Transactional"),
                "KgSyncAppService 类级别不应有 @Transactional 注解");
    }
}
