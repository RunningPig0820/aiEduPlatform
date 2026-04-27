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

    @Update("UPDATE t_kg_textbook SET status = #{status}, modified_by = #{modifiedBy} WHERE uri = #{uri} AND is_deleted = false")
    int updateStatus(@Param("uri") String uri, @Param("status") String status, @Param("modifiedBy") Long modifiedBy);

    @Select("SELECT DISTINCT grade FROM t_kg_textbook WHERE subject = #{subject} AND is_deleted = false ORDER BY grade")
    List<String> selectDistinctGradesBySubject(@Param("subject") String subject);

    /**
     * 按版本+学科查询不重复的年级列表（动态 SQL，参数为 null 时不过滤）
     */
    @Select("<script>" +
            "SELECT DISTINCT grade FROM t_kg_textbook WHERE is_deleted = false " +
            "<if test='edition != null'> AND edition = #{edition}</if>" +
            "<if test='subject != null'> AND subject = #{subject}</if>" +
            " ORDER BY grade" +
            "</script>")
    List<String> selectDistinctGradesByEditionSubject(
            @Param("edition") String edition,
            @Param("subject") String subject);

    /**
     * 按版本+学科查询教材列表（动态 SQL，参数为 null 时不过滤）
     */
    @Select("<script>" +
            "SELECT * FROM t_kg_textbook WHERE is_deleted = false " +
            "<if test='edition != null'> AND edition = #{edition}</if>" +
            "<if test='subject != null'> AND subject = #{subject}</if>" +
            " ORDER BY grade" +
            "</script>")
    List<KgTextbookPo> selectByEditionSubject(
            @Param("edition") String edition,
            @Param("subject") String subject);

    @Select("SELECT * FROM t_kg_textbook WHERE edition = #{edition} AND subject = #{subject} "
            + "AND grade = #{grade} AND is_deleted = false ORDER BY grade, sort")
    List<KgTextbookPo> selectAllActiveByEditionSubjectGrade(
            @Param("edition") String edition,
            @Param("subject") String subject,
            @Param("grade") String grade);

    @Select("SELECT * FROM t_kg_textbook WHERE grade = #{grade} AND is_deleted = false ORDER BY sort")
    List<KgTextbookPo> selectAllActiveByGrade(@Param("grade") String grade);
}
