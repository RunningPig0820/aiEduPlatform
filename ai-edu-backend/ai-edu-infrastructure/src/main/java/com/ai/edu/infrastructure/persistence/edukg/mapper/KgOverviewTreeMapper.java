package com.ai.edu.infrastructure.persistence.edukg.mapper;

import com.ai.edu.domain.edukg.model.valueobject.KgTreeNode;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 教材树浏览 Mapper（知识地图点击式下钻——每次单层查询，最多 2 表 JOIN，索引命中）。
 *
 * <p>替代原 7 表 JOIN 的知识点总览分页（慢 SQL：GROUP BY 先于 LIMIT + COUNT DISTINCT 全扫）。
 * 每层查询按关系表外键（textbook_uri/chapter_uri/section_uri）过滤，均命中唯一/普通索引。
 */
@Mapper
@DS("kg")
public interface KgOverviewTreeMapper {

    /**
     * 学段 → 年级（GROUP BY grade 去重，MIN(sort) 作顺序）。
     *
     * <p>⚠️ 讨巧实现（数据现状仅人教版 + 数学，教材表年级够用）：年级由教材表推导。
     * 正确应取组织域 {@code SchoolStageEnum} + {@code GradeEnum} 的「学段→年级」权威关系，
     * 见 {@code KgKnowledgeOverviewController.grades} 注释——教材覆盖多版本/多学科后切换。
     *
     * <p>注意：不能用 {@code DISTINCT grade, sort}——DISTINCT 按全部 select 列去重，
     * 同一年级多本教材（sort 不同）会重复；必须 GROUP BY grade 且仅按年级聚合（MIN(sort) 顺序）。
     */
    @Select("SELECT grade AS uri, grade AS label, MIN(sort) AS orderIndex FROM t_kg_textbook " +
            "WHERE stage = #{stage} AND is_deleted = false AND grade IS NOT NULL AND grade <> '' " +
            "GROUP BY grade ORDER BY MIN(sort)")
    List<KgTreeNode> selectGradesByStage(@Param("stage") String stage);

    /** 年级 → 课本（单表，WHERE stage+grade，ORDER BY sort）。 */
    @Select("SELECT uri, label, 0 AS orderIndex FROM t_kg_textbook " +
            "WHERE stage = #{stage} AND grade = #{grade} AND is_deleted = false ORDER BY sort")
    List<KgTreeNode> selectTextbooksByStageAndGrade(@Param("stage") String stage, @Param("grade") String grade);

    /** 课本 → 章节（textbook_chapter ⋈ chapter，2 表，ORDER BY order_index）。 */
    @Select("SELECT ch.uri AS uri, ch.label AS label, tc.order_index AS orderIndex " +
            "FROM t_kg_textbook_chapter tc " +
            "JOIN t_kg_chapter ch ON ch.uri = tc.chapter_uri AND ch.is_deleted = false " +
            "WHERE tc.textbook_uri = #{textbookUri} AND tc.is_deleted = false " +
            "ORDER BY tc.order_index")
    List<KgTreeNode> selectChaptersByTextbookUri(@Param("textbookUri") String textbookUri);

    /** 章节 → 小节（chapter_section ⋈ section，2 表，ORDER BY order_index）。 */
    @Select("SELECT sec.uri AS uri, sec.label AS label, cs.order_index AS orderIndex " +
            "FROM t_kg_chapter_section cs " +
            "JOIN t_kg_section sec ON sec.uri = cs.section_uri AND sec.is_deleted = false " +
            "WHERE cs.chapter_uri = #{chapterUri} AND cs.is_deleted = false " +
            "ORDER BY cs.order_index")
    List<KgTreeNode> selectSectionsByChapterUri(@Param("chapterUri") String chapterUri);

    /** 小节 → 知识点（section_kp ⋈ knowledge_point，2 表，ORDER BY order_index）。 */
    @Select("SELECT kp.uri AS uri, kp.label AS label, skp.order_index AS orderIndex " +
            "FROM t_kg_section_kp skp " +
            "JOIN t_kg_knowledge_point kp ON kp.uri = skp.kp_uri AND kp.is_deleted = false " +
            "WHERE skp.section_uri = #{sectionUri} AND skp.is_deleted = false " +
            "ORDER BY skp.order_index")
    List<KgTreeNode> selectKnowledgePointsBySectionUri(@Param("sectionUri") String sectionUri);
}
