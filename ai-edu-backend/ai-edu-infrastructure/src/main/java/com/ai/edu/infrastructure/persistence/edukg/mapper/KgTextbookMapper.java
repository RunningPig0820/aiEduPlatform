package com.ai.edu.infrastructure.persistence.edukg.mapper;

import com.ai.edu.infrastructure.persistence.edukg.po.KgTextbookPo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 教材Mapper接口
 */
@Mapper
@DS("kg")
public interface KgTextbookMapper extends BaseMapper<KgTextbookPo> {

    @Select("SELECT * FROM t_kg_textbook WHERE uri = #{uri} AND is_deleted = false")
    KgTextbookPo selectByUri(@Param("uri") String uri);

    @Select("SELECT * FROM t_kg_textbook WHERE subject = #{subject} AND is_deleted = false ORDER BY grade")
    List<KgTextbookPo> selectBySubject(@Param("subject") String subject);

    @Select("SELECT * FROM t_kg_textbook WHERE subject = #{subject} AND stage = #{stage} AND is_deleted = false ORDER BY grade")
    List<KgTextbookPo> selectBySubjectAndStage(@Param("subject") String subject, @Param("stage") String stage);

    @Select("SELECT * FROM t_kg_textbook WHERE is_deleted = false ORDER BY grade, order_index")
    List<KgTextbookPo> selectAllActive();

    @Update("UPDATE t_kg_textbook SET status = #{status}, modified_by = #{modifiedBy} WHERE uri = #{uri} AND is_deleted = false")
    int updateStatus(@Param("uri") String uri, @Param("status") String status, @Param("modifiedBy") Long modifiedBy);

    @Select("SELECT DISTINCT grade FROM t_kg_textbook WHERE is_deleted = false ORDER BY grade")
    List<String> selectDistinctGrades();

    @Select("SELECT DISTINCT grade FROM t_kg_textbook WHERE subject = #{subject} AND is_deleted = false ORDER BY grade")
    List<String> selectDistinctGradesBySubject(@Param("subject") String subject);
}
