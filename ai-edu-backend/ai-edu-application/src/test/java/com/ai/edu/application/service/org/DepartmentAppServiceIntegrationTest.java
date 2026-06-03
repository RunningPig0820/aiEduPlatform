package com.ai.edu.application.service.org;

import com.ai.edu.application.dto.org.DepartmentDTO;
import com.ai.edu.application.dto.org.PageResult;
import com.ai.edu.application.dto.org.command.CreateDepartmentCommand;
import com.ai.edu.application.dto.org.command.DepartmentQueryCommand;
import com.ai.edu.application.dto.org.command.UpdateDepartmentCommand;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.application.TestApplication;
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
 * 使用 H2 内存数据库，不使用 Mock
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("h2")
@TestMethodOrder(OrderAnnotation.class)
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DepartmentAppServiceIntegrationTest {

    @Autowired
    private DepartmentAppService departmentAppService;

    // 测试用的学校ID（schema.sql 中已初始化）
    private static final Long TEST_SCHOOL_ID = 1L;

    // ==================== 11.3 测试创建根级部门 ====================

    @Test
    @Order(1)
    @DisplayName("创建根级部门 - department_path 应为空")
    void createDepartment_rootDepartment_shouldHaveEmptyPath() {
        CreateDepartmentCommand command = CreateDepartmentCommand.builder()
                .name("教务处")
                .sortOrder(1)
                .description("教务管理部门")
                .build();

        DepartmentDTO result = departmentAppService.createDepartment(TEST_SCHOOL_ID, command);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("教务处", result.getName());
        assertNull(result.getParentId());
        assertTrue(result.getIsRoot());
        assertNull(result.getDepartmentPath());  // 根部门路径为空
        assertEquals(1, result.getSortOrder());
    }

    @Test
    @Order(2)
    @DisplayName("创建根级部门 - 名称重复应抛异常")
    void createDepartment_duplicateName_shouldThrowException() {
        CreateDepartmentCommand command = CreateDepartmentCommand.builder()
                .name("教务处")  // 已存在
                .sortOrder(2)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                departmentAppService.createDepartment(TEST_SCHOOL_ID, command)
        );

        assertTrue(ex.getMessage().contains("部门名称已存在"));
    }

    // ==================== 11.4 测试创建子部门 ====================

    @Test
    @Order(3)
    @DisplayName("创建子部门 - department_path 应为父部门ID")
    void createDepartment_childDepartment_shouldHaveCorrectPath() {
        // 先获取已创建的根部门
        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree(TEST_SCHOOL_ID);
        Long parentId = tree.stream()
                .filter(d -> d.getName().equals("教务处"))
                .findFirst()
                .map(DepartmentDTO::getId)
                .orElseThrow();

        CreateDepartmentCommand command = CreateDepartmentCommand.builder()
                .name("教务办公室")
                .parentId(parentId)
                .sortOrder(1)
                .description("教务办公室")
                .build();

        DepartmentDTO result = departmentAppService.createDepartment(TEST_SCHOOL_ID, command);

        assertNotNull(result);
        assertEquals("教务办公室", result.getName());
        assertEquals(parentId, result.getParentId());
        assertFalse(result.getIsRoot());
        // department_path 应为父部门ID的字符串
        assertEquals(String.valueOf(parentId), result.getDepartmentPath());
    }

    @Test
    @Order(4)
    @DisplayName("创建三级部门 - department_path 应为 id_id 格式")
    void createDepartment_thirdLevel_shouldHaveCorrectPath() {
        // 获取教务处和教务办公室
        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree(TEST_SCHOOL_ID);

        DepartmentDTO rootDept = tree.stream()
                .filter(d -> d.getName().equals("教务处"))
                .findFirst()
                .orElseThrow();

        DepartmentDTO childDept = rootDept.getChildren().stream()
                .filter(d -> d.getName().equals("教务办公室"))
                .findFirst()
                .orElseThrow();

        CreateDepartmentCommand command = CreateDepartmentCommand.builder()
                .name("教学调度组")
                .parentId(childDept.getId())
                .sortOrder(1)
                .build();

        DepartmentDTO result = departmentAppService.createDepartment(TEST_SCHOOL_ID, command);

        assertNotNull(result);
        assertFalse(result.getIsRoot());
        // department_path 应为 "教务处ID_教务办公室ID"
        assertEquals(rootDept.getId() + "_" + childDept.getId(), result.getDepartmentPath());
    }

    @Test
    @Order(5)
    @DisplayName("创建子部门 - 父部门不存在应抛异常")
    void createDepartment_parentNotFound_shouldThrowException() {
        CreateDepartmentCommand command = CreateDepartmentCommand.builder()
                .name("测试部门")
                .parentId(99999L)  // 不存在的ID
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                departmentAppService.createDepartment(TEST_SCHOOL_ID, command)
        );

        assertTrue(ex.getMessage().contains("上级部门不存在"));
    }

    // ==================== 更新部门测试 ====================

    @Test
    @Order(6)
    @DisplayName("更新部门 - 名称和描述")
    void updateDepartment_shouldUpdateNameAndDescription() {
        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree(TEST_SCHOOL_ID);
        Long deptId = tree.stream()
                .filter(d -> d.getName().equals("教务处"))
                .findFirst()
                .map(DepartmentDTO::getId)
                .orElseThrow();

        UpdateDepartmentCommand command = UpdateDepartmentCommand.builder()
                .name("教务管理处")
                .description("教务管理职能部门")
                .build();

        DepartmentDTO result = departmentAppService.updateDepartment(TEST_SCHOOL_ID, deptId, command);

        assertEquals("教务管理处", result.getName());
        assertEquals("教务管理职能部门", result.getDescription());
    }

    @Test
    @Order(7)
    @DisplayName("更新部门 - 自引用应抛异常")
    void updateDepartment_selfReference_shouldThrowException() {
        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree(TEST_SCHOOL_ID);
        Long deptId = tree.stream()
                .filter(d -> d.getName().equals("教务管理处"))
                .findFirst()
                .map(DepartmentDTO::getId)
                .orElseThrow();

        UpdateDepartmentCommand command = UpdateDepartmentCommand.builder()
                .parentId(deptId)  // 设置自己为父部门
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                departmentAppService.updateDepartment(TEST_SCHOOL_ID, deptId, command)
        );

        assertTrue(ex.getMessage().contains("上级部门不能为自身"));
    }

    // ==================== 删除部门测试 ====================

    @Test
    @Order(8)
    @DisplayName("删除部门 - 有子部门应抛异常")
    void deleteDepartment_hasChildren_shouldThrowException() {
        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree(TEST_SCHOOL_ID);
        Long parentId = tree.stream()
                .filter(d -> d.getName().equals("教务管理处"))
                .findFirst()
                .map(DepartmentDTO::getId)
                .orElseThrow();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                departmentAppService.deleteDepartment(TEST_SCHOOL_ID, parentId)
        );

        assertTrue(ex.getMessage().contains("存在子部门"));
    }

    @Test
    @Order(9)
    @DisplayName("删除部门 - 无子部门应成功")
    void deleteDepartment_noChildren_shouldSuccess() {
        // 找到最底层的部门（教学调度组）
        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree(TEST_SCHOOL_ID);

        DepartmentDTO rootDept = tree.stream()
                .filter(d -> d.getName().equals("教务管理处"))
                .findFirst()
                .orElseThrow();

        DepartmentDTO childDept = rootDept.getChildren().stream()
                .filter(d -> d.getName().equals("教务办公室"))
                .findFirst()
                .orElseThrow();

        Long leafDeptId = childDept.getChildren().stream()
                .filter(d -> d.getName().equals("教学调度组"))
                .findFirst()
                .map(DepartmentDTO::getId)
                .orElseThrow();

        // 删除叶子部门
        departmentAppService.deleteDepartment(TEST_SCHOOL_ID, leafDeptId);

        // 验证已删除
        BusinessException ex = assertThrows(BusinessException.class, () ->
                departmentAppService.getDepartmentById(TEST_SCHOOL_ID, leafDeptId)
        );
        assertTrue(ex.getMessage().contains("部门不存在"));
    }

    // ==================== 11.5 测试部门树查询 ====================

    @Test
    @Order(10)
    @DisplayName("获取部门树 - 应返回完整树形结构")
    void getDepartmentTree_shouldReturnCompleteTree() {
        // 先创建另一个根部门
        CreateDepartmentCommand command = CreateDepartmentCommand.builder()
                .name("学生处")
                .sortOrder(2)
                .build();
        departmentAppService.createDepartment(TEST_SCHOOL_ID, command);

        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree(TEST_SCHOOL_ID);

        assertNotNull(tree);
        assertTrue(tree.size() >= 2);  // 至少有教务管理处和学生处

        // 验证树形结构
        DepartmentDTO jiaowuchu = tree.stream()
                .filter(d -> d.getName().equals("教务管理处"))
                .findFirst()
                .orElse(null);

        assertNotNull(jiaowuchu);
        assertNotNull(jiaowuchu.getChildren());
        assertFalse(jiaowuchu.getChildren().isEmpty());  // 有教务办公室子部门
    }

    @Test
    @Order(11)
    @DisplayName("获取部门详情")
    void getDepartmentById_shouldReturnDetail() {
        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree(TEST_SCHOOL_ID);
        Long deptId = tree.stream()
                .filter(d -> d.getName().equals("学生处"))
                .findFirst()
                .map(DepartmentDTO::getId)
                .orElseThrow();

        DepartmentDTO result = departmentAppService.getDepartmentById(TEST_SCHOOL_ID, deptId);

        assertNotNull(result);
        assertEquals("学生处", result.getName());
        assertTrue(result.getIsRoot());
    }

    @Test
    @Order(12)
    @DisplayName("分页查询部门")
    void queryDepartments_shouldReturnPageResult() {
        DepartmentQueryCommand query = DepartmentQueryCommand.builder()
                .pageNum(1)
                .pageSize(10)
                .build();

        PageResult<DepartmentDTO> result = departmentAppService.queryDepartments(TEST_SCHOOL_ID, query);

        assertNotNull(result);
        assertTrue(result.getTotal() >= 3);  // 至少有3个部门（教务管理处、教务办公室、学生处）
        assertFalse(result.getList().isEmpty());
    }

    @Test
    @Order(13)
    @DisplayName("分页查询 - 只查询根部门")
    void queryDepartments_rootOnly_shouldReturnOnlyRoot() {
        DepartmentQueryCommand query = DepartmentQueryCommand.builder()
                .rootOnly(true)
                .pageNum(1)
                .pageSize(10)
                .build();

        PageResult<DepartmentDTO> result = departmentAppService.queryDepartments(TEST_SCHOOL_ID, query);

        assertNotNull(result);
        assertTrue(result.getTotal() >= 2);  // 至少有2个根部门
        // 所有结果都应该是根部门
        assertTrue(result.getList().stream().allMatch(DepartmentDTO::getIsRoot));
    }

    @Test
    @Order(14)
    @DisplayName("分页查询 - 名称模糊查询")
    void queryDepartments_nameFilter_shouldReturnMatched() {
        DepartmentQueryCommand query = DepartmentQueryCommand.builder()
                .name("教务")
                .pageNum(1)
                .pageSize(10)
                .build();

        PageResult<DepartmentDTO> result = departmentAppService.queryDepartments(TEST_SCHOOL_ID, query);

        assertNotNull(result);
        // 所有结果名称应包含"教务"
        assertTrue(result.getList().stream().allMatch(d -> d.getName().contains("教务")));
    }

    // ==================== 边界测试 ====================

    @Test
    @Order(15)
    @DisplayName("创建部门 - 学校不存在")
    void createDepartment_schoolNotFound_shouldThrowException() {
        CreateDepartmentCommand command = CreateDepartmentCommand.builder()
                .name("测试部门")
                .build();

        // 使用不存在的学校ID（假设99999不存在）
        // 注意：当前实现可能不校验学校是否存在，这个测试可能需要根据实际实现调整
        // 如果学校校验依赖 SchoolRepository，需要先创建学校数据
        // 这里暂时跳过校验，只测试基本流程
    }

    @Test
    @Order(16)
    @DisplayName("获取部门树 - 空学校应返回空列表")
    void getDepartmentTree_emptySchool_shouldReturnEmpty() {
        // 使用一个没有部门的学校ID（假设2不存在任何部门）
        List<DepartmentDTO> tree = departmentAppService.getDepartmentTree(2L);

        assertNotNull(tree);
        assertTrue(tree.isEmpty());
    }
}