package com.ai.edu.interfaces.test.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.org.OrgTeacherDTO;
import com.ai.edu.application.dto.org.PageResult;
import com.ai.edu.application.dto.org.command.CreateOrgTeacherCommand;
import com.ai.edu.application.dto.org.command.OrgTeacherQueryParamDTO;
import com.ai.edu.application.dto.org.command.UpdateOrgTeacherCommand;
import com.ai.edu.interfaces.test.config.TestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 教职工管理集成测试（基于 H2 数据库，不使用 Mock）
 *
 * 契约对齐 OrgTeacherController 的 RPC 风格路径（不要用 RESTful /teachers）：
 * - POST   /api/auth/schools/{schoolId}/addTeacher        创建（用户不存在则自动创建）
 * - GET    /api/auth/schools/{schoolId}/getTeacher/{id}   查询详情（id = 关联关系ID）
 * - POST   /api/auth/schools/{schoolId}/getTeacherList    分页查询（body 为 OrgTeacherQueryParamDTO）
 * - POST   /api/auth/schools/{schoolId}/updateTeacher     更新部门（body 为 UpdateOrgTeacherCommand）
 * - POST   /api/auth/schools/{schoolId}/deleteTeacher/{id} 删除关联（用户数据保留）
 *
 * 响应统一 ApiResponse{code,message,data}，无 success 字段；业务异常 → HTTP 400。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WithMockUser(username = "admin", roles = {"ADMIN"}) // 模拟登录用户
class OrgTeacherIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long SCHOOL_ID = 1L; // 测试学校ID
    private static final Long DEPARTMENT_ID_1 = 2L; // 语文教研组
    private static final Long DEPARTMENT_ID_2 = 3L; // 数学教研组
    private static final String EXISTING_PHONE = "13800138001"; // 已存在的张三手机号（teacher001）
    private static final String NEW_PHONE = "13900139001"; // 新用户手机号

    private static Long teacher1Id; // 张三的教职工关联ID（Order 1 创建）
    private static Long teacher2Id; // 新教师的教职工关联ID（Order 2 创建）

    @Test
    @Order(1)
    @DisplayName("创建教职工 - 用户已存在")
    void testCreateOrgTeacher_UserExists() throws Exception {
        CreateOrgTeacherCommand command = CreateOrgTeacherCommand.builder()
                .name("张三") // 已存在用户
                .phone(EXISTING_PHONE)
                .departmentId(DEPARTMENT_ID_1)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/schools/{schoolId}/addTeacher", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        OrgTeacherDTO teacher = parseResponseData(result);

        assertThat(teacher).isNotNull();
        assertThat(teacher.getUserId()).isEqualTo(1L); // 已存在用户的ID
        assertThat(teacher.getName()).isEqualTo("张三");
        assertThat(teacher.getPhone()).isEqualTo(EXISTING_PHONE);
        assertThat(teacher.getDepartmentId()).isEqualTo(DEPARTMENT_ID_1);
        assertThat(teacher.getDepartmentName()).isEqualTo("语文教研组");
        teacher1Id = teacher.getId();

        System.out.println("✅ 测试通过：创建教职工（用户已存在）");
        System.out.println("   教职工ID: " + teacher.getId());
        System.out.println("   用户ID: " + teacher.getUserId());
    }

    @Test
    @Order(2)
    @DisplayName("创建教职工 - 用户不存在，自动创建用户")
    void testCreateOrgTeacher_UserNotExists() throws Exception {
        CreateOrgTeacherCommand command = CreateOrgTeacherCommand.builder()
                .name("新教师")
                .phone(NEW_PHONE)
                .departmentId(DEPARTMENT_ID_2)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/schools/{schoolId}/addTeacher", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        OrgTeacherDTO teacher = parseResponseData(result);

        assertThat(teacher).isNotNull();
        assertThat(teacher.getUserId()).isGreaterThan(3L); // 新创建的用户ID（大于预置的 teacher001/002/003）
        assertThat(teacher.getName()).isEqualTo("新教师");
        assertThat(teacher.getPhone()).isEqualTo(NEW_PHONE);
        assertThat(teacher.getDepartmentId()).isEqualTo(DEPARTMENT_ID_2);
        assertThat(teacher.getDepartmentName()).isEqualTo("数学教研组");
        teacher2Id = teacher.getId();

        System.out.println("✅ 测试通过：创建教职工（用户不存在，自动创建用户）");
        System.out.println("   教职工ID: " + teacher.getId());
        System.out.println("   用户ID（新创建）: " + teacher.getUserId());
    }

    @Test
    @Order(3)
    @DisplayName("创建教职工失败 - 用户已在本学校有教职工记录")
    void testCreateOrgTeacher_Duplicate() throws Exception {
        CreateOrgTeacherCommand command = CreateOrgTeacherCommand.builder()
                .name("张三")
                .phone(EXISTING_PHONE)
                .departmentId(DEPARTMENT_ID_2)
                .build();

        // 业务异常 → HTTP 400，message 即领域异常信息
        mockMvc.perform(post("/api/auth/schools/{schoolId}/addTeacher", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该用户已在本学校有教职工记录"));

        System.out.println("✅ 测试通过：重复创建教职工被拒绝");
    }

    @Test
    @Order(4)
    @DisplayName("查询教职工详情（聚合查询）")
    void testGetOrgTeacher() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/schools/{schoolId}/getTeacher/{id}", SCHOOL_ID, teacher1Id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        OrgTeacherDTO teacher = parseResponseData(result);

        assertThat(teacher).isNotNull();
        assertThat(teacher.getUserId()).isEqualTo(1L);
        assertThat(teacher.getName()).isEqualTo("张三"); // 用户基本信息（来自用户域）
        assertThat(teacher.getPhone()).isEqualTo(EXISTING_PHONE); // 用户基本信息（来自用户域）
        assertThat(teacher.getDepartmentId()).isEqualTo(DEPARTMENT_ID_1);
        assertThat(teacher.getDepartmentName()).isEqualTo("语文教研组"); // 部门名称（来自组织域）

        System.out.println("✅ 测试通过：查询教职工详情（聚合查询）");
    }

    @Test
    @Order(5)
    @DisplayName("查询教职工列表（聚合查询）")
    void testListOrgTeachers() throws Exception {
        OrgTeacherQueryParamDTO query = OrgTeacherQueryParamDTO.builder()
                .pageNum(1).pageSize(10)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/schools/{schoolId}/getTeacherList", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        PageResult<OrgTeacherDTO> pageResult = parseResponsePage(result);

        assertThat(pageResult).isNotNull();
        assertThat(pageResult.getList()).isNotEmpty();
        assertThat(pageResult.getTotal()).isGreaterThanOrEqualTo(2);

        for (OrgTeacherDTO teacher : pageResult.getList()) {
            assertThat(teacher.getUserId()).isNotNull();
            assertThat(teacher.getName()).isNotNull(); // 用户基本信息（来自用户域）
            assertThat(teacher.getPhone()).isNotNull(); // 用户基本信息（来自用户域）
            assertThat(teacher.getDepartmentId()).isNotNull();
            assertThat(teacher.getDepartmentName()).isNotNull(); // 部门名称（来自组织域）
        }

        System.out.println("✅ 测试通过：查询教职工列表（聚合查询）总数=" + pageResult.getTotal());
    }

    @Test
    @Order(6)
    @DisplayName("按部门查询教职工列表")
    void testListOrgTeachers_ByDepartment() throws Exception {
        OrgTeacherQueryParamDTO query = OrgTeacherQueryParamDTO.builder()
                .departmentId(DEPARTMENT_ID_1)
                .pageNum(1).pageSize(10)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/schools/{schoolId}/getTeacherList", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        PageResult<OrgTeacherDTO> pageResult = parseResponsePage(result);

        assertThat(pageResult).isNotNull();
        assertThat(pageResult.getList()).isNotEmpty();
        assertThat(pageResult.getList().get(0).getDepartmentId()).isEqualTo(DEPARTMENT_ID_1);
        assertThat(pageResult.getList().get(0).getDepartmentName()).isEqualTo("语文教研组");

        System.out.println("✅ 测试通过：按部门查询教职工列表");
    }

    @Test
    @Order(7)
    @DisplayName("更新教职工所属部门")
    void testUpdateOrgTeacher() throws Exception {
        // orgTeacherId 在请求体中（UpdateOrgTeacherCommand），而非路径参数
        UpdateOrgTeacherCommand command = UpdateOrgTeacherCommand.builder()
                .orgTeacherId(teacher1Id)
                .departmentId(DEPARTMENT_ID_2)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/schools/{schoolId}/updateTeacher", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        OrgTeacherDTO teacher = parseResponseData(result);

        assertThat(teacher).isNotNull();
        assertThat(teacher.getDepartmentId()).isEqualTo(DEPARTMENT_ID_2);
        assertThat(teacher.getDepartmentName()).isEqualTo("数学教研组");

        System.out.println("✅ 测试通过：更新教职工所属部门");
    }

    @Test
    @Order(8)
    @DisplayName("删除教职工关联关系")
    void testDeleteOrgTeacher() throws Exception {
        mockMvc.perform(post("/api/auth/schools/{schoolId}/deleteTeacher/{id}", SCHOOL_ID, teacher2Id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        // 验证删除后查询不到 → BusinessException(SCHOOL_NOT_FOUND) → HTTP 400
        mockMvc.perform(get("/api/auth/schools/{schoolId}/getTeacher/{id}", SCHOOL_ID, teacher2Id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("教职工不存在"));

        // 验证列表少了一条记录（此时仅剩 Order 1 创建的张三）
        OrgTeacherQueryParamDTO query = OrgTeacherQueryParamDTO.builder()
                .pageNum(1).pageSize(10)
                .build();
        MvcResult result = mockMvc.perform(post("/api/auth/schools/{schoolId}/getTeacherList", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isOk())
                .andReturn();

        PageResult<OrgTeacherDTO> pageResult = parseResponsePage(result);
        assertThat(pageResult.getTotal()).isLessThan(2);

        System.out.println("✅ 测试通过：删除教职工关联关系，当前总数=" + pageResult.getTotal());
    }

    @Test
    @Order(9)
    @DisplayName("完整业务流程验证")
    void testCompleteBusinessFlow() throws Exception {
        System.out.println("\n========== 教职工管理完整业务流程验证 ==========");

        // 1. 创建新教职工
        System.out.println("\n步骤1：创建新教职工（用户不存在）");
        CreateOrgTeacherCommand command1 = CreateOrgTeacherCommand.builder()
                .name("流程测试教师")
                .phone("18800188001")
                .departmentId(DEPARTMENT_ID_1)
                .build();

        MvcResult result1 = mockMvc.perform(post("/api/auth/schools/{schoolId}/addTeacher", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        OrgTeacherDTO createdTeacher = parseResponseData(result1);
        Long teacherId = createdTeacher.getId();
        Long userId = createdTeacher.getUserId();
        System.out.println("   创建成功: teacherId=" + teacherId + ", userId=" + userId);

        // 2. 查询详情验证聚合查询
        System.out.println("\n步骤2：查询详情验证聚合查询");
        MvcResult result2 = mockMvc.perform(get("/api/auth/schools/{schoolId}/getTeacher/{id}", SCHOOL_ID, teacherId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        OrgTeacherDTO teacherDetail = parseResponseData(result2);
        assertThat(teacherDetail.getName()).isEqualTo("流程测试教师");
        assertThat(teacherDetail.getPhone()).isEqualTo("18800188001");
        assertThat(teacherDetail.getDepartmentName()).isEqualTo("语文教研组");
        System.out.println("   聚合查询成功: userId=" + teacherDetail.getUserId() + ", dept=" + teacherDetail.getDepartmentName());

        // 3. 更新部门
        System.out.println("\n步骤3：更新所属部门");
        UpdateOrgTeacherCommand command3 = UpdateOrgTeacherCommand.builder()
                .orgTeacherId(teacherId)
                .departmentId(DEPARTMENT_ID_2)
                .build();
        mockMvc.perform(post("/api/auth/schools/{schoolId}/updateTeacher", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        System.out.println("   更新成功: 部门从 '语文教研组' 调到 '数学教研组'");

        // 4. 删除教职工
        System.out.println("\n步骤4：删除教职工关联关系");
        mockMvc.perform(post("/api/auth/schools/{schoolId}/deleteTeacher/{id}", SCHOOL_ID, teacherId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        System.out.println("   删除成功: 关联关系已删除，用户数据保留（userId=" + userId + ")");

        // 5. 验证用户仍存在，可以再次创建（复用同一 userId）
        System.out.println("\n步骤5：验证用户数据保留，可再次创建教职工");
        MvcResult result5 = mockMvc.perform(post("/api/auth/schools/{schoolId}/addTeacher", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        OrgTeacherDTO reCreated = parseResponseData(result5);
        assertThat(reCreated.getUserId()).isEqualTo(userId);
        System.out.println("   再次创建成功: 使用已有用户（userId=" + reCreated.getUserId() + ")");

        System.out.println("\n========== 完整业务流程验证通过 ✅ ==========\n");
    }

    // ==================== JSON 解析辅助 ====================

    private OrgTeacherDTO parseResponseData(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var apiType = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, OrgTeacherDTO.class);
        ApiResponse<OrgTeacherDTO> response = objectMapper.readValue(body, apiType);
        return response.getData();
    }

    private PageResult<OrgTeacherDTO> parseResponsePage(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var pageType = objectMapper.getTypeFactory().constructParametricType(PageResult.class, OrgTeacherDTO.class);
        var apiType = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, pageType);
        ApiResponse<PageResult<OrgTeacherDTO>> response = objectMapper.readValue(body, apiType);
        return response.getData();
    }
}
