package com.ai.edu.infrastructure.persistence.edukg.mapper;

import com.ai.edu.domain.edukg.model.entity.relation.KgChapterSection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 章节-小节关联Mapper接口
 */
@Mapper
@DS("kg")
public interface KgChapterSectionMapper extends BaseMapper<KgChapterSection> {

    @Select("SELECT * FROM t_kg_chapter_section WHERE chapter_uri = #{chapterUri} AND is_deleted = false ORDER BY order_index")
    List<KgChapterSection> selectByChapterUri(@Param("chapterUri") String chapterUri);

    @Select("SELECT * FROM t_kg_chapter_section WHERE section_uri = #{sectionUri} AND is_deleted = false")
    List<KgChapterSection> selectBySectionUri(@Param("sectionUri") String sectionUri);

    @Update("UPDATE t_kg_chapter_section SET is_deleted = 1, modified_by = #{modifiedBy} WHERE is_deleted = false")
    int batchDeleteAll(@Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_chapter_section SET is_deleted = 1, modified_by = #{modifiedBy} WHERE chapter_uri = #{chapterUri} AND is_deleted = false")
    int deleteByChapterUri(@Param("chapterUri") String chapterUri, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_chapter_section SET is_deleted = 1, modified_by = #{modifiedBy} WHERE section_uri = #{sectionUri} AND is_deleted = false")
    int deleteBySectionUri(@Param("sectionUri") String sectionUri, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_chapter_section SET order_index = #{orderIndex}, modified_by = #{modifiedBy} WHERE chapter_uri = #{chapterUri} AND section_uri = #{sectionUri} AND is_deleted = false")
    int updateOrderIndex(@Param("chapterUri") String chapterUri, @Param("sectionUri") String sectionUri, @Param("orderIndex") Integer orderIndex, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_chapter_section SET is_deleted = 1, modified_by = #{modifiedBy} WHERE chapter_uri = #{chapterUri} AND section_uri = #{sectionUri} AND is_deleted = false")
    int softDeleteRelation(@Param("chapterUri") String chapterUri, @Param("sectionUri") String sectionUri, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_chapter_section SET is_deleted = 1, modified_by = #{modifiedBy} WHERE chapter_uri = #{chapterUri} AND is_deleted = false")
    int softDeleteByChapterUri(@Param("chapterUri") String chapterUri, @Param("modifiedBy") Long modifiedBy);

    @Select("SELECT * FROM t_kg_chapter_section WHERE is_deleted = false")
    List<KgChapterSection> selectAllActiveRelations();

    @Insert("<script>" +
            "INSERT INTO t_kg_chapter_section (chapter_uri, section_uri, order_index, created_by, modified_by, is_deleted) VALUES " +
            "<foreach item='item' collection='list' separator=','>" +
            "(#{item.chapterUri}, #{item.sectionUri}, #{item.orderIndex}, #{item.createdBy}, #{item.modifiedBy}, 0)" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<KgChapterSection> relations);
}
