package com.ai.edu.domain.organization.repository;

import com.ai.edu.domain.organization.model.entity.School;

import java.util.List;
import java.util.Optional;

/**
 * 学校仓储接口
 * 定义学校实体的持久化操作
 */
public interface SchoolRepository {

    /**
     * 保存学校
     */
    School save(School school);

    /**
     * 根据ID查找学校
     */
    Optional<School> findById(Long id);

    /**
     * 根据编码查找学校
     */
    Optional<School> findByCode(String code);

    /**
     * 根据名称查找学校
     */
    Optional<School> findByName(String name);

    /**
     * 查找所有学校
     */
    List<School> findAll();

    /**
     * 根据省市查找学校
     */
    List<School> findByProvinceAndCity(String province, String city);

    /**
     * 根据学校类型查找学校
     */
    List<School> findBySchoolType(String schoolType);

    /**
     * 查找所有活跃学校
     */
    List<School> findAllActive();

    /**
     * 检查编码是否已存在
     */
    boolean existsByCode(String code);

    /**
     * 检查名称是否已存在
     */
    boolean existsByName(String name);

    /**
     * 根据ID删除学校
     */
    void deleteById(Long id);

    /**
     * 根据ID恢复学校
     */
    void restoreById(Long id);
}