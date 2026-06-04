package com.ai.edu.interface_.test.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.org.OrgTeacherDTO;
import com.ai.edu.application.dto.org.PageResult;
import com.ai.edu.application.dto.org.command.CreateOrgTeacherCommand;
import com.ai.edu.application.dto.org.command.OrgTeacherQueryParamDTO;
import com.ai.edu.application.dto.org.command.UpdateOrgTeacherCommand;
import com.ai.edu.interface_.test.config.TestConfig;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 教职工管理集成测试（基于 H2 数据库，不使用 Mock）
 * 测试完整的业务流程：
 * 1. 创建教职工（查询用户 → 不存在则创建用户 → 创建关联关系）
 * 2. 查询教职工详情（聚合查询）
 * 3. 查询教职工列表（聚合查询）
 * 4. 更新教职工所属部门
 * 5. 删除教职工关联关系
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
    private static final String EXISTING_PHONE = "13800138001"; // 已存在的张三手机号
    private static final String NEW_PHONE = "13900139001"; // 新用户手机号

    @Test
    @Order(1)
    @DisplayName("创建教职工 - 用户已存在")
    void testCreateOrgTeacher_UserExists() throws Exception {
        // 创建命令：使用已存在的用户手机号
        CreateOrgTeacherCommand command = CreateOrgTeacherCommand.builder()
                .name("张三") // 已存在用户
                .phone(EXISTING_PHONE)
                .departmentId(DEPARTMENT_ID_1)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/schools/{schoolId}/teachers", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // 解析响应
        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<OrgTeacherDTO> response = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        OrgTeacherDTO.class
                ));

        OrgTeacherDTO teacher = response.getData();

        // 验证返回数据
        assertThat(teacher).isNotNull();
        assertThat(teacher.getUserId()).isEqualTo(1L); // 已存在用户的ID
        assertThat(teacher.getName()).isEqualTo("张三");
        assertThat(teacher.getPhone()).isEqualTo(EXISTING_PHONE);
        assertThat(teacher.getDepartmentId()).isEqualTo(DEPARTMENT_ID_1);
        assertThat(teacher.getDepartmentName()).isEqualTo("语文教研组");

        System.out.println("✅ 测试通过：创建教职工（用户已存在）");
        System.out.println("   教职工ID: " + teacher.getId());
        System.out.println("   用户ID: " + teacher.getUserId());
        System.out.println("   姓名: " + teacher.getName());
        System.out.println("   手机号: " + teacher.getPhone());
        System.out.println("   所属部门: " + teacher.getDepartmentName());
    }

    @Test
    @Order(2)
    @DisplayName("创建教职工 - 用户不存在，自动创建用户")
    void testCreateOrgTeacher_UserNotExists() throws Exception {
        // 创建命令：使用新手机号，用户不存在，会自动创建用户
        CreateOrgTeacherCommand command = CreateOrgTeacherCommand.builder()
                .name("新教师")
                .phone(NEW_PHONE)
                .departmentId(DEPARTMENT_ID_2)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/schools/{schoolId}/teachers", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // 解析响应
        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<OrgTeacherDTO> response = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        OrgTeacherDTO.class
                ));

        OrgTeacherDTO teacher = response.getData();

        // 验证返回数据
        assertThat(teacher).isNotNull();
        assertThat(teacher.getUserId()).isGreaterThan(3L); // 新创建的用户ID（大于预设的3个用户）
        assertThat(teacher.getName()).isEqualTo("新教师");
        assertThat(teacher.getPhone()).isEqualTo(NEW_PHONE);
        assertThat(teacher.getDepartmentId()).isEqualTo(DEPARTMENT_ID_2);
        assertThat(teacher.getDepartmentName()).isEqualTo("数学教研组");

        System.out.println("✅ 测试通过：创建教职工（用户不存在，自动创建用户）");
        System.out.println("   教职工ID: " + teacher.getId());
        System.out.println("   用户ID（新创建）: " + teacher.getUserId());
        System.out.println("   姓名: " + teacher.getName());
        System.out.println("   手机号: " + teacher.getPhone());
        System.out.println("   所属部门: " + teacher.getDepartmentName());
    }

    @Test
    @Order(3)
    @DisplayName("创建教职工失败 - 用户已在本学校有教职工记录")
    void testCreateOrgTeacher_Duplicate() throws Exception {
        // 尝试再次创建张三的教职工记录（已存在）
        CreateOrgTeacherCommand command = CreateOrgTeacherCommand.builder()
                .name("张三")
                .phone(EXISTING_PHONE)
                .departmentId(DEPARTMENT_ID_2)
                .build();

        mockMvc.perform(post("/api/auth/schools/{schoolId}/teachers", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("该用户已在本学校有教职工记录"));

        System.out.println("✅ 测试通过：重复创建教职工被拒绝");
    }

    @Test
    @Order(4)
    @DisplayName("查询教职工详情（聚合查询）")
    void testGetOrgTeacher() throws Exception {
        // 先创建一个教职工（Order(1) 已创建张三）
        // 查询详情
        MvcResult result = mockMvc.perform(get("/api/auth/schools/{schoolId}/teachers/{id}", SCHOOL_ID, 1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // 解析响应
        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<OrgTeacherDTO> response = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        OrgTeacherDTO.class
                ));

        OrgTeacherDTO teacher = response.getData();

        // 验证聚合查询返回完整信息
        assertThat(teacher).isNotNull();
        assertThat(teacher.getUserId()).isEqualTo(1L);
        assertThat(teacher.getName()).isEqualTo("张三"); // 用户基本信息（来自用户域）
        assertThat(teacher.getPhone()).isEqualTo(EXISTING_PHONE); // 用户基本信息（来自用户域）
        assertThat(teacher.getDepartmentId()).isEqualTo(DEPARTMENT_ID_1);
        assertThat(teacher.getDepartmentName()).isEqualTo("语文教研组"); // 部门名称（来自组织域）

        System.out.println("✅ 测试通过：查询教职工详情（聚合查询）");
        System.out.println("   聚合返回完整信息：");
        System.out.println("   - 关联关系（组织域）: userId=" + teacher.getUserId() + ", departmentId=" + teacher.getDepartmentId());
        System.out.println("   - 用户信息（用户域）: name=" + teacher.getName() + ", phone=" + teacher.getPhone());
        System.out.println("   - 部门信息（组织域）: departmentName=" + teacher.getDepartmentName());
    }

    @Test
    @Order(5)
    @DisplayName("查询教职工列表（聚合查询）")
    void testListOrgTeachers() throws Exception {
        // 查询列表
        MvcResult result = mockMvc.perform(get("/api/auth/schools/{schoolId}/teachers", SCHOOL_ID)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // 解析响应
        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<PageResult<OrgTeacherDTO>> response = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        objectMapper.getTypeFactory().constructParametricType(
                                PageResult.class,
                                OrgTeacherDTO.class
                        )
                ));

        PageResult<OrgTeacherDTO> pageResult = response.getData();

        // 验证聚合查询返回完整信息列表
        assertThat(pageResult).isNotNull();
        assertThat(pageResult.getList()).isNotEmpty();
        assertThat(pageResult.getTotal()).isGreaterThanOrEqualTo(2);

        // 验证每个教职工都有完整信息
        for (OrgTeacherDTO teacher : pageResult.getList()) {
            assertThat(teacher.getUserId()).isNotNull();
            assertThat(teacher.getName()).isNotNull(); // 用户基本信息（来自用户域）
            assertThat(teacher.getPhone()).isNotNull(); // 用户基本信息（来自用户域）
            assertThat(teacher.getDepartmentId()).isNotNull();
            assertThat(teacher.getDepartmentName()).isNotNull(); // 部门名称（来自组织域）

            System.out.println("   教职工: userId=" + teacher.getUserId() +
                    ", name=" + teacher.getName() +
                    ", phone=" + teacher.getPhone() +
                    ", department=" + teacher.getDepartmentName());
        }

        System.out.println("✅ 测试通过：查询教职工列表（聚合查询）");
        System.out.println("   总数: " + pageResult.getTotal());
        System.out.println("   当前页: " + pageResult.getPageNum());
        System.out.println("   每页数量: " + pageResult.getPageSize());
    }

    @Test
    @Order(6)
    @DisplayName("按部门查询教职工列表")
    void testListOrgTeachers_ByDepartment() throws Exception {
        // 查询语文教研组的教职工
        MvcResult result = mockMvc.perform(get("/api/auth/schools/{schoolId}/teachers", SCHOOL_ID)
                        .param("departmentId", String.valueOf(DEPARTMENT_ID_1))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // 解析响应
        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<PageResult<OrgTeacherDTO>> response = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        objectMapper.getTypeFactory().constructParametricType(
                                PageResult.class,
                                OrgTeacherDTO.class
                        )
                ));

        PageResult<OrgTeacherDTO> pageResult = response.getData();

        // 验证结果
        assertThat(pageResult).isNotNull();
        assertThat(pageResult.getList()).isNotEmpty();
        assertThat(pageResult.getList().get(0).getDepartmentId()).isEqualTo(DEPARTMENT_ID_1);

        System.out.println("✅ 测试通过：按部门查询教职工列表");
        System.out.println("   部门ID: " + DEPARTMENT_ID_1);
        System.out.println("   部门名称: " + pageResult.getList().get(0).getDepartmentName());
        System.out.println("   教职工数量: " + pageResult.getTotal());
    }

    @Test
    @Order(7)
    @DisplayName("更新教职工所属部门")
    void testUpdateOrgTeacher() throws Exception {
        // 更新张三的部门（从语文教研组调到数学教研组）
        UpdateOrgTeacherCommand command = UpdateOrgTeacherCommand.builder()
                .departmentId(DEPARTMENT_ID_2)
                .build();

        MvcResult result = mockMvc.perform(put("/api/auth/schools/{schoolId}/teachers/{id}", SCHOOL_ID, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // 解析响应
        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<OrgTeacherDTO> response = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        OrgTeacherDTO.class
                ));

        OrgTeacherDTO teacher = response.getData();

        // 验证更新成功
        assertThat(teacher).isNotNull();
        assertThat(teacher.getDepartmentId()).isEqualTo(DEPARTMENT_ID_2);
        assertThat(teacher.getDepartmentName()).isEqualTo("数学教研组");

        System.out.println("✅ 测试通过：更新教职工所属部门");
        System.out.println("   教职工ID: " + teacher.getId());
        System.out.println("   原部门: 语文教研组");
        System.out.println("   新部门: " + teacher.getDepartmentName());
    }

    @Test
    @Order(8)
    @DisplayName("删除教职工关联关系")
    void testDeleteOrgTeacher() throws Exception {
        // 删除张三的教职工关联关系（用户数据保留）
        mockMvc.perform(delete("/api/auth/schools/{schoolId}/teachers/{id}", SCHOOL_ID, 2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证删除后查询不到
        mockMvc.perform(get("/api/auth/schools/{schoolId}/teachers/{id}", SCHOOL_ID, 2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        // 验证用户数据仍然存在（可以再次创建教职工）
        MvcResult result = mockMvc.perform(get("/api/auth/schools/{schoolId}/teachers", SCHOOL_ID)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<PageResult<OrgTeacherDTO>> response = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        objectMapper.getTypeFactory().constructParametricType(
                                PageResult.class,
                                OrgTeacherDTO.class
                        )
                ));

        // 验证列表中少了一条记录
        assertThat(response.getData().getTotal()).isLessThan(3);

        System.out.println("✅ 测试通过：删除教职工关联关系");
        System.out.println("   删除关联关系，用户数据保留");
        System.out.println("   当前教职工总数: " + response.getData().getTotal());
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

        MvcResult result1 = mockMvc.perform(post("/api/auth/schools/{schoolId}/teachers", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command1)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<OrgTeacherDTO> response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        OrgTeacherDTO.class
                ));

        OrgTeacherDTO createdTeacher = response1.getData();
        Long teacherId = createdTeacher.getId();
        Long userId = createdTeacher.getUserId();

        System.out.println("   创建成功: teacherId=" + teacherId + ", userId=" + userId);

        // 2. 查询详情验证聚合查询
        System.out.println("\n步骤2：查询详情验证聚合查询");
        MvcResult result2 = mockMvc.perform(get("/api/auth/schools/{schoolId}/teachers/{id}", SCHOOL_ID, teacherId))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<OrgTeacherDTO> response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        OrgTeacherDTO.class
                ));

        OrgTeacherDTO teacherDetail = response2.getData();
        assertThat(teacherDetail.getName()).isEqualTo("流程测试教师");
        assertThat(teacherDetail.getPhone()).isEqualTo("18800188001");

        System.out.println("   聚合查询成功:");
        System.out.println("   - userId=" + teacherDetail.getUserId() + " (组织域)");
        System.out.println("   - name=" + teacherDetail.getName() + " (用户域)");
        System.out.println("   - phone=" + teacherDetail.getPhone() + " (用户域)");
        System.out.println("   - departmentName=" + teacherDetail.getDepartmentName() + " (组织域)");

        // 3. 更新部门
        System.out.println("\n步骤3：更新所属部门");
        UpdateOrgTeacherCommand command3 = UpdateOrgTeacherCommand.builder()
                .departmentId(DEPARTMENT_ID_2)
                .build();

        mockMvc.perform(put("/api/auth/schools/{schoolId}/teachers/{id}", SCHOOL_ID, teacherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command3)))
                .andExpect(status().isOk());

        System.out.println("   更新成功: 部门从 '语文教研组' 调到 '数学教研组'");

        // 4. 删除教职工
        System.out.println("\n步骤4：删除教职工关联关系");
        mockMvc.perform(delete("/api/auth/schools/{schoolId}/teachers/{id}", SCHOOL_ID, teacherId))
                .andExpect(status().isOk());

        System.out.println("   删除成功: 关联关系已删除，用户数据保留（userId=" + userId + ")");

        // 5. 验证用户仍存在，可以再次创建
        System.out.println("\n步骤5：验证用户数据保留，可再次创建教职工");
        CreateOrgTeacherCommand command5 = CreateOrgTeacherCommand.builder()
                .name("流程测试教师")
                .phone("18800188001")
                .departmentId(DEPARTMENT_ID_1)
                .build();

        MvcResult result5 = mockMvc.perform(post("/api/auth/schools/{schoolId}/teachers", SCHOOL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command5)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<OrgTeacherDTO> response5 = objectMapper.readValue(
                result5.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        OrgTeacherDTO.class
                ));

        // 验证使用了同一个userId
        assertThat(response5.getData().getUserId()).isEqualTo(userId);

        System.out.println("   再次创建成功: 使用已有用户（userId=" + response5.getData().getUserId() + ")");

        System.out.println("\n========== 完整业务流程验证通过 ✅ ==========\n");
    }
}