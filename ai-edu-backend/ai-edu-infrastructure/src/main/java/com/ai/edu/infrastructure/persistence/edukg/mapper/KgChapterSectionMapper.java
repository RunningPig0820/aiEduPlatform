package com.ai.edu.infrastructure.persistence.edukg.mapper;

import com.ai.edu.infrastructure.persistence.edukg.po.KgChapterSectionPo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 章节-小节关联Mapper接口
 */
@Mapper
@DS("kg")
public interface KgChapterSectionMapper extends BaseMapper<KgChapterSectionPo> {

    @Select("SELECT * FROM t_kg_chapter_section WHERE chapter_uri = #{chapterUri} AND is_deleted = false ORDER BY order_index")
    List<KgChapterSectionPo> selectByChapterUri(@Param("chapterUri") String chapterUri);

    @Select("SELECT * FROM t_kg_chapter_section WHERE section_uri = #{sectionUri} AND is_deleted = false")
    List<KgChapterSectionPo> selectBySectionUri(@Param("sectionUri") String sectionUri);

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

    /**
     * 批量查询：按多个 chapter_uri 查询完整关联记录（用于导航树构建）
     */
    @Select("<script>" +
            "SELECT * FROM t_kg_chapter_section WHERE chapter_uri IN " +
            "<foreach item='uri' collection='chapterUris' open='(' separator=',' close=')'>#{uri}</foreach>" +
            " AND is_deleted = false ORDER BY chapter_uri, order_index" +
            "</script>")
    List<KgChapterSectionPo> selectByChapterUris(@Param("chapterUris") List<String> chapterUris);

    /**
     * 批量查询：按多个 chapter_uri 查询所有 section_uri（用于 Repository 组装）
     */
    @Select("<script>" +
            "SELECT DISTINCT section_uri FROM t_kg_chapter_section WHERE chapter_uri IN " +
            "<foreach item='uri' collection='chapterUris' open='(' separator=',' close=')'>#{uri}</foreach>" +
            " AND is_deleted = false" +
            "</script>")
    List<String> selectSectionUrisByChapterUris(@Param("chapterUris") List<String> chapterUris);

    @Insert("<script>" +
            "INSERT INTO t_kg_chapter_section (chapter_uri, section_uri, order_index, created_by, modified_by, is_deleted) VALUES " +
            "<foreach item='item' collection='list' separator=','>" +
            "(#{item.chapterUri}, #{item.sectionUri}, #{item.orderIndex}, #{item.createdBy}, #{item.modifiedBy}, 0)" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<KgChapterSectionPo> relations);
}
