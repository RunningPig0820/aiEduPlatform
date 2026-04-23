package com.ai.edu.infrastructure.persistence.edukg.mapper;

import com.ai.edu.infrastructure.persistence.edukg.po.KgTextbookChapterPo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 教材-章节关联Mapper接口
 */
@Mapper
@DS("kg")
public interface KgTextbookChapterMapper extends BaseMapper<KgTextbookChapterPo> {

    @Select("SELECT * FROM t_kg_textbook_chapter WHERE textbook_uri = #{textbookUri} AND is_deleted = false ORDER BY order_index")
    List<KgTextbookChapterPo> selectByTextbookUri(@Param("textbookUri") String textbookUri);

    @Select("SELECT * FROM t_kg_textbook_chapter WHERE chapter_uri = #{chapterUri} AND is_deleted = false")
    List<KgTextbookChapterPo> selectByChapterUri(@Param("chapterUri") String chapterUri);

    @Update("UPDATE t_kg_textbook_chapter SET is_deleted = 1, modified_by = #{modifiedBy} WHERE is_deleted = false")
    int batchDeleteAll(@Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_textbook_chapter SET is_deleted = 1, modified_by = #{modifiedBy} WHERE textbook_uri = #{textbookUri} AND is_deleted = false")
    int deleteByTextbookUri(@Param("textbookUri") String textbookUri, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_textbook_chapter SET is_deleted = 1, modified_by = #{modifiedBy} WHERE chapter_uri = #{chapterUri} AND is_deleted = false")
    int deleteByChapterUri(@Param("chapterUri") String chapterUri, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_textbook_chapter SET order_index = #{orderIndex}, modified_by = #{modifiedBy} WHERE textbook_uri = #{textbookUri} AND chapter_uri = #{chapterUri} AND is_deleted = false")
    int updateOrderIndex(@Param("textbookUri") String textbookUri, @Param("chapterUri") String chapterUri, @Param("orderIndex") Integer orderIndex, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_textbook_chapter SET is_deleted = 1, modified_by = #{modifiedBy} WHERE textbook_uri = #{textbookUri} AND chapter_uri = #{chapterUri} AND is_deleted = false")
    int softDeleteRelation(@Param("textbookUri") String textbookUri, @Param("chapterUri") String chapterUri, @Param("modifiedBy") Long modifiedBy);

    @Update("UPDATE t_kg_textbook_chapter SET is_deleted = 1, modified_by = #{modifiedBy} WHERE textbook_uri = #{textbookUri} AND is_deleted = false")
    int softDeleteByTextbookUri(@Param("textbookUri") String textbookUri, @Param("modifiedBy") Long modifiedBy);

    /**
     * 批量查询：按多个 textbook_uri 查询完整关联记录（用于导航树构建）
     */
    @Select("<script>" +
            "SELECT * FROM t_kg_textbook_chapter WHERE textbook_uri IN " +
            "<foreach item='uri' collection='textbookUris' open='(' separator=',' close=')'>#{uri}</foreach>" +
            " AND is_deleted = false ORDER BY textbook_uri, order_index" +
            "</script>")
    List<KgTextbookChapterPo> selectByTextbookUris(@Param("textbookUris") List<String> textbookUris);

    /**
     * 批量查询：按多个 textbook_uri 查询所有 chapter_uri（用于 Repository 组装）
     */
    @Select("<script>" +
            "SELECT DISTINCT chapter_uri FROM t_kg_textbook_chapter WHERE textbook_uri IN " +
            "<foreach item='uri' collection='textbookUris' open='(' separator=',' close=')'>#{uri}</foreach>" +
            " AND is_deleted = false" +
            "</script>")
    List<String> selectChapterUrisByTextbookUris(@Param("textbookUris") List<String> textbookUris);

    @Insert("<script>" +
            "INSERT INTO t_kg_textbook_chapter (textbook_uri, chapter_uri, order_index, created_by, modified_by, is_deleted) VALUES " +
            "<foreach item='item' collection='list' separator=','>" +
            "(#{item.textbookUri}, #{item.chapterUri}, #{item.orderIndex}, #{item.createdBy}, #{item.modifiedBy}, 0)" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<KgTextbookChapterPo> relations);
}
