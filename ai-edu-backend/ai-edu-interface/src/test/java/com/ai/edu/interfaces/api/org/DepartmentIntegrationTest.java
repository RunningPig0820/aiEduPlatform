package com.ai.edu.interfaces.api.org;

import com.ai.edu.application.dto.org.DepartmentDTO;
import com.ai.edu.application.dto.org.command.CreateDepartmentCommand;
import com.ai.edu.application.service.org.DepartmentAppService;
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
 * 部门应用服务集成测试
 * 使用 H2 内存数据库，在 interface 模块运行
 */
@SpringBootTest(classes = AiEduPlatformApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DepartmentIntegrationTest {

    @Autowired
    private DepartmentAppService departmentAppService;

    private static final Long TEST_SCHOOL_ID = 1L;

    @Test
    @Order(1)
    @DisplayName("创建根级部门")
    void createRootDepartment() {
        CreateDepartmentCommand command = CreateDepartmentCommand.builder()
                .name("教务处")
                .sortOrder(1)
                .description("教务管理部门")
                .build();

        DepartmentDTO result = departmentAppService.createDepartment(TEST_SCHOOL_ID, command);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("教务处", result.getName());
        assertTrue(result.getIsRoot());
        assertNull(result.getDepartmentPath());
    }

    @Test
    @Order(2)
    @DisplayName("创建子部门")
    void createChildDepartment() {
        // 先创建根部门
        CreateDepartmentCommand rootCmd = CreateDepartmentCommand.builder()
                .name("学生处")
                .sortOrder(2)
                .build();
        DepartmentDTO root = departmentAppService.createDepartment(TEST_SCHOOL_ID, rootCmd);

        // 创建子部门
        CreateDepartmentCommand childCmd = CreateDepartmentCommand.builder()
                .name("心理咨询室")
                .parentId(root.getId())
                .sortOrder(1)
                .build();
        DepartmentDTO child = departmentAppService.createDepartment(TEST_SCHOOL_ID, childCmd);

        assertNotNull(child);
        assertEquals(root.getId(), child.getParentId());
        assertEquals(String.valueOf(root.getId()), child.getDepartmentPath());
    }

    @Test
    @Order(3)
    @DisplayName("获取部门树")
    void getDepartmentTree() {
        // 创建两个根部门
        departmentAppService.createDepartment(TEST_SCHOOL_ID,
                CreateDepartmentCommand.builder().name("部门A").build());
        departmentAppService.createDepartment(TEST_SCHOOL_ID,
                CreateDepartmentCommand.builder().name("部门B").build());

        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree(TEST_SCHOOL_ID);

        assertNotNull(tree);
        assertTrue(tree.size() >= 2);
    }

    @Test
    @Order(4)
    @DisplayName("删除部门")
    void deleteDepartment() {
        // 创建叶子部门
        DepartmentDTO dept = departmentAppService.createDepartment(TEST_SCHOOL_ID,
                CreateDepartmentCommand.builder().name("临时部门").build());

        // 删除
        departmentAppService.deleteDepartment(TEST_SCHOOL_ID, dept.getId());

        // 验证已删除
        assertThrows(Exception.class, () ->
                departmentAppService.getDepartmentById(TEST_SCHOOL_ID, dept.getId()));
    }
}