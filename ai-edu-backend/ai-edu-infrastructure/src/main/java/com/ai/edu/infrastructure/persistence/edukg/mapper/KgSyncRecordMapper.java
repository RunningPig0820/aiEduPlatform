package com.ai.edu.infrastructure.persistence.edukg.mapper;

import com.ai.edu.domain.edukg.model.entity.KgSyncRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 同步记录Mapper接口
 */
@Mapper
@DS("kg")
public interface KgSyncRecordMapper extends BaseMapper<KgSyncRecord> {

    @Select("SELECT * FROM t_kg_sync_record WHERE is_deleted = false ORDER BY started_at DESC LIMIT #{limit}")
    List<KgSyncRecord> selectRecent(@Param("limit") int limit);

    @Select("SELECT * FROM t_kg_sync_record WHERE scope = #{scope} AND is_deleted = false ORDER BY started_at DESC")
    List<KgSyncRecord> selectByScope(@Param("scope") String scope);
}
