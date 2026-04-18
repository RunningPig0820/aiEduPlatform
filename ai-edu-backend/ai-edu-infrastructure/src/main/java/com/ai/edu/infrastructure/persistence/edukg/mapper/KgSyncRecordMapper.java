package com.ai.edu.infrastructure.persistence.edukg.mapper;

import com.ai.edu.infrastructure.persistence.edukg.po.KgSyncRecordPo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 同步记录Mapper接口
 */
@Mapper
@DS("kg")
public interface KgSyncRecordMapper extends BaseMapper<KgSyncRecordPo> {

    @Select("SELECT * FROM t_kg_sync_record WHERE is_deleted = false ORDER BY started_at DESC LIMIT #{limit}")
    List<KgSyncRecordPo> selectRecent(@Param("limit") int limit);

    @Select("SELECT * FROM t_kg_sync_record WHERE scope = #{scope} AND is_deleted = false ORDER BY started_at DESC")
    List<KgSyncRecordPo> selectByScope(@Param("scope") String scope);
}
