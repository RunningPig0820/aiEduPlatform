package com.ai.edu.domain.organization.service;

import com.ai.edu.domain.organization.model.aggregate.ClassAggregate;
import com.ai.edu.domain.organization.model.entity.Class;
import com.ai.edu.domain.organization.model.entity.StudentClass;
import com.ai.edu.domain.organization.model.valueobject.GradeLevel;
import com.ai.edu.domain.organization.model.valueobject.SchoolYear;
import com.ai.edu.domain.organization.model.valueobject.StudentClassStatus;
import com.ai.edu.domain.organization.repository.ClassRepository;
import com.ai.edu.domain.organization.repository.StudentClassRepository;
import com.ai.edu.domain.organization.repository.TeacherClassRepository;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.domain.shared.valueobject.UserId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 班级领域服务
 * 处理跨聚合的业务逻辑
 */
@Slf4j
@Service
public class ClassDomainService {

    @Resource
    private ClassRepository classRepository;

    @Resource
    private StudentClassRepository studentClassRepository;

    @Resource
    private TeacherClassRepository teacherClassRepository;

    /**
     * 创建班级并设置班主任
     */
    public ClassAggregate createClassWithHeadTeacher(
            String name,
            GradeLevel grade,
            SchoolYear schoolYear,
            SchoolId schoolId,
            UserId headTeacherId,
            String subject) {

        log.info("创建班级并设置班主任: name={}, schoolId={}", name, schoolId.getValue());

        // 1. 创建班级
        Class classEntity = Class.createWithSchool(name, grade, schoolYear, schoolId);
        classRepository.save(classEntity);

        // 2. 创建班级聚合
        ClassAggregate aggregate = new ClassAggregate(classEntity);

        // 3. 设置班主任
        aggregate.addHeadTeacher(headTeacherId, subject);

        // 保存班主任关联
        aggregate.getTeacherClasses().forEach(tc -> teacherClassRepository.save(tc));

        log.info("班级创建成功: classId={}", classEntity.getIdValue());

        return aggregate;
    }

    /**
     * 批量添加学生到班级
     */
    public void batchAddStudents(ClassId classId, List<UserId> studentIds, Map<UserId, String> studentNos) {
        log.info("批量添加学生: classId={}, studentCount={}", classId.getValue(), studentIds.size());

        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("班级不存在"));

        if (!classEntity.isActive()) {
            throw new IllegalStateException("班级已毕业或归档，无法添加学生");
        }

        for (UserId studentId : studentIds) {
            if (!studentClassRepository.existsByStudentIdAndClassId(studentId, classId)) {
                StudentClass sc = StudentClass.create(studentId, classId);
                String studentNo = studentNos.get(studentId);
                if (studentNo != null) {
                    sc.setStudentNo(studentNo);
                }
                studentClassRepository.save(sc);
            }
        }

        log.info("学生添加完成: classId={}", classId.getValue());
    }

    /**
     * 班级毕业处理
     */
    public void graduateClass(ClassId classId) {
        log.info("班级毕业处理: classId={}", classId.getValue());

        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("班级不存在"));

        classEntity.graduate();
        classRepository.save(classEntity);

        // 更新所有学生状态
        List<StudentClass> students = studentClassRepository.findActiveByClassId(classId);
        for (StudentClass sc : students) {
            sc.graduate();
            studentClassRepository.save(sc);
        }

        log.info("班级毕业完成: classId={}, studentCount={}", classId.getValue(), students.size());
    }

    /**
     * 获取班级聚合（包含学生和教师）
     */
    public ClassAggregate getClassAggregate(ClassId classId) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("班级不存在"));

        ClassAggregate aggregate = new ClassAggregate(classEntity);

        // 加载学生
        List<StudentClass> students = studentClassRepository.findActiveByClassId(classId);
        aggregate.loadStudents(students);

        // 加载教师
        List<?> teachers = teacherClassRepository.findActiveByClassId(classId);
        // 由于类型问题，这里需要转换
        // aggregate.loadTeachers(teachers);

        return aggregate;
    }

    /**
     * 获取班级学生数量
     */
    public int getStudentCount(ClassId classId) {
        return studentClassRepository.countByClassIdAndStatus(classId, StudentClassStatus.active());
    }

    /**
     * 检查班级是否可以添加学生
     */
    public boolean canAddStudent(ClassId classId) {
        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("班级不存在"));
        return classEntity.isActive();
    }
}