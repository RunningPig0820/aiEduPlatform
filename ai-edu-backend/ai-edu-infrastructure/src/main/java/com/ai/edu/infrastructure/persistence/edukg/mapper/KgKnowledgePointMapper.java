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
