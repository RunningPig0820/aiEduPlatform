package com.ai.edu.domain.organization.model.aggregate;

import com.ai.edu.domain.organization.model.entity.Department;
import com.ai.edu.domain.organization.model.entity.DepartmentEdu;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行政班聚合根测试
 * 覆盖 AdminClassAggregate 的创建、操作和校验
 */
@TestMethodOrder(OrderAnnotation.class)
class AdminClassAggregateTest {

    private static final SchoolId SCHOOL_ID = SchoolId.of(1L);

    // ==================== 创建测试 ====================

    @Test
    @Order(1)
    @DisplayName("createStage() 应正确创建学段聚合")
    void createStage() {
        AdminClassAggregate aggregate = AdminClassAggregate.createStage(
                SCHOOL_ID, "小学部", "PRIMARY", "4", 0, "小学部测试");

        assertNotNull(aggregate.getDepartment());
        assertNotNull(aggregate.getDepartmentEdu());
        assertEquals("小学部", aggregate.getName());
        assertTrue(aggregate.isStage());
        assertEquals("PRIMARY", aggregate.getDepartmentEdu().getStageCode());
        assertEquals("4", aggregate.getDepartmentEdu().getStageYearCode());
        assertTrue(aggregate.getDepartment().isAdminClass());
    }

    @Test
    @Order(2)
    @DisplayName("createGrade() 应正确创建年级聚合（含父节点）")
    void createGrade() {
        Department parentStage = Department.createAdminClassRoot(SCHOOL_ID, "小学部", 0, null);
        AdminClassAggregate aggregate = AdminClassAggregate.createGrade(
                SCHOOL_ID, "2024级", parentStage, "PRIMARY", "4", "1", "2024", 0, null);

        assertTrue(aggregate.isGrade());
        assertEquals("1", aggregate.getDepartmentEdu().getGradeCode());
        assertEquals("2024", aggregate.getDepartmentEdu().getEnrollmentYear());
        assertEquals(parentStage.getIdValue(), aggregate.getDepartment().getParentId());
    }

    @Test
    @Order(3)
    @DisplayName("createClass() 应正确创建班级聚合（含父节点）")
    void createClass() {
        Department parentGrade = Department.createAdminClassRoot(SCHOOL_ID, "2024级", 0, null);
        AdminClassAggregate aggregate = AdminClassAggregate.createClass(
                SCHOOL_ID, "1班", parentGrade, "PRIMARY", "4", "1", "2024", 0, null);

        assertTrue(aggregate.isClass());
        assertEquals("1班", aggregate.getName());
        assertEquals(parentGrade.getIdValue(), aggregate.getDepartment().getParentId());
    }

    @Test
    @Order(4)
    @DisplayName("from() 应从已有 Department + DepartmentEdu 重建聚合")
    void from() {
        Department dept = Department.createAdminClassRoot(SCHOOL_ID, "小学部", 0, null);
        DepartmentEdu edu = DepartmentEdu.createStage(null, SCHOOL_ID, "PRIMARY", "4");
        AdminClassAggregate aggregate = AdminClassAggregate.from(dept, edu);

        assertEquals(dept, aggregate.getDepartment());
        assertEquals(edu, aggregate.getDepartmentEdu());
    }

    // ==================== 校验测试 ====================

    @Test
    @Order(10)
    @DisplayName("AdminClassAggregate(null) 应抛异常")
    void constructor_shouldThrowOnNull() {
        assertThrows(IllegalArgumentException.class, () -> new AdminClassAggregate(null));
    }

    @Test
    @Order(11)
    @DisplayName("AdminClassAggregate(ORG部门) 应抛异常")
    void constructor_shouldThrowOnOrgDepartment() {
        Department orgDept = Department.createRoot(SCHOOL_ID, "教务处", 0, null);
        assertThrows(IllegalArgumentException.class, () -> new AdminClassAggregate(orgDept));
    }

    // ==================== 操作测试 ====================

    @Test
    @Order(15)
    @DisplayName("delete() 应同时软删除 Department 和 DepartmentEdu")
    void delete() {
        AdminClassAggregate aggregate = AdminClassAggregate.createStage(
                SCHOOL_ID, "小学部", "PRIMARY", "4", 0, null);

        aggregate.delete();
        assertTrue(aggregate.getDepartment().isDeleted());
        assertTrue(aggregate.getDepartmentEdu().isDeleted());
    }

    @Test
    @Order(16)
    @DisplayName("updateInfo() 应更新 Department 基本信息")
    void updateInfo() {
        AdminClassAggregate aggregate = AdminClassAggregate.createStage(
                SCHOOL_ID, "小学部", "PRIMARY", "4", 0, null);

        aggregate.updateInfo("中学部", 1, "更新描述");
        assertEquals("中学部", aggregate.getName());
        assertEquals("更新描述", aggregate.getDepartment().getDescription());
    }

    @Test
    @Order(17)
    @DisplayName("updateEduInfo() 应更新 DepartmentEdu 属性")
    void updateEduInfo() {
        AdminClassAggregate aggregate = AdminClassAggregate.createStage(
                SCHOOL_ID, "小学部", "PRIMARY", "4", 0, null);

        aggregate.updateEduInfo("JUNIOR_HIGH", "5", "7", "2024");
        assertEquals("JUNIOR_HIGH", aggregate.getDepartmentEdu().getStageCode());
        assertEquals("5", aggregate.getDepartmentEdu().getStageYearCode());
        assertEquals("7", aggregate.getDepartmentEdu().getGradeCode());
        assertEquals("2024", aggregate.getDepartmentEdu().getEnrollmentYear());
    }

    @Test
    @Order(18)
    @DisplayName("updateEduInfo() edu 未加载时抛异常")
    void updateEduInfo_shouldThrowWhenEduNotLoaded() {
        Department dept = Department.createAdminClassRoot(SCHOOL_ID, "小学部", 0, null);
        AdminClassAggregate aggregate = new AdminClassAggregate(dept);
        assertThrows(IllegalStateException.class, () ->
                aggregate.updateEduInfo("JUNIOR_HIGH", null, null, null));
    }

    @Test
    @Order(19)
    @DisplayName("moveTo() 应更新父节点")
    void moveTo() {
        AdminClassAggregate aggregate = AdminClassAggregate.createStage(
                SCHOOL_ID, "小学部", "PRIMARY", "4", 0, null);
        Department newParent = Department.createAdminClassRoot(SCHOOL_ID, "新学段", 0, null);
        // 模拟已保存的父节点（设置 ID）
        newParent.setId(com.ai.edu.domain.organization.model.valueobject.DepartmentId.of(200L));

        aggregate.moveTo(newParent);
        assertEquals(newParent.getIdValue(), aggregate.getDepartment().getParentId());
    }
}
