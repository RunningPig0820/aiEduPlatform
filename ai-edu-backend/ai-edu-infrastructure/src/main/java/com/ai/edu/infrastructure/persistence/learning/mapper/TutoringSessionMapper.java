package com.ai.edu.infrastructure.persistence.learning.mapper;

import com.ai.edu.infrastructure.persistence.learning.po.TutoringSessionPo;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 答疑会话 Mapper（路由 ai_edu_learning 库）。
 */
@Mapper
@DS("learning")
public interface TutoringSessionMapper extends BaseMapper<TutoringSessionPo> {

    /** 查该学生全部 ACTIVE 会话（断点恢复/活跃数统计）。 */
    @Select("SELECT * FROM t_tutoring_session WHERE student_id = #{studentId} AND status = 'ACTIVE' AND is_deleted = false ORDER BY id")
    List<TutoringSessionPo> selectActiveByStudentId(@Param("studentId") Long studentId);

    /** 回填 COS 对话归档 objectKey（首次实时写即调用）。 */
    @Update("UPDATE t_tutoring_session SET transcript_url = #{transcriptUrl}, modified_by = #{modifiedBy} " +
            "WHERE id = #{id} AND is_deleted = false")
    int updateTranscriptUrl(@Param("id") Long id, @Param("transcriptUrl") String transcriptUrl,
                            @Param("modifiedBy") Long modifiedBy);
}
