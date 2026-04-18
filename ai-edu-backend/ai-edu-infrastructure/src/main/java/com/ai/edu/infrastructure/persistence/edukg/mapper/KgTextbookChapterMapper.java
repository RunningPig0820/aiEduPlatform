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

    @Select("SELECT * FROM t_kg_textbook_chapter WHERE is_deleted = false")
    List<KgTextbookChapterPo> selectAllActiveRelations();

    @Insert("<script>" +
            "INSERT INTO t_kg_textbook_chapter (textbook_uri, chapter_uri, order_index, created_by, modified_by, is_deleted) VALUES " +
            "<foreach item='item' collection='list' separator=','>" +
            "(#{item.textbookUri}, #{item.chapterUri}, #{item.orderIndex}, #{item.createdBy}, #{item.modifiedBy}, 0)" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<KgTextbookChapterPo> relations);
}
