package com.ai.edu.application.service.org;

import com.ai.edu.application.dto.org.ClassDTO;
import com.ai.edu.application.dto.org.command.AddStudentCommand;
import com.ai.edu.application.dto.org.command.CreateClassCommand;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.organization.model.aggregate.ClassAggregate;
import com.ai.edu.domain.organization.model.entity.Class;
import com.ai.edu.domain.organization.model.entity.StudentClass;
import com.ai.edu.domain.organization.model.entity.TeacherClass;
import com.ai.edu.domain.organization.model.valueobject.GradeLevel;
import com.ai.edu.domain.organization.model.valueobject.SchoolYear;
import com.ai.edu.domain.organization.repository.ClassRepository;
import com.ai.edu.domain.organization.repository.SchoolRepository;
import com.ai.edu.domain.organization.repository.StudentClassRepository;
import com.ai.edu.domain.organization.repository.TeacherClassRepository;
import com.ai.edu.domain.organization.service.ClassDomainService;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.SchoolId;
import com.ai.edu.domain.shared.valueobject.UserId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 班级应用服务
 */
@Slf4j
@Service
public class ClassAppService {

    @Resource
    private ClassDomainService classDomainService;

    @Resource
    private ClassRepository classRepository;

    @Resource
    private SchoolRepository schoolRepository;

    @Resource
    private StudentClassRepository studentClassRepository;

    @Resource
    private TeacherClassRepository teacherClassRepository;

    /**
     * 创建班级
     */
    @Transactional
    public ClassDTO createClass(CreateClassCommand command) {
        log.info("创建班级: name={}, schoolId={}", command.getName(), command.getSchoolId());

        // 验证学校存在
        SchoolId schoolId = SchoolId.of(command.getSchoolId());
        if (!schoolRepository.findById(schoolId).isPresent()) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_FOUND, "学校不存在");
        }

        // 转换值对象
        GradeLevel grade = GradeLevel.of(command.getGrade());
        SchoolYear schoolYear = SchoolYear.of(command.getSchoolYear());

        // 创建班级
        Class classEntity = Class.createWithSchool(command.getName(), grade, schoolYear, schoolId);

        if (command.getCode() != null) {
            classEntity.setCode(command.getCode());
        }

        classRepository.save(classEntity);

        // 如果有班主任，设置班主任
        if (command.getHeadTeacherId() != null && command.getSubject() != null) {
            ClassAggregate aggregate = new ClassAggregate(classEntity);
            aggregate.addHeadTeacher(UserId.of(command.getHeadTeacherId()), command.getSubject());

            // 保存班主任关联
            aggregate.getTeacherClasses().forEach(tc -> teacherClassRepository.save(tc));
        }

        log.info("班级创建成功: classId={}", classEntity.getIdValue());

        return toDTO(classEntity);
    }

    /**
     * 获取班级详情
     */
    public ClassDTO getClassById(Long id) {
        log.info("获取班级详情: id={}", id);

        Class classEntity = classRepository.findById(ClassId.of(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND, "班级不存在"));

        ClassDTO dto = toDTO(classEntity);

        // 获取学生数量
        int studentCount = studentClassRepository.countByClassIdAndStatus(
                ClassId.of(id),
                com.ai.edu.domain.organization.model.valueobject.StudentClassStatus.active()
        );
        dto.setStudentCount(studentCount);

        // 获取班主任
        TeacherClass headTeacher = teacherClassRepository.findHeadTeacherByClassId(ClassId.of(id))
                .orElse(null);
        if (headTeacher != null) {
            dto.setHeadTeacherId(headTeacher.getTeacherIdValue());
        }

        return dto;
    }

    /**
     * 添加学生到班级
     */
    @Transactional
    public void addStudent(Long classId, AddStudentCommand command) {
        log.info("添加学生到班级: classId={}, studentId={}", classId, command.getStudentId());

        ClassId clsId = ClassId.of(classId);
        UserId studentId = UserId.of(command.getStudentId());

        // 检查班级状态
        if (!classDomainService.canAddStudent(clsId)) {
            throw new BusinessException(ErrorCode.CLASS_NOT_ACTIVE, "班级已毕业或归档，无法添加学生");
        }

        // 检查是否已加入
        if (studentClassRepository.existsByStudentIdAndClassId(studentId, clsId)) {
            throw new BusinessException(ErrorCode.STUDENT_ALREADY_IN_CLASS, "学生已在班级中");
        }

        // 创建学生班级关联
        StudentClass studentClass = StudentClass.create(studentId, clsId);
        if (command.getStudentNo() != null) {
            studentClass.setStudentNo(command.getStudentNo());
        }
        studentClassRepository.save(studentClass);

        log.info("学生添加成功: classId={}, studentId={}", classId, command.getStudentId());
    }

    /**
     * 班级毕业
     */
    @Transactional
    public void graduateClass(Long id) {
        log.info("班级毕业: id={}", id);
        classDomainService.graduateClass(ClassId.of(id));
    }

    /**
     * 获取学校的班级列表
     */
    public List<ClassDTO> listClassesBySchool(Long schoolId) {
        log.info("获取学校班级列表: schoolId={}", schoolId);

        List<Class> classes = classRepository.findBySchoolId(SchoolId.of(schoolId));
        return classes.stream().map(this::toDTO).toList();
    }

    // ==================== 转换方法 ====================

    private ClassDTO toDTO(Class classEntity) {
        ClassDTO dto = ClassDTO.builder()
                .id(classEntity.getIdValue())
                .schoolId(classEntity.getSchoolIdValue())
                .name(classEntity.getName())
                .code(classEntity.getCode())
                .grade(classEntity.getGradeValue())
                .schoolYear(classEntity.getSchoolYearValue())
                .classType(classEntity.getClassTypeValue())
                .status(classEntity.getStatusValue())
                .description(classEntity.getDescription())
                .build();

        return dto;
    }
}