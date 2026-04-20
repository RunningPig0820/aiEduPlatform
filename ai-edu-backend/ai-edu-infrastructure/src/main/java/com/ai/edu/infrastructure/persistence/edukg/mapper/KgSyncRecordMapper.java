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

    /**
     * 按维度字段筛选查询同步记录
     * 参数为 null 时表示不限制该维度
     */
    @Select("<script>" +
            "SELECT * FROM t_kg_sync_record WHERE is_deleted = false " +
            "<if test='edition != null'> AND edition = #{edition}</if>" +
            "<if test='subject != null'> AND subject = #{subject}</if>" +
            "<if test='stage != null'> AND stage = #{stage}</if>" +
            "<if test='grade != null'> AND grade = #{grade}</if>" +
            " ORDER BY started_at DESC LIMIT #{limit}" +
            "</script>")
    List<KgSyncRecordPo> selectByScopeFields(
            @Param("edition") String edition,
            @Param("subject") String subject,
            @Param("stage") String stage,
            @Param("grade") String grade,
            @Param("limit") int limit);

    @Select("SELECT * FROM t_kg_sync_record WHERE edition = #{edition} AND subject = #{subject} "
            + "AND ((#{stage} IS NULL AND stage IS NULL) OR stage = #{stage}) "
            + "AND ((#{grade} IS NULL AND grade IS NULL) OR grade = #{grade}) "
            + "AND status = 'running' AND is_deleted = false ORDER BY started_at DESC LIMIT 1")
    KgSyncRecordPo selectLatestRunningByScope(
            @Param("edition") String edition,
            @Param("subject") String subject,
            @Param("stage") String stage,
            @Param("grade") String grade);
}
