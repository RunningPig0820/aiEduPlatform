package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.School;

import java.util.List;
import java.util.Optional;

/**
 * 学校仓储接口
 */
public interface SchoolRepository {

    School save(School school);

    Optional<School> findById(Long id);

    Optional<School> findByCode(String code);

    List<School> findByProvinceAndCity(String province, String city);

    List<School> findBySchoolType(String schoolType);

    List<School> findAllActive();

    boolean existsByCode(String code);

    void deleteById(Long id);

    void restoreById(Long id);
}