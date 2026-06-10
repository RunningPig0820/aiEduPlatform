package com.ai.edu.interfaces.api.org;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.org.AdminClassNodeDTO;
import com.ai.edu.application.dto.org.command.CreateAdminClassNodeCommand;
import com.ai.edu.application.dto.org.command.UpdateAdminClassNodeCommand;
import com.ai.edu.interfaces.test.config.TestConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 行政班 API 集成测试
 * 使用 H2 数据库 + MockMvc 测试完整 HTTP 流程
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestMethodOrder(OrderAnnotation.class)
@WithMockUser(username = "admin", roles = {"ADMIN"})
class AdminClassControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long SCHOOL_ID = 1L;
    private static Long stageDeptId;
    private static Long gradeDeptId;
    private static Long classDeptId;

    // ==================== POST: 创建节点 ====================

    @Test
    @Order(1)
    @DisplayName("POST /api/admin-classes 创建学段节点")
    void createStageNode() throws Exception {
        CreateAdminClassNodeCommand command = CreateAdminClassNodeCommand.builder()
                .name("小学部")
                .schoolId(SCHOOL_ID)
                .deptType(3)
                .stageCode("PRIMARY")
                .stageYearCode("4")
                .sortOrder(0)
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.name").value("小学部"))
                .andExpect(jsonPath("$.data.deptType").value(3))
                .andReturn();

        ApiResponse<AdminClassNodeDTO> response = parseResponse(result, AdminClassNodeDTO.class);
        assertThat(response.getData()).isNotNull();
        stageDeptId = response.getData().getDeptId();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/admin-classes 创建年级节点")
    void createGradeNode() throws Exception {
        CreateAdminClassNodeCommand command = CreateAdminClassNodeCommand.builder()
                .name("2024级")
                .schoolId(SCHOOL_ID)
                .parentId(stageDeptId)
                .deptType(4)
                .stageCode("PRIMARY")
                .stageYearCode("4")
                .gradeCode("1")
                .enrollmentYear("2024")
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.parentId").value(stageDeptId.intValue()))
                .andReturn();

        ApiResponse<AdminClassNodeDTO> response = parseResponse(result, AdminClassNodeDTO.class);
        gradeDeptId = response.getData().getDeptId();
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/admin-classes 创建班级节点")
    void createClassNode() throws Exception {
        CreateAdminClassNodeCommand command = CreateAdminClassNodeCommand.builder()
                .name("1班")
                .schoolId(SCHOOL_ID)
                .parentId(gradeDeptId)
                .deptType(5)
                .stageCode("PRIMARY")
                .stageYearCode("4")
                .gradeCode("1")
                .enrollmentYear("2024")
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.deptType").value(5))
                .andReturn();

        ApiResponse<AdminClassNodeDTO> response = parseResponse(result, AdminClassNodeDTO.class);
        classDeptId = response.getData().getDeptId();
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/admin-classes 参数缺失应返回 10003")
    void createWithMissingParams() throws Exception {
        CreateAdminClassNodeCommand command = CreateAdminClassNodeCommand.builder()
                .name("错误节点")
                .build();

        mockMvc.perform(post("/api/admin-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/admin-classes 无效 deptType 应抛异常")
    void createWithInvalidDeptType() throws Exception {
        CreateAdminClassNodeCommand command = CreateAdminClassNodeCommand.builder()
                .name("错误节点")
                .schoolId(SCHOOL_ID)
                .deptType(99)
                .stageCode("PRIMARY")
                .stageYearCode("4")
                .build();

        mockMvc.perform(post("/api/admin-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("10003"));
    }

    // ==================== GET: 查询 ====================

    @Test
    @Order(6)
    @DisplayName("GET /api/admin-classes?schoolId= 查询行政班树")
    void getNodeTree() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin-classes")
                        .param("schoolId", String.valueOf(SCHOOL_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();

        ApiResponse<List<AdminClassNodeDTO>> response = parseResponseList(result);
        assertThat(response.getData()).isNotEmpty();
        AdminClassNodeDTO stage = response.getData().get(0);
        assertThat(stage.getChildren()).isNotEmpty();
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/admin-classes/{id} 查询节点详情")
    void getNodeDetail() throws Exception {
        mockMvc.perform(get("/api/admin-classes/" + stageDeptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.name").value("小学部"))
                .andExpect(jsonPath("$.data.stageCode").value("PRIMARY"));
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/admin-classes/{id} 不存在节点应返回 10002")
    void getNodeDetail_notFound() throws Exception {
        mockMvc.perform(get("/api/admin-classes/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("80001"));
    }

    // ==================== PUT: 更新 ====================

    @Test
    @Order(9)
    @DisplayName("PUT /api/admin-classes/{id} 更新节点")
    void updateNode() throws Exception {
        UpdateAdminClassNodeCommand command = UpdateAdminClassNodeCommand.builder()
                .name("2025级")
                .gradeCode("2")
                .enrollmentYear("2025")
                .build();

        mockMvc.perform(put("/api/admin-classes/" + gradeDeptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.name").value("2025级"))
                .andExpect(jsonPath("$.data.gradeCode").value("2"));
    }

    @Test
    @Order(10)
    @DisplayName("PUT /api/admin-classes/{id} 不存在节点应返回 10002")
    void updateNode_notFound() throws Exception {
        UpdateAdminClassNodeCommand command = UpdateAdminClassNodeCommand.builder()
                .name("不存在节点")
                .build();

        mockMvc.perform(put("/api/admin-classes/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("80001"));
    }

    // ==================== DELETE: 删除 ====================

    @Test
    @Order(11)
    @DisplayName("DELETE /api/admin-classes/{id} 删除有子节点的节点应失败")
    void deleteNode_withChildren() throws Exception {
        mockMvc.perform(delete("/api/admin-classes/" + stageDeptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("10001"));
    }

    @Test
    @Order(12)
    @DisplayName("DELETE /api/admin-classes/{id} 删除叶子节点应成功")
    void deleteLeafNode() throws Exception {
        mockMvc.perform(delete("/api/admin-classes/" + classDeptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        // 确认已删除
        mockMvc.perform(get("/api/admin-classes/" + classDeptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("80001"));
    }

    // ==================== 辅助方法 ====================

    private <T> ApiResponse<T> parseResponse(MvcResult result, Class<T> dataClass) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, dataClass));
    }

    private ApiResponse<List<AdminClassNodeDTO>> parseResponseList(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class,
                        objectMapper.getTypeFactory().constructCollectionLikeType(List.class, AdminClassNodeDTO.class)));
    }
}
