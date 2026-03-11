package com.ai.edu.infrastructure.persistence.mapper;

import com.ai.edu.domain.homework.model.entity.Homework;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作业Mapper接口
 */
@Mapper
public interface HomeworkMapper extends BaseMapper<Homework> {
}