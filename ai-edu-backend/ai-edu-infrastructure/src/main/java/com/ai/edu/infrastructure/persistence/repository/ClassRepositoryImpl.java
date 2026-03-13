package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.organization.model.entity.Class;
import com.ai.edu.domain.organization.repository.ClassRepository;
import com.ai.edu.infrastructure.persistence.mapper.ClassMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 班级仓储实现
 */
@Repository
public class ClassRepositoryImpl implements ClassRepository {

    @Resource
    private ClassMapper classMapper;


    @Override
    public Class save(Class classEntity) {
        if (classEntity.getId() == null) {
            classMapper.insert(classEntity);
        } else {
            classMapper.updateById(classEntity);
        }
        return classEntity;
    }

    @Override
    public Optional<Class> findById(Long id) {
        return Optional.ofNullable(classMapper.selectById(id));
    }

    @Override
    public Optional<Class> findByCode(String code) {
        return classMapper.selectByCode(code);
    }

    @Override
    public List<Class> findBySchoolId(Long schoolId) {
        return classMapper.selectBySchoolId(schoolId);
    }

    @Override
    public List<Class> findByGrade(String grade) {
        return classMapper.selectByGrade(grade);
    }

    @Override
    public List<Class> findBySchoolYear(String schoolYear) {
        return classMapper.selectBySchoolYear(schoolYear);
    }

    @Override
    public List<Class> findByStatus(String status) {
        return classMapper.selectByStatus(status);
    }

    @Override
    public List<Class> findActiveBySchoolId(Long schoolId) {
        return classMapper.selectActiveBySchoolId(schoolId);
    }

    @Override
    public Optional<Class> findActiveById(Long id) {
        return classMapper.selectActiveById(id);
    }

    @Override
    public boolean existsByNameAndSchoolYear(String name, String schoolYear) {
        return classMapper.existsByNameAndSchoolYear(name, schoolYear);
    }

    @Override
    public int countBySchoolIdAndStatus(Long schoolId, String status) {
        return classMapper.countBySchoolIdAndStatus(schoolId, status);
    }

    @Override
    public void deleteById(Long id) {
        Class classEntity = classMapper.selectById(id);
        if (classEntity != null) {
            classEntity.delete();
            classMapper.updateById(classEntity);
        }
    }

    @Override
    public void restoreById(Long id) {
        Class classEntity = classMapper.selectById(id);
        if (classEntity != null) {
            classEntity.restore();
            classMapper.updateById(classEntity);
        }
    }
}