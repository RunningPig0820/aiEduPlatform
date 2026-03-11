package com.ai.edu.infrastructure.persistence.mapper;

import com.ai.edu.domain.learning.model.entity.ErrorBook;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 错题Mapper接口
 */
@Mapper
public interface ErrorBookMapper extends BaseMapper<ErrorBook> {
}