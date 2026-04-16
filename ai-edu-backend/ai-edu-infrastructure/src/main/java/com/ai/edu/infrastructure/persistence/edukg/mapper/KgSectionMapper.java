package com.ai.edu.infrastructure.persistence.edukg.mapper;

import com.ai.edu.domain.edukg.model.entity.KgSection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 小节Mapper接口
 */
@Mapper
@DS("kg")
public interface KgSectionMapper extends BaseMapper<KgSection> {

    @Select("SELECT * FROM t_kg_section WHERE uri = #{uri} AND is_deleted = false")
    KgSection selectByUri(@Param("uri") String uri);

    @Select("<script>" +
            "SELECT * FROM t_kg_section WHERE uri IN " +
            "<foreach item='uri' collection='uris' open='(' separator=',' close=')'>#{uri}</foreach>" +
            " AND is_deleted = false" +
            "</script>")
    List<KgSection> selectByUris(@Param("uris") List<String> uris);

    @Select("SELECT * FROM t_kg_section WHERE status = #{status} AND is_deleted = false")
    List<KgSection> selectByStatus(@Param("status") String status);

    @Update("UPDATE t_kg_section SET status = #{status}, modified_by = #{modifiedBy} WHERE uri = #{uri} AND is_deleted = false")
    int updateStatus(@Param("uri") String uri, @Param("status") String status, @Param("modifiedBy") Long modifiedBy);
}
