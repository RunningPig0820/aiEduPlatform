package com.ai.edu.domain.organization.model.entity;

import com.ai.edu.domain.shared.valueobject.SchoolId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行政班实体测试
 * 覆盖 DepartmentEdu 实体工厂方法和 Department departmentType 字段
 */
@TestMethodOrder(OrderAnnotation.class)
class AdminClassEntityTest {

    private static final SchoolId SCHOOL_ID = SchoolId.of(1L);
    private static final Long DEPT_ID = 100L;

    // ==================== DepartmentEdu factory methods ====================

    @Test
    @Order(1)
    @DisplayName("DepartmentEdu.createStage() 应正确创建学段扩展属性")
    void departmentEdu_createStage() {
        DepartmentEdu edu = DepartmentEdu.createStage(DEPT_ID, SCHOOL_ID, "PRIMARY", "4");

        assertEquals(DEPT_ID, edu.getDeptId());
        assertEquals(SCHOOL_ID, edu.getSchoolId());
        assertTrue(edu.isStage());
        assertFalse(edu.isGrade());
        assertFalse(edu.isClass());
        assertEquals("PRIMARY", edu.getStageCode());
        assertEquals("4", edu.getStageYearCode());
        assertEquals("", edu.getGradeCode());
        assertEquals("", edu.getEnrollmentYear());
        assertFalse(edu.isDeleted());
    }

    @Test
    @Order(2)
    @DisplayName("DepartmentEdu.createGrade() 应正确创建年级扩展属性")
    void departmentEdu_createGrade() {
        DepartmentEdu edu = DepartmentEdu.createGrade(DEPT_ID, SCHOOL_ID, "PRIMARY", "4", "1", "2024");

        assertEquals(DEPT_ID, edu.getDeptId());
        assertTrue(edu.isGrade());
        assertEquals("PRIMARY", edu.getStageCode());
        assertEquals("4", edu.getStageYearCode());
        assertEquals("1", edu.getGradeCode());
        assertEquals("2024", edu.getEnrollmentYear());
    }

    @Test
    @Order(3)
    @DisplayName("DepartmentEdu.createClass() 应正确创建班级扩展属性")
    void departmentEdu_createClass() {
        DepartmentEdu edu = DepartmentEdu.createClass(DEPT_ID, SCHOOL_ID, "JUNIOR_HIGH", "5", "7", "2023");

        assertEquals(DEPT_ID, edu.getDeptId());
        assertTrue(edu.isClass());
        assertEquals("JUNIOR_HIGH", edu.getStageCode());
        assertEquals("5", edu.getStageYearCode());
        assertEquals("7", edu.getGradeCode());
        assertEquals("2023", edu.getEnrollmentYear());
    }

    @Test
    @Order(4)
    @DisplayName("DepartmentEdu.update() 应正确更新可修改字段")
    void departmentEdu_update() {
        DepartmentEdu edu = DepartmentEdu.createStage(DEPT_ID, SCHOOL_ID, "PRIMARY", "4");
        edu.update("JUNIOR_HIGH", "5", "7", "2024");

        assertEquals("JUNIOR_HIGH", edu.getStageCode());
        assertEquals("5", edu.getStageYearCode());
        assertEquals("7", edu.getGradeCode());
        assertEquals("2024", edu.getEnrollmentYear());
    }

    @Test
    @Order(5)
    @DisplayName("DepartmentEdu.update() 对 null 参数应保留原值")
    void departmentEdu_update_shouldKeepOldValueOnNull() {
        DepartmentEdu edu = DepartmentEdu.createGrade(DEPT_ID, SCHOOL_ID, "PRIMARY", "4", "1", "2024");
        edu.update(null, null, null, null);

        assertEquals("PRIMARY", edu.getStageCode());
        assertEquals("4", edu.getStageYearCode());
        assertEquals("1", edu.getGradeCode());
        assertEquals("2024", edu.getEnrollmentYear());
    }

    @Test
    @Order(6)
    @DisplayName("DepartmentEdu.delete() 和 restore() 应正确切换删除状态")
    void departmentEdu_deleteAndRestore() {
        DepartmentEdu edu = DepartmentEdu.createClass(DEPT_ID, SCHOOL_ID, "PRIMARY", "4", "1", "2024");

        assertFalse(edu.isDeleted());
        edu.delete();
        assertTrue(edu.isDeleted());
        edu.restore();
        assertFalse(edu.isDeleted());
    }

    @Test
    @Order(7)
    @DisplayName("DepartmentEdu.setId() 重复设置应抛出异常")
    void departmentEdu_setId_twiceShouldThrow() {
        DepartmentEdu edu = DepartmentEdu.createStage(DEPT_ID, SCHOOL_ID, "PRIMARY", "4");
        edu.setId(com.ai.edu.domain.organization.model.valueobject.DepartmentEduId.of(1L));
        assertThrows(IllegalStateException.class, () ->
                edu.setId(com.ai.edu.domain.organization.model.valueobject.DepartmentEduId.of(2L)));
    }

    // ==================== Department departmentType ====================

    @Test
    @Order(10)
    @DisplayName("Department.createRoot() 应默认 departmentType = ORG")
    void department_createRoot_defaultOrg() {
        Department dept = Department.createRoot(SCHOOL_ID, "教务处", 0, "test");

        assertTrue(dept.isOrg());
        assertFalse(dept.isAdminClass());
        assertEquals("ORG", dept.getDepartmentTypeValue());
    }

    @Test
    @Order(11)
    @DisplayName("Department.createAdminClassRoot() 应设置 departmentType = ADMIN_CLASS")
    void department_createAdminClassRoot() {
        Department dept = Department.createAdminClassRoot(SCHOOL_ID, "小学部", 0, null);

        assertTrue(dept.isAdminClass());
        assertFalse(dept.isOrg());
        assertEquals("ADMIN_CLASS", dept.getDepartmentTypeValue());
    }

    @Test
    @Order(12)
    @DisplayName("Department.createAdminClassChild() 应设置 parentId 和 ADMIN_CLASS 类型")
    void department_createAdminClassChild() {
        Department parent = Department.createAdminClassRoot(SCHOOL_ID, "小学部", 0, null);
        Department child = Department.createAdminClassChild(SCHOOL_ID, "2024级", parent, 0, null);

        assertEquals(parent.getIdValue(), child.getParentId());
        assertTrue(child.isAdminClass());
        assertEquals(parent.getDepartmentPath(), child.getDepartmentPath());
    }

    @Test
    @Order(13)
    @DisplayName("Department.fromPO() null departmentType 应默认 ORG")
    void department_fromPO_nullTypeDefaultsToOrg() {
        Department dept = Department.fromPO(1L, SCHOOL_ID, "test", null, "1", null, 0, "", 0L, 0L, null, null, false);

        assertTrue(dept.isOrg());
        assertEquals("ORG", dept.getDepartmentTypeValue());
    }
}
