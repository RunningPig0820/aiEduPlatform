package com.ai.edu.infrastructure.persistence.edukg.mapper;

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
}
