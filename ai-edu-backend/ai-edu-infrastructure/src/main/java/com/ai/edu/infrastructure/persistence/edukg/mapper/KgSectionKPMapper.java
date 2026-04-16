package com.ai.edu.infrastructure.persistence.edukg.mapper;

import com.ai.edu.domain.edukg.model.entity.relation.KgSectionKP;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 小节-知识点关联Mapper接口
 */
@Mapper
@DS("kg")
public interface KgSectionKPMapper extends BaseMapper<KgSectionKP> {

    @Select("SELECT * FROM t_kg_section_kp WHERE section_uri = #{sectionUri} AND is_deleted = false ORDER BY order_index")
    List<KgSectionKP> selectBySectionUri(@Param("sectionUri") String sectionUri);

    @Select("SELECT * FROM t_kg_section_kp WHERE kp_uri = #{kpUri} AND is_deleted = false")
    List<KgSectionKP> selectByKpUri(@Param("kpUri") String kpUri);

    @Update("UPDATE t_kg_section_kp SET is_deleted = 1, modified_by = #{modifiedBy} WHERE is_deleted = false")
    int batchDeleteAll(@Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_section_kp SET is_deleted = 1, modified_by = #{modifiedBy} WHERE section_uri = #{sectionUri} AND is_deleted = false")
    int deleteBySectionUri(@Param("sectionUri") String sectionUri, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_section_kp SET is_deleted = 1, modified_by = #{modifiedBy} WHERE kp_uri = #{kpUri} AND is_deleted = false")
    int deleteByKpUri(@Param("kpUri") String kpUri, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_section_kp SET order_index = #{orderIndex}, modified_by = #{modifiedBy} WHERE section_uri = #{sectionUri} AND kp_uri = #{kpUri} AND is_deleted = false")
    int updateOrderIndex(@Param("sectionUri") String sectionUri, @Param("kpUri") String kpUri, @Param("orderIndex") Integer orderIndex, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_section_kp SET is_deleted = 1, modified_by = #{modifiedBy} WHERE section_uri = #{sectionUri} AND kp_uri = #{kpUri} AND is_deleted = false")
    int softDeleteRelation(@Param("sectionUri") String sectionUri, @Param("kpUri") String kpUri, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_section_kp SET is_deleted = 1, modified_by = #{modifiedBy} WHERE section_uri = #{sectionUri} AND is_deleted = false")
    int softDeleteBySectionUri(@Param("sectionUri") String sectionUri, @Param("modifiedBy") Long modifiedBy);

    @Select("SELECT * FROM t_kg_section_kp WHERE is_deleted = false")
    List<KgSectionKP> selectAllActiveRelations();

    @Insert("<script>" +
            "INSERT INTO t_kg_section_kp (section_uri, kp_uri, order_index, created_by, modified_by, is_deleted) VALUES " +
            "<foreach item='item' collection='list' separator=','>" +
            "(#{item.sectionUri}, #{item.kpUri}, #{item.orderIndex}, #{item.createdBy}, #{item.modifiedBy}, 0)" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<KgSectionKP> relations);
}
