package com.ai.edu.interfaces.test.api;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.org.AdminClassStudentDTO;
import com.ai.edu.application.dto.org.command.CreateAdminClassStudentCommand;
import com.ai.edu.application.dto.org.command.ParentCommand;
import com.ai.edu.application.dto.org.command.UpdateAdminClassStudentCommand;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WithMockUser(username = "admin", roles = {"ADMIN"})
class AdminClassStudentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long SCHOOL_ID = 1L;
    private static final Long CLASS_DEPT_ID_1 = 12L;
    private static final Long CLASS_DEPT_ID_2 = 13L;
    private static final Long GRADE_DEPT_ID = 11L;
    private static final String EXISTING_STUDENT_PHONE = "13800000001";
    private static final String EXISTING_PARENT_PHONE = "13900000001";
    private static final String NEW_STUDENT_PHONE = "13700000001";
    private static final String NEW_PARENT_PHONE = "13600000001";

    // ==================== 添加学生 ====================

    @Test
    @Order(1)
    @DisplayName("添加学生 - 学生用户已存在，绑定已有家长")
    void testAddStudent_ExistingUser_WithExistingParent() throws Exception {
        CreateAdminClassStudentCommand command = CreateAdminClassStudentCommand.builder()
                .name("测试学生A")
                .phone(EXISTING_STUDENT_PHONE)
                .idCard("110101200001011234")
                .studentNo("2024001")
                .parents(List.of(
                        ParentCommand.builder().name("测试家长X").phone(EXISTING_PARENT_PHONE).relationship("父亲").build()
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        AdminClassStudentDTO student = parseResponseData(result, AdminClassStudentDTO.class);

        assertThat(student).isNotNull();
        assertThat(student.getStudentUserId()).isEqualTo(10L);
        assertThat(student.getName()).isEqualTo("测试学生A");
        assertThat(student.getPhone()).isEqualTo(EXISTING_STUDENT_PHONE);
        assertThat(student.getMaskedIdCard()).isNotEmpty();
        assertThat(student.getMaskedIdCard()).contains("*");
        assertThat(student.getStudentNo()).isEqualTo("2024001");
        assertThat(student.getDeptId()).isEqualTo(CLASS_DEPT_ID_1);
        assertThat(student.getStatus()).isEqualTo("ACTIVE");
        assertThat(student.getParents()).isNotEmpty();
        assertThat(student.getParents().get(0).getUserId()).isEqualTo(20L);

        System.out.println("PASS: 添加学生成功（用户+家长均已存在）studentUserId=" + student.getStudentUserId());
    }

    @Test
    @Order(2)
    @DisplayName("添加学生 - 学生和家长都不存在，自动创建")
    void testAddStudent_NewUser_WithNewParents() throws Exception {
        CreateAdminClassStudentCommand command = CreateAdminClassStudentCommand.builder()
                .name("新学生")
                .phone(NEW_STUDENT_PHONE)
                .idCard("440101201001015678")
                .studentNo("2024002")
                .parents(List.of(
                        ParentCommand.builder().name("新家长1").phone(NEW_PARENT_PHONE).relationship("母亲").build(),
                        ParentCommand.builder().name("新家长2").phone("13600000002").relationship("父亲").build()
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        AdminClassStudentDTO student = parseResponseData(result, AdminClassStudentDTO.class);

        assertThat(student).isNotNull();
        assertThat(student.getStudentUserId()).isGreaterThan(11L);
        assertThat(student.getName()).isEqualTo("新学生");
        assertThat(student.getPhone()).isEqualTo(NEW_STUDENT_PHONE);
        assertThat(student.getMaskedIdCard()).contains("*");
        assertThat(student.getStatus()).isEqualTo("ACTIVE");
        assertThat(student.getParents()).hasSize(2);

        System.out.println("PASS: 添加学生成功（新用户+新家长自动创建）studentUserId=" + student.getStudentUserId());
    }

    @Test
    @Order(3)
    @DisplayName("添加学生失败 - 重复添加到同一班级")
    void testAddStudent_Duplicate() throws Exception {
        CreateAdminClassStudentCommand command = CreateAdminClassStudentCommand.builder()
                .name("测试学生A")
                .phone(EXISTING_STUDENT_PHONE)
                .idCard("110101200001011234")
                .build();

        mockMvc.perform(post("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("80011"));

        System.out.println("PASS: 重复添加被正确拒绝");
    }

    @Test
    @Order(4)
    @DisplayName("添加学生失败 - 手机号被非STUDENT角色占用")
    void testAddStudent_PhoneUsedByOtherRole() throws Exception {
        CreateAdminClassStudentCommand command = CreateAdminClassStudentCommand.builder()
                .name("张三")
                .phone("13800138001")
                .idCard("110101200001011234")
                .build();

        mockMvc.perform(post("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value("10001"));

        System.out.println("PASS: 手机号被其他角色占用，正确拒绝");
    }

    @Test
    @Order(5)
    @DisplayName("添加学生失败 - 行政班节点无效")
    void testAddStudent_InvalidClassNode() throws Exception {
        CreateAdminClassStudentCommand command = CreateAdminClassStudentCommand.builder()
                .name("测试")
                .phone("13700000009")
                .idCard("110101200001011234")
                .build();

        mockMvc.perform(post("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, 2L)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value("80010"));

        System.out.println("PASS: 无效行政班节点，正确拒绝");
    }

    // ==================== 查询学生列表 ====================

    @Test
    @Order(6)
    @DisplayName("查询学生列表 - 查询单个班级")
    void testListStudents_SingleClass() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        List<AdminClassStudentDTO> students = parseResponseList(result, AdminClassStudentDTO.class);

        assertThat(students).isNotEmpty();
        assertThat(students.size()).isGreaterThanOrEqualTo(2);
        for (AdminClassStudentDTO s : students) {
            assertThat(s.getStudentUserId()).isNotNull();
            assertThat(s.getName()).isNotNull();
            assertThat(s.getPhone()).isNotNull();
            assertThat(s.getDeptName()).isNotNull();
        }

        System.out.println("PASS: 查询一班学生列表: " + students.size() + " 人");
    }

    @Test
    @Order(7)
    @DisplayName("查询学生列表 - 查询年级（递归查所有子班级）")
    void testListStudents_GradeWithDescendants() throws Exception {
        CreateAdminClassStudentCommand command = CreateAdminClassStudentCommand.builder()
                .name("测试学生B")
                .phone("13800000002")
                .idCard("440101201001019999")
                .studentNo("2024003")
                .build();

        mockMvc.perform(post("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_2)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, GRADE_DEPT_ID)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        List<AdminClassStudentDTO> students = parseResponseList(result, AdminClassStudentDTO.class);

        assertThat(students).isNotEmpty();
        assertThat(students.size()).isGreaterThanOrEqualTo(3);
        boolean hasClass1 = students.stream().anyMatch(s -> "一班".equals(s.getDeptName()));
        boolean hasClass2 = students.stream().anyMatch(s -> "二班".equals(s.getDeptName()));
        assertThat(hasClass1).isTrue();
        assertThat(hasClass2).isTrue();

        System.out.println("PASS: 查询一年级（递归）: " + students.size() + " 人（一班+二班）");
    }

    // ==================== 修改学生 ====================

    @Test
    @Order(8)
    @DisplayName("修改学生 - 更新学号")
    void testUpdateStudent_StudentNo() throws Exception {
        // 创建一个学生用于更新
        CreateAdminClassStudentCommand createCmd = CreateAdminClassStudentCommand.builder()
                .name("学号更新测试").phone("13788888888").idCard("110101200001019999").build();
        MvcResult cr = mockMvc.perform(post("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isOk()).andReturn();
        Long sid = parseResponseData(cr, AdminClassStudentDTO.class).getId();

        UpdateAdminClassStudentCommand command = UpdateAdminClassStudentCommand.builder()
                .id(sid)
                .studentNo("2024-UPDATED")
                .build();

        MvcResult result = mockMvc.perform(put("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        AdminClassStudentDTO student = parseResponseData(result, AdminClassStudentDTO.class);
        assertThat(student.getStudentNo()).isEqualTo("2024-UPDATED");

        System.out.println("PASS: 学号更新成功: " + student.getStudentNo());
    }

    @Test
    @Order(9)
    @DisplayName("修改学生 - 毕业操作")
    void testUpdateStudent_Graduate() throws Exception {
        UpdateAdminClassStudentCommand command = UpdateAdminClassStudentCommand.builder()
                .id(1L)
                .status("GRADUATED")
                .build();

        MvcResult result = mockMvc.perform(put("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        AdminClassStudentDTO student = parseResponseData(result, AdminClassStudentDTO.class);
        assertThat(student.getStatus()).isEqualTo("GRADUATED");

        // 验证已毕业不出现在列表中
        MvcResult listResult = mockMvc.perform(get("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<AdminClassStudentDTO> activeStudents = parseResponseList(listResult, AdminClassStudentDTO.class);
        assertThat(activeStudents.stream().noneMatch(s -> s.getId().equals(1L))).isTrue();

        System.out.println("PASS: 毕业操作成功，已毕业学生不在在读列表中");
    }

    @Test
    @Order(10)
    @DisplayName("修改学生 - 恢复在读")
    void testUpdateStudent_Reactivate() throws Exception {
        UpdateAdminClassStudentCommand command = UpdateAdminClassStudentCommand.builder()
                .id(1L)
                .status("ACTIVE")
                .build();

        MvcResult result = mockMvc.perform(put("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        AdminClassStudentDTO student = parseResponseData(result, AdminClassStudentDTO.class);
        assertThat(student.getStatus()).isEqualTo("ACTIVE");

        System.out.println("PASS: 恢复在读成功");
    }

    // ==================== 删除学生 ====================

    @Test
    @Order(11)
    @DisplayName("删除学生关联 - 用户数据保留")
    void testDeleteStudent() throws Exception {
        // 先创建一个学生用于删除
        CreateAdminClassStudentCommand createCmd = CreateAdminClassStudentCommand.builder()
                .name("待删除学生")
                .phone("13777777777")
                .idCard("110101200001019999")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isOk())
                .andReturn();

        AdminClassStudentDTO created = parseResponseData(createResult, AdminClassStudentDTO.class);
        Long idToDelete = created.getId();

        // 删除


        mockMvc.perform(delete("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students/{id}",
                        SCHOOL_ID, CLASS_DEPT_ID_1, idToDelete)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        // 验证重新用同一手机号可添加（用户数据保留）
        CreateAdminClassStudentCommand command = CreateAdminClassStudentCommand.builder()
                .name("测试学生A")
                .phone(EXISTING_STUDENT_PHONE)
                .idCard("110101200001011234")
                .build();

        MvcResult reAddResult = mockMvc.perform(post("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        AdminClassStudentDTO reAdded = parseResponseData(reAddResult, AdminClassStudentDTO.class);
        assertThat(reAdded.getStudentUserId()).isEqualTo(10L);

        System.out.println("PASS: 删除关联成功，用户数据保留，重新添加复用同一 userId=" + reAdded.getStudentUserId());
    }

    // ==================== 完整流程 ====================

    @Test
    @Order(12)
    @DisplayName("完整业务流程")
    void testCompleteBusinessFlow() throws Exception {
        System.out.println("\n=== 行政班学生完整业务流程 ===");

        CreateAdminClassStudentCommand cmd = CreateAdminClassStudentCommand.builder()
                .name("流程学生")
                .phone("13500000001")
                .idCard("330101201501016666")
                .studentNo("FLOW001")
                .parents(List.of(
                        ParentCommand.builder().name("流程家长").phone("13400000001").relationship("母亲").build()
                ))
                .build();

        // 1. 添加
        MvcResult r1 = mockMvc.perform(post("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        AdminClassStudentDTO created = parseResponseData(r1, AdminClassStudentDTO.class);
        Long createdId = created.getId();
        Long createdUserId = created.getStudentUserId();
        assertThat(created.getName()).isEqualTo("流程学生");
        assertThat(created.getParents()).hasSize(1);
        System.out.println("1. 添加成功: id=" + createdId);

        // 2. 查询
        MvcResult r2 = mockMvc.perform(get("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        List<AdminClassStudentDTO> list = parseResponseList(r2, AdminClassStudentDTO.class);
        assertThat(list.stream().anyMatch(s -> s.getId().equals(createdId))).isTrue();
        System.out.println("2. 列表查询找到该学生");

        // 3. 修改学号
        UpdateAdminClassStudentCommand updateCmd = UpdateAdminClassStudentCommand.builder()
                .id(createdId).studentNo("FLOW-UPDATED").build();
        mockMvc.perform(put("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCmd)))
                .andExpect(status().isOk());
        System.out.println("3. 修改学号成功");

        // 4. 毕业
        UpdateAdminClassStudentCommand graduateCmd = UpdateAdminClassStudentCommand.builder()
                .id(createdId).status("GRADUATED").build();
        mockMvc.perform(put("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(graduateCmd)))
                .andExpect(status().isOk());
        System.out.println("4. 毕业成功");

        // 5. 删除
        mockMvc.perform(delete("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students/{id}",
                        SCHOOL_ID, CLASS_DEPT_ID_1, createdId)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        System.out.println("5. 删除成功");

        // 6. 重新添加（验证用户保留）
        MvcResult r6 = mockMvc.perform(post("/api/auth/schools/{schoolId}/admin-classes/{deptId}/students",
                        SCHOOL_ID, CLASS_DEPT_ID_1)
                        .characterEncoding("UTF-8").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andReturn();
        AdminClassStudentDTO reAdded = parseResponseData(r6, AdminClassStudentDTO.class);
        assertThat(reAdded.getStudentUserId()).isEqualTo(createdUserId);
        System.out.println("6. 重新添加复用同一 userId=" + createdUserId);

        System.out.println("=== 完整业务流程验证通过 ===\n");
    }

    // ==================== JSON 解析辅助 ====================

    @SuppressWarnings("unchecked")
    private <T> T parseResponseData(MvcResult result, Class<T> dataType) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var apiType = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, dataType);
        ApiResponse<T> response = objectMapper.readValue(body, apiType);
        return response.getData();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> parseResponseList(MvcResult result, Class<T> itemType) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var listType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
        var apiType = objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, listType);
        ApiResponse<List<T>> response = objectMapper.readValue(body, apiType);
        return response.getData();
    }
}
