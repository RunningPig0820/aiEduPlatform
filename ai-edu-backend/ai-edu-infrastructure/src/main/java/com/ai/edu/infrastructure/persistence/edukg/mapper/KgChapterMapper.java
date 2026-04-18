package com.ai.edu.infrastructure.persistence.edukg.mapper;

import com.ai.edu.infrastructure.persistence.edukg.po.KgChapterPo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 章节Mapper接口
 */
@Mapper
@DS("kg")
public interface KgChapterMapper extends BaseMapper<KgChapterPo> {

    @Select("SELECT * FROM t_kg_chapter WHERE uri = #{uri} AND is_deleted = false")
    KgChapterPo selectByUri(@Param("uri") String uri);

    @Select("<script>" +
            "SELECT * FROM t_kg_chapter WHERE uri IN " +
            "<foreach item='uri' collection='uris' open='(' separator=',' close=')'>#{uri}</foreach>" +
            " AND is_deleted = false" +
            "</script>")
    List<KgChapterPo> selectByUris(@Param("uris") List<String> uris);

    @Select("SELECT * FROM t_kg_chapter WHERE status = #{status} AND is_deleted = false")
    List<KgChapterPo> selectByStatus(@Param("status") String status);

    @Update("UPDATE t_kg_chapter SET status = #{status}, modified_by = #{modifiedBy} WHERE uri = #{uri} AND is_deleted = false")
    int updateStatus(@Param("uri") String uri, @Param("status") String status, @Param("modifiedBy") Long modifiedBy);
}
