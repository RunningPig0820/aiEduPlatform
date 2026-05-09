package com.ai.edu.domain.organization.service;

import com.ai.edu.domain.organization.model.entity.Class;
import com.ai.edu.domain.organization.model.entity.Grade;
import com.ai.edu.domain.organization.model.valueobject.GradeLevel;
import com.ai.edu.domain.organization.repository.ClassRepository;
import com.ai.edu.domain.organization.repository.GradeRepository;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 年级领域服务
 * 处理年级相关的业务逻辑
 */
@Slf4j
@Service
public class GradeDomainService {

    @Resource
    private GradeRepository gradeRepository;

    @Resource
    private ClassRepository classRepository;

    /**
     * 为学校创建年级
     */
    public Grade createGradeForSchool(String name, GradeLevel gradeLevel, SchoolId schoolId) {
        log.info("创建年级: name={}, schoolId={}, gradeLevel={}", name, schoolId.getValue(), gradeLevel.getValue());

        // 检查是否已存在相同 gradeLevel 的年级
        List<Grade> existingGrades = gradeRepository.findBySchoolId(schoolId);
        boolean exists = existingGrades.stream()
                .anyMatch(g -> g.getGradeLevel() != null && g.getGradeLevel().equals(gradeLevel));

        if (exists) {
            throw new IllegalStateException("学校已存在该年级级别");
        }

        Grade grade = Grade.createWithSchool(name, gradeLevel, schoolId);
        gradeRepository.save(grade);

        log.info("年级创建成功: gradeId={}", grade.getId());

        return grade;
    }

    /**
     * 获取学校的所有年级
     */
    public List<Grade> getSchoolGrades(SchoolId schoolId) {
        return gradeRepository.findBySchoolId(schoolId);
    }

    /**
     * 获取年级下的班级数量
     */
    public int getClassCountByGrade(SchoolId schoolId, GradeLevel gradeLevel) {
        List<com.ai.edu.domain.organization.model.entity.Class> classes = classRepository.findByGrade(gradeLevel);
        // 过滤属于该学校的班级
        return (int) classes.stream()
                .filter(c -> c.getSchoolId() != null && c.getSchoolId().equals(schoolId))
                .count();
    }

    /**
     * 检查年级是否属于学校
     */
    public boolean isGradeBelongsToSchool(Long gradeId, SchoolId schoolId) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("年级不存在"));

        return grade.getSchoolId() != null && grade.getSchoolId().equals(schoolId);
    }
}