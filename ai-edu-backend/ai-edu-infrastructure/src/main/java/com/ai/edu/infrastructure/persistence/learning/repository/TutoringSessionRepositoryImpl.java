package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.repository.TutoringSessionRepository;
import com.ai.edu.infrastructure.persistence.learning.mapper.TutoringSessionMapper;
import com.ai.edu.infrastructure.persistence.learning.po.TutoringSessionPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 答疑会话仓储实现（PO ↔ 聚合根桥接，Mapper 经 {@code @DS("learning")} 路由 ai_edu_learning）。
 */
@Repository
public class TutoringSessionRepositoryImpl implements TutoringSessionRepository {

    @Resource
    private TutoringSessionMapper tutoringSessionMapper;

    @Override
    public TutoringSession save(TutoringSession session) {
        TutoringSessionPo po = TutoringSessionPo.from(session);
        if (po.getId() == null) {
            tutoringSessionMapper.insert(po);
            session.setId(po.getId());
        } else {
            tutoringSessionMapper.updateById(po);
        }
        return session;
    }

    @Override
    public Optional<TutoringSession> findById(Long sessionId) {
        TutoringSessionPo po = tutoringSessionMapper.selectById(sessionId);
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }

    @Override
    public List<TutoringSession> findActiveByStudentId(Long studentId) {
        return TutoringSessionPo.toEntityList(tutoringSessionMapper.selectActiveByStudentId(studentId));
    }

    @Override
    public void updateTranscriptUrl(Long sessionId, String transcriptUrl) {
        tutoringSessionMapper.updateTranscriptUrl(sessionId, transcriptUrl, 0L);
    }
}
