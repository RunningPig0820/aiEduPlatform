package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.organization.model.entity.StudentClass;
import com.ai.edu.domain.organization.repository.StudentClassRepository;
import com.ai.edu.infrastructure.persistence.mapper.StudentClassMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学生-班级关联仓储实现
 */
@Repository
public class StudentClassRepositoryImpl implements StudentClassRepository {

    @Resource
    private StudentClassMapper studentClassMapper;

    @Override
    public StudentClass save(StudentClass studentClass) {
        if (studentClass.getId() == null) {
            studentClassMapper.insert(studentClass);
        } else {
            studentClassMapper.updateById(studentClass);
        }
        return studentClass;
    }

    @Override
    public Optional<StudentClass> findById(Long id) {
        return Optional.ofNullable(studentClassMapper.selectById(id));
    }

    @Override
    public Optional<StudentClass> findByStudentIdAndClassId(Long studentId, Long classId) {
        return studentClassMapper.selectByStudentIdAndClassId(studentId, classId);
    }

    @Override
    public Optional<StudentClass> findActiveByStudentId(Long studentId) {
        return studentClassMapper.selectActiveByStudentId(studentId);
    }

    @Override
    public List<StudentClass> findByClassId(Long classId) {
        return studentClassMapper.selectByClassId(classId);
    }

    @Override
    public List<StudentClass> findActiveByClassId(Long classId) {
        return studentClassMapper.selectActiveByClassId(classId);
    }

    @Override
    public List<StudentClass> findByStudentId(Long studentId) {
        return studentClassMapper.selectByStudentId(studentId);
    }

    @Override
    public List<StudentClass> findByStatus(String status) {
        return studentClassMapper.selectByStatus(status);
    }

    @Override
    public boolean existsByStudentIdAndClassId(Long studentId, Long classId) {
        return studentClassMapper.existsByStudentIdAndClassId(studentId, classId);
    }

    @Override
    public int countByClassIdAndStatus(Long classId, String status) {
        return studentClassMapper.countByClassIdAndStatus(classId, status);
    }

    @Override
    public void deleteById(Long id) {
        studentClassMapper.deleteById(id);
    }

    @Override
    public void deleteByStudentId(Long studentId) {
        LambdaQueryWrapper<StudentClass> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentClass::getStudentId, studentId);
        studentClassMapper.delete(wrapper);
    }

    @Override
    public void deleteByClassId(Long classId) {
        LambdaQueryWrapper<StudentClass> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentClass::getClassId, classId);
        studentClassMapper.delete(wrapper);
    }
}