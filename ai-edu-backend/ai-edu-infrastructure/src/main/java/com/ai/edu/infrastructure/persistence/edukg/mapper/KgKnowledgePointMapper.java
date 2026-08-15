package com.ai.edu.infrastructure.persistence.edukg.mapper;

import com.ai.edu.domain.edukg.model.valueobject.KgKpPlacement;
import com.ai.edu.infrastructure.persistence.edukg.po.KgKnowledgePointPo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 知识点Mapper接口
 */
@Mapper
@DS("kg")
public interface KgKnowledgePointMapper extends BaseMapper<KgKnowledgePointPo> {

    @Select("SELECT * FROM t_kg_knowledge_point WHERE uri = #{uri} AND is_deleted = false")
    KgKnowledgePointPo selectByUri(@Param("uri") String uri);

    /** 按知识点名精确匹配（答疑 label→URI 解析，limit 1 取最先收录）。 */
    @Select("SELECT * FROM t_kg_knowledge_point WHERE label = #{label} AND is_deleted = false LIMIT 1")
    KgKnowledgePointPo selectByLabel(@Param("label") String label);

    /** 按知识点名模糊匹配（答疑 label→URI 解析兜底，limit 1）。 */
    @Select("SELECT * FROM t_kg_knowledge_point WHERE label LIKE CONCAT('%', #{label}, '%') AND is_deleted = false LIMIT 1")
    KgKnowledgePointPo selectByLabelLike(@Param("label") String label);

    /** 按知识点名模糊召回多个候选（LLM 消歧候选列表，limit 10）。 */
    @Select("SELECT * FROM t_kg_knowledge_point WHERE label LIKE CONCAT('%', #{label}, '%') AND is_deleted = false LIMIT 10")
    List<KgKnowledgePointPo> selectByLabelLikeList(@Param("label") String label);

    @Select("<script>" +
            "SELECT * FROM t_kg_knowledge_point WHERE uri IN " +
            "<foreach item='uri' collection='uris' open='(' separator=',' close=')'>#{uri}</foreach>" +
            " AND is_deleted = false" +
            "</script>")
    List<KgKnowledgePointPo> selectByUris(@Param("uris") List<String> uris);

    @Select("SELECT * FROM t_kg_knowledge_point WHERE status = #{status} AND is_deleted = false")
    List<KgKnowledgePointPo> selectByStatus(@Param("status") String status);

    @Update("UPDATE t_kg_knowledge_point SET status = #{status}, modified_by = #{modifiedBy} WHERE uri = #{uri} AND is_deleted = false")
    int updateStatus(@Param("uri") String uri, @Param("status") String status, @Param("modifiedBy") Long modifiedBy);

    /** 批量反查 kp 归属（kp→section→chapter→textbook 的 stage/chapter/section 投影）。 */
    @Select("<script>" +
            "SELECT kp.uri AS kpUri, kp.label AS kpLabel, tb.stage AS stage, ch.label AS chapterLabel, sec.label AS sectionLabel " +
            "FROM t_kg_knowledge_point kp " +
            "LEFT JOIN t_kg_section_kp skp ON skp.kp_uri = kp.uri AND skp.is_deleted = false " +
            "LEFT JOIN t_kg_section sec ON sec.uri = skp.section_uri AND sec.is_deleted = false " +
            "LEFT JOIN t_kg_chapter_section cs ON cs.section_uri = skp.section_uri AND cs.is_deleted = false " +
            "LEFT JOIN t_kg_chapter ch ON ch.uri = cs.chapter_uri AND ch.is_deleted = false " +
            "LEFT JOIN t_kg_textbook_chapter tc ON tc.chapter_uri = cs.chapter_uri AND tc.is_deleted = false " +
            "LEFT JOIN t_kg_textbook tb ON tb.uri = tc.textbook_uri AND tb.is_deleted = false " +
            "WHERE kp.uri IN " +
            "<foreach item='uri' collection='uris' open='(' separator=',' close=')'>#{uri}</foreach>" +
            "</script>")
    List<KgKpPlacement> selectPlacementByUris(@Param("uris") List<String> uris);

    /** 按学段分页列教材知识点（textbook[stage]→chapter→section→kp 反向 JOIN）。 */
    @Select("SELECT kp.uri AS kpUri, kp.label AS kpLabel, tb.stage AS stage, ch.label AS chapterLabel, sec.label AS sectionLabel " +
            "FROM t_kg_textbook tb " +
            "JOIN t_kg_textbook_chapter tc ON tc.textbook_uri = tb.uri AND tc.is_deleted = false " +
            "JOIN t_kg_chapter ch ON ch.uri = tc.chapter_uri AND ch.is_deleted = false " +
            "JOIN t_kg_chapter_section cs ON cs.chapter_uri = ch.uri AND cs.is_deleted = false " +
            "JOIN t_kg_section sec ON sec.uri = cs.section_uri AND sec.is_deleted = false " +
            "JOIN t_kg_section_kp skp ON skp.section_uri = sec.uri AND skp.is_deleted = false " +
            "JOIN t_kg_knowledge_point kp ON kp.uri = skp.kp_uri AND kp.is_deleted = false " +
            "WHERE tb.stage = #{stage} AND tb.is_deleted = false " +
            "GROUP BY kp.uri, kp.label, tb.stage, ch.label, sec.label " +
            "ORDER BY kp.uri LIMIT #{limit} OFFSET #{offset}")
    List<KgKpPlacement> selectPageByStage(@Param("stage") String stage, @Param("offset") int offset, @Param("limit") int limit);

    /** 某学段教材知识点总数。 */
    @Select("SELECT COUNT(DISTINCT kp.uri) " +
            "FROM t_kg_textbook tb " +
            "JOIN t_kg_textbook_chapter tc ON tc.textbook_uri = tb.uri AND tc.is_deleted = false " +
            "JOIN t_kg_chapter ch ON ch.uri = tc.chapter_uri AND ch.is_deleted = false " +
            "JOIN t_kg_chapter_section cs ON cs.chapter_uri = ch.uri AND cs.is_deleted = false " +
            "JOIN t_kg_section sec ON sec.uri = cs.section_uri AND sec.is_deleted = false " +
            "JOIN t_kg_section_kp skp ON skp.section_uri = sec.uri AND skp.is_deleted = false " +
            "JOIN t_kg_knowledge_point kp ON kp.uri = skp.kp_uri AND kp.is_deleted = false " +
            "WHERE tb.stage = #{stage} AND tb.is_deleted = false")
    long countByStage(@Param("stage") String stage);
}
