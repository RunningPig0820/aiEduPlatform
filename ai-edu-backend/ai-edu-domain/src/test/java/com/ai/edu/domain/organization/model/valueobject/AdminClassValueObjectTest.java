package com.ai.edu.domain.organization.model.valueobject;

import com.ai.edu.domain.organization.model.valueobject.enums.DepartmentTypeEnum;
import com.ai.edu.domain.organization.model.valueobject.enums.DeptEduTypeEnum;
import com.ai.edu.domain.organization.model.valueobject.enums.StageYearCodeEnum;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行政班值对象测试
 * 覆盖 DepartmentType, DeptEduType, StageYearCode 的创建和校验
 */
@TestMethodOrder(OrderAnnotation.class)
class AdminClassValueObjectTest {

    // ==================== DepartmentType ====================

    @Test
    @Order(1)
    @DisplayName("DepartmentType.org() 应返回 ORG 类型")
    void departmentType_org_shouldReturnOrg() {
        DepartmentTypeEnum type = DepartmentTypeEnum.ORG;
        assertEquals("ORG", type.getValue());
        assertTrue(type.isOrg());
        assertFalse(type.isAdminClass());
        assertEquals("普通组织部门", type.getDescription());
    }

    @Test
    @Order(2)
    @DisplayName("DepartmentType.adminClass() 应返回 ADMIN_CLASS 类型")
    void departmentType_adminClass_shouldReturnAdminClass() {
        DepartmentTypeEnum type = DepartmentTypeEnum.ADMIN_CLASS;
        assertEquals("ADMIN_CLASS", type.getValue());
        assertTrue(type.isAdminClass());
        assertFalse(type.isOrg());
        assertEquals("行政班节点", type.getDescription());
    }

    @Test
    @Order(3)
    @DisplayName("DepartmentType.of() 合法值应正确解析")
    void departmentType_of_shouldParseValidValues() {
        assertEquals("ORG", DepartmentTypeEnum.of("ORG").getValue());
        assertEquals("ADMIN_CLASS", DepartmentTypeEnum.of("ADMIN_CLASS").getValue());
    }

    @Test
    @Order(4)
    @DisplayName("DepartmentType.of(null) 应抛出异常")
    void departmentType_of_shouldThrowOnNull() {
        assertThrows(IllegalArgumentException.class, () -> DepartmentTypeEnum.of(null));
    }

    @Test
    @Order(5)
    @DisplayName("DepartmentType.of(空字符串) 应抛出异常")
    void departmentType_of_shouldThrowOnBlank() {
        assertThrows(IllegalArgumentException.class, () -> DepartmentTypeEnum.of(""));
    }

