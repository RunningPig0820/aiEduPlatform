package com.ai.edu.interfaces.api.org;

import com.ai.edu.application.dto.org.AdminClassNodeDTO;
import com.ai.edu.application.dto.org.command.CreateAdminClassNodeCommand;
import com.ai.edu.application.dto.org.command.UpdateAdminClassNodeCommand;
import com.ai.edu.application.service.org.AdminClassAppService;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.interfaces.AiEduPlatformApplication;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行政班应用服务集成测试
 * 使用 H2 内存数据库
 */
@SpringBootTest(classes = AiEduPlatformApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class AdminClassServiceIntegrationTest {

    @Autowired
    private AdminClassAppService adminClassAppService;

    private static final Long TEST_SCHOOL_ID = 1L;
    private static Long stageDeptId;
    private static Long gradeDeptId;

    // ==================== 创建测试 ====================

    @Test
    @Order(1)
    @DisplayName("创建学段节点：Department + DepartmentEdu 同时创建")
    void createStageNode() {
        CreateAdminClassNodeCommand command = CreateAdminClassNodeCommand.builder()
                .name("小学部")
                .schoolId(TEST_SCHOOL_ID)
                .deptType(3)
                .stageCode("PRIMARY")
                .stageYearCode("4")
                .sortOrder(0)
                .description("小学部测试")
                .build();

        AdminClassNodeDTO result = adminClassAppService.createNode(command);

        assertNotNull(result);
        assertNotNull(result.getDeptId());
        assertEquals("小学部", result.getName());
        assertEquals("ADMIN_CLASS", result.getDepartmentType());
        assertEquals(3, result.getDeptType());
        assertEquals("PRIMARY", result.getStageCode());
        assertEquals("4", result.getStageYearCode());
        stageDeptId = result.getDeptId();
    }

    @Test
    @Order(2)
    @DisplayName("创建年级节点：挂在学段下")
    void createGradeNode() {
        assertNotNull(stageDeptId, "需要先创建学段节点");

        CreateAdminClassNodeCommand command = CreateAdminClassNodeCommand.builder()
                .name("2024级")
                .schoolId(TEST_SCHOOL_ID)
                .parentId(stageDeptId)
                .deptType(4)
                .stageCode("PRIMARY")
                .stageYearCode("4")
                .gradeCode("1")
                .enrollmentYear("2024")
                .sortOrder(0)
                .build();

        AdminClassNodeDTO result = adminClassAppService.createNode(command);

        assertNotNull(result);
        assertEquals(stageDeptId, result.getParentId());
        assertEquals(4, result.getDeptType());
        assertEquals("1", result.getGradeCode());
        assertEquals("2024", result.getEnrollmentYear());
        gradeDeptId = result.getDeptId();
    }

    @Test
    @Order(3)
    @DisplayName("创建班级节点：挂在年级下")
    void createClassNode() {
        assertNotNull(gradeDeptId, "需要先创建年级节点");

        CreateAdminClassNodeCommand command = CreateAdminClassNodeCommand.builder()
                .name("1班")
                .schoolId(TEST_SCHOOL_ID)
                .parentId(gradeDeptId)
                .deptType(5)
                .stageCode("PRIMARY")
                .stageYearCode("4")
                .gradeCode("1")
                .enrollmentYear("2024")
                .sortOrder(0)
                .build();

        AdminClassNodeDTO result = adminClassAppService.createNode(command);

        assertNotNull(result);
        assertEquals(gradeDeptId, result.getParentId());
        assertEquals(5, result.getDeptType());
    }

    @Test
    @Order(4)
    @DisplayName("创建年级节点缺少 gradeCode/enrollmentYear 应抛异常")
    void createGradeWithMissingFields() {
        CreateAdminClassNodeCommand command = CreateAdminClassNodeCommand.builder()
                .name("错误年级")
                .schoolId(TEST_SCHOOL_ID)
                .deptType(4)
                .stageCode("PRIMARY")
                .stageYearCode("4")
                .build();

        assertThrows(BusinessException.class, () -> adminClassAppService.createNode(command));
    }

    // ==================== 树查询测试 ====================

    @Test
    @Order(5)
    @DisplayName("getNodeTree() 应返回正确的三层树结构")
    void getNodeTree() {
        List<AdminClassNodeDTO> tree = adminClassAppService.getNodeTree(TEST_SCHOOL_ID);

        assertNotNull(tree);
        assertFalse(tree.isEmpty());

        // 根节点应是学段
        AdminClassNodeDTO stage = tree.get(0);
        assertEquals("小学部", stage.getName());
        assertEquals(3, stage.getDeptType());
        assertNotNull(stage.getChildren());
        assertEquals(1, stage.getChildren().size());

        // 子节点是年级
        AdminClassNodeDTO grade = stage.getChildren().get(0);
        assertEquals("2024级", grade.getName());
        assertEquals(4, grade.getDeptType());
        assertNotNull(grade.getChildren());
        assertEquals(1, grade.getChildren().size());

        // 孙节点是班级
        AdminClassNodeDTO clazz = grade.getChildren().get(0);
        assertEquals("1班", clazz.getName());
        assertEquals(5, clazz.getDeptType());
    }

    // ==================== 更新测试 ====================

    @Test
    @Order(6)
    @DisplayName("更新节点名称和扩展属性")
    void updateNode() {
        assertNotNull(gradeDeptId);

        UpdateAdminClassNodeCommand command = UpdateAdminClassNodeCommand.builder()
                .name("2025级")
                .gradeCode("2")
                .enrollmentYear("2025")
                .build();

        AdminClassNodeDTO result = adminClassAppService.updateNode(gradeDeptId, command);

        assertEquals("2025级", result.getName());
        assertEquals("2", result.getGradeCode());
        assertEquals("2025", result.getEnrollmentYear());
    }

    // ==================== 详情查询测试 ====================

    @Test
    @Order(7)
    @DisplayName("getNodeDetail() 应返回完整节点信息")
    void getNodeDetail() {
        assertNotNull(stageDeptId);

        AdminClassNodeDTO detail = adminClassAppService.getNodeDetail(stageDeptId);

        assertNotNull(detail);
        assertEquals("小学部", detail.getName());
        assertEquals("PRIMARY", detail.getStageCode());
        assertEquals("ADMIN_CLASS", detail.getDepartmentType());
    }

    // ==================== 删除测试 ====================

    @Test
    @Order(8)
    @DisplayName("删除有子节点的节点应抛异常")
    void deleteNodeWithChildren() {
        assertNotNull(stageDeptId);
        assertThrows(BusinessException.class, () -> adminClassAppService.deleteNode(stageDeptId));
    }

    @Test
    @Order(9)
    @DisplayName("删除叶子节点应成功（班级）")
    void deleteLeafNode() {
        // 创建一个独立的班级节点用于测试删除
        CreateAdminClassNodeCommand cmd = CreateAdminClassNodeCommand.builder()
                .name("独立班级")
                .schoolId(TEST_SCHOOL_ID)
                .deptType(5)
                .stageCode("PRIMARY")
                .stageYearCode("4")
                .gradeCode("1")
                .enrollmentYear("2024")
                .build();
        AdminClassNodeDTO node = adminClassAppService.createNode(cmd);

        // 删除
        adminClassAppService.deleteNode(node.getDeptId());

        // 验证已删除
        assertThrows(BusinessException.class, () ->
                adminClassAppService.getNodeDetail(node.getDeptId()));
    }

    @Test
    @Order(10)
    @DisplayName("查询空学校应返回空列表")
    void getNodeTree_emptySchool() {
        List<AdminClassNodeDTO> tree = adminClassAppService.getNodeTree(2L);
        assertNotNull(tree);
        assertTrue(tree.isEmpty());
    }
}
