package com.ai.edu.infrastructure.persistence.repository;

import com.ai.edu.domain.organization.model.entity.School;
import com.ai.edu.domain.organization.repository.SchoolRepository;
import com.ai.edu.infrastructure.persistence.mapper.SchoolMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学校仓储实现
 */
@Repository
public class SchoolRepositoryImpl implements SchoolRepository {

    @Resource
    private SchoolMapper schoolMapper;


    @Override
    public School save(School school) {
        if (school.getId() == null) {
            schoolMapper.insert(school);
        } else {
            schoolMapper.updateById(school);
        }
        return school;
    }

    @Override
    public Optional<School> findById(Long id) {
        return Optional.ofNullable(schoolMapper.selectById(id));
    }

    @Override
    public Optional<School> findByCode(String code) {
        return Optional.ofNullable(schoolMapper.selectByCode(code));
    }

    @Override
    public List<School> findByProvinceAndCity(String province, String city) {
        return schoolMapper.selectByProvinceAndCity(province, city);
    }

    @Override
    public List<School> findBySchoolType(String schoolType) {
        return schoolMapper.selectBySchoolType(schoolType);
    }

    @Override
    public List<School> findAllActive() {
        return schoolMapper.selectAllActive();
    }

    @Override
    public boolean existsByCode(String code) {
        return schoolMapper.existsByCode(code);
    }

    @Override
    public void deleteById(Long id) {
        School school = schoolMapper.selectById(id);
        if (school != null) {
            school.delete();
            schoolMapper.updateById(school);
        }
    }

    @Override
    public void restoreById(Long id) {
        School school = schoolMapper.selectById(id);
        if (school != null) {
            school.restore();
            schoolMapper.updateById(school);
        }
    }
}