    @Test
    @Order(6)
    @DisplayName("DepartmentType.of(非法值) 应抛出异常")
    void departmentType_of_shouldThrowOnInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DepartmentTypeEnum.of("INVALID"));
    }

    @Test
    @Order(7)
    @DisplayName("DepartmentType equals 和 hashCode 应一致")
    void departmentType_equalsAndHashCode() {
        DepartmentTypeEnum t1 = DepartmentTypeEnum.of("ORG");
        DepartmentTypeEnum t2 = DepartmentTypeEnum.ORG;
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    // ==================== DeptEduType ====================

    @Test
    @Order(10)
    @DisplayName("DeptEduType.STAGE 应返回值 3")
    void deptEduType_stage_shouldReturn3() {
        DeptEduTypeEnum type = DeptEduTypeEnum.STAGE;
        assertEquals(3, type.getValue());
        assertTrue(type.isStage());
        assertFalse(type.isGrade());
        assertFalse(type.isClass());
        assertFalse(type.requiresGradeInfo());
        assertEquals("学段", type.getDescription());
    }

    @Test
    @Order(11)
    @DisplayName("DeptEduType.GRADE 应返回值 4")
    void deptEduType_grade_shouldReturn4() {
        DeptEduTypeEnum type = DeptEduTypeEnum.GRADE;
        assertEquals(4, type.getValue());
        assertFalse(type.isStage());
        assertTrue(type.isGrade());
        assertFalse(type.isClass());
        assertTrue(type.requiresGradeInfo());
        assertEquals("年级", type.getDescription());
    }

    @Test
    @Order(12)
    @DisplayName("DeptEduType.CLASS 应返回值 5")
    void deptEduType_class_shouldReturn5() {
        DeptEduTypeEnum type = DeptEduTypeEnum.CLASS;
        assertEquals(5, type.getValue());
        assertFalse(type.isStage());
        assertFalse(type.isGrade());
        assertTrue(type.isClass());
        assertTrue(type.requiresGradeInfo());
        assertEquals("班级", type.getDescription());
    }

    @Test
    @Order(13)
    @DisplayName("DeptEduType.of() 合法值应正确解析")
    void deptEduType_of_shouldParseValidValues() {
        assertEquals(3, DeptEduTypeEnum.of(3).getValue());
        assertEquals(4, DeptEduTypeEnum.of(4).getValue());
        assertEquals(5, DeptEduTypeEnum.of(5).getValue());
    }

    @Test
    @Order(14)
    @DisplayName("DeptEduType.of(null) 应抛出异常")
    void deptEduType_of_shouldThrowOnNull() {
        assertThrows(IllegalArgumentException.class, () -> DeptEduTypeEnum.of(null));
    }

    @Test
    @Order(15)
    @DisplayName("DeptEduType.of(0) 应抛出异常")
    void deptEduType_of_shouldThrowOnInvalid0() {
        assertThrows(IllegalArgumentException.class, () -> DeptEduTypeEnum.of(0));
    }

    @Test
    @Order(16)
    @DisplayName("DeptEduType.of(6) 应抛出异常")
    void deptEduType_of_shouldThrowOnInvalid6() {
        assertThrows(IllegalArgumentException.class, () -> DeptEduTypeEnum.of(6));
    }

    // ==================== StageYearCode ====================

    @Test
    @Order(20)
    @DisplayName("StageYearCode.primarySix() 应返回值 4, 年数 6")
    void stageYearCode_primarySix() {
        StageYearCodeEnum code = StageYearCodeEnum.PRIMARY_SIX;
        assertEquals("4", code.getValue());
        assertEquals(6, code.getYearCount());
        assertTrue(code.isPrimary());
        assertFalse(code.isJuniorHigh());
        assertFalse(code.isSeniorHigh());
    }

    @Test
    @Order(21)
    @DisplayName("StageYearCode.juniorThree() 应返回值 5, 年数 3")
    void stageYearCode_juniorThree() {
        StageYearCodeEnum code = StageYearCodeEnum.JUNIOR_THREE;
        assertEquals("5", code.getValue());
        assertEquals(3, code.getYearCount());
        assertTrue(code.isJuniorHigh());
        assertFalse(code.isPrimary());
        assertFalse(code.isSeniorHigh());
    }

    @Test
    @Order(22)
    @DisplayName("StageYearCode.seniorThree() 应返回值 7, 年数 3")
    void stageYearCode_seniorThree() {
        StageYearCodeEnum code = StageYearCodeEnum.SENIOR_THREE;
        assertEquals("7", code.getValue());
        assertEquals(3, code.getYearCount());
        assertTrue(code.isSeniorHigh());
        assertFalse(code.isPrimary());
        assertFalse(code.isJuniorHigh());
    }

    @Test
    @Order(23)
    @DisplayName("StageYearCode.of() 合法值应正确解析")
    void stageYearCode_of_shouldParseValidValues() {
        assertEquals("4", StageYearCodeEnum.of("4").getValue());
        assertEquals("5", StageYearCodeEnum.of("5").getValue());
        assertEquals("7", StageYearCodeEnum.of("7").getValue());
    }

    @Test
    @Order(24)
    @DisplayName("StageYearCode.of(null) 应抛出异常")
    void stageYearCode_of_shouldThrowOnNull() {
        assertThrows(IllegalArgumentException.class, () -> StageYearCodeEnum.of(null));
    }

    @Test
    @Order(25)
    @DisplayName("StageYearCode.of(非法值) 应抛出异常")
    void stageYearCode_of_shouldThrowOnInvalid() {
        assertThrows(IllegalArgumentException.class, () -> StageYearCodeEnum.of("99"));
    }
}
