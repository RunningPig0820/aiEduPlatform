package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import com.ai.edu.infrastructure.persistence.learning.mapper.TutoringSessionMapper;
import com.ai.edu.infrastructure.persistence.learning.po.TutoringSessionPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 答疑会话仓储实现单测（mock Mapper，验证 PO ↔ 聚合根桥接）。
 */
class TutoringSessionRepositoryImplTest {

    private static final long STUDENT_ID = 501L;
    private static final long SESSION_ID = 1001L;

    private TutoringSessionRepositoryImpl repo;
    private TutoringSessionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = mock(TutoringSessionMapper.class);
        repo = new TutoringSessionRepositoryImpl();
        setField(repo, "tutoringSessionMapper", mapper);
    }

    @Test
    @DisplayName("save 新会话：insert 并回填主键")
    void save_newInsertsAndSetsId() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        when(mapper.insert(any())).thenAnswer(inv -> {
            TutoringSessionPo po = inv.getArgument(0);
            setField(po, "id", SESSION_ID);
            return 1;
        });

        TutoringSession saved = repo.save(session);

        assertEquals(SESSION_ID, saved.getId());
        verify(mapper).insert(any());
        verify(mapper, never()).updateById(any());
    }

    @Test
    @DisplayName("save 已存在：updateById")
    void save_existingUpdates() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);

        repo.save(session);

        verify(mapper).updateById(any());
        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName("findById：PO → 聚合根，计数/状态完整恢复")
    void findById_restoresAggregate() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        session.recordRound();
        when(mapper.selectById(SESSION_ID)).thenReturn(TutoringSessionPo.from(session));

        Optional<TutoringSession> found = repo.findById(SESSION_ID);

        assertTrue(found.isPresent());
        assertEquals(SESSION_ID, found.get().getId());
        assertEquals(TutoringState.ACTIVE, found.get().getStatus());
        assertEquals(1, found.get().getRoundCount());
    }

    @Test
    @DisplayName("findActiveByStudentId：返回 ACTIVE 会话列表")
    void findActiveByStudentId() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        when(mapper.selectActiveByStudentId(STUDENT_ID)).thenReturn(List.of(TutoringSessionPo.from(session)));

        List<TutoringSession> list = repo.findActiveByStudentId(STUDENT_ID);

        assertEquals(1, list.size());
        assertEquals(SESSION_ID, list.get(0).getId());
    }

    @Test
    @DisplayName("findListByStudentId：委托 Mapper 返回该学生全部会话（历史列表）")
    void findListByStudentId() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        when(mapper.selectListByStudentId(STUDENT_ID)).thenReturn(List.of(TutoringSessionPo.from(session)));

        List<TutoringSession> list = repo.findListByStudentId(STUDENT_ID);

        assertEquals(1, list.size());
        assertEquals(SESSION_ID, list.get(0).getId());
        verify(mapper).selectListByStudentId(STUDENT_ID);
    }

    @Test
    @DisplayName("softDelete：委托 deleteById（全局逻辑删 is_deleted=1，非物理删）")
    void softDelete() {
        repo.softDelete(SESSION_ID);

        verify(mapper).deleteById(SESSION_ID);
    }

    @Test
    @DisplayName("updateTranscriptUrl：委托 Mapper 回填 objectKey")
    void updateTranscriptUrl() {
        repo.updateTranscriptUrl(SESSION_ID, "tutoring/transcripts/1001.json");

        verify(mapper).updateTranscriptUrl(SESSION_ID, "tutoring/transcripts/1001.json", 0L);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
