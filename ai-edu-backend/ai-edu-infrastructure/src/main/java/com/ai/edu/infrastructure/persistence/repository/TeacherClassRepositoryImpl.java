package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.organization.model.entity.TeacherClass;
import com.ai.edu.domain.organization.repository.TeacherClassRepository;
import com.ai.edu.infrastructure.persistence.mapper.TeacherClassMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 老师-班级关联仓储实现
 */
@Repository
public class TeacherClassRepositoryImpl implements TeacherClassRepository {

    @Resource
    private TeacherClassMapper teacherClassMapper;

    @Override
    public TeacherClass save(TeacherClass teacherClass) {
        if (teacherClass.getId() == null) {
            teacherClassMapper.insert(teacherClass);
        } else {
            teacherClassMapper.updateById(teacherClass);
        }
        return teacherClass;
    }

    @Override
    public Optional<TeacherClass> findById(Long id) {
        return Optional.ofNullable(teacherClassMapper.selectById(id));
    }

    @Override
    public Optional<TeacherClass> findByTeacherIdAndClassId(Long teacherId, Long classId) {
        return teacherClassMapper.selectByTeacherIdAndClassId(teacherId, classId);
    }

    @Override
    public List<TeacherClass> findByClassId(Long classId) {
        return teacherClassMapper.selectByClassId(classId);
    }

    @Override
    public List<TeacherClass> findActiveByClassId(Long classId) {
        return teacherClassMapper.selectActiveByClassId(classId);
    }

    @Override
    public List<TeacherClass> findByTeacherId(Long teacherId) {
        return teacherClassMapper.selectByTeacherId(teacherId);
    }

    @Override
    public List<TeacherClass> findActiveByTeacherId(Long teacherId) {
        return teacherClassMapper.selectActiveByTeacherId(teacherId);
    }

    @Override
    public Optional<TeacherClass> findHeadTeacherByClassId(Long classId) {
        return teacherClassMapper.selectHeadTeacherByClassId(classId);
    }

    @Override
    public List<TeacherClass> findByTeacherIdAndSubject(Long teacherId, String subject) {
        return teacherClassMapper.selectByTeacherIdAndSubject(teacherId, subject);
    }

    @Override
    public boolean existsByTeacherIdAndClassId(Long teacherId, Long classId) {
        return teacherClassMapper.existsByTeacherIdAndClassId(teacherId, classId);
    }

    @Override
    public boolean existsHeadTeacherByClassId(Long classId) {
        return teacherClassMapper.existsHeadTeacherByClassId(classId);
    }

    @Override
    public int countByClassIdAndStatus(Long classId, String status) {
        return teacherClassMapper.countByClassIdAndStatus(classId, status);
    }

    @Override
    public void deleteById(Long id) {
        teacherClassMapper.deleteById(id);
    }

    @Override
    public void deleteByTeacherId(Long teacherId) {
        LambdaQueryWrapper<TeacherClass> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherClass::getTeacherId, teacherId);
        teacherClassMapper.delete(wrapper);
    }

    @Override
    public void deleteByClassId(Long classId) {
        LambdaQueryWrapper<TeacherClass> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherClass::getClassId, classId);
        teacherClassMapper.delete(wrapper);
    }
}