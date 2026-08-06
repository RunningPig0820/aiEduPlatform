package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.ErrorEvent;
import com.ai.edu.domain.learning.model.valueobject.TutoringEmotion;
import com.ai.edu.infrastructure.persistence.learning.mapper.ErrorEventMapper;
import com.ai.edu.infrastructure.persistence.learning.po.ErrorEventPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 答疑错误事件仓储实现单测（mock Mapper，验证 PO ↔ 实体桥接）。
 */
class ErrorEventRepositoryImplTest {

    private static final Long STUDENT_ID = 501L;
    private static final Long SESSION_ID = 1001L;

    private ErrorEventRepositoryImpl repo;
    private ErrorEventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = mock(ErrorEventMapper.class);
        repo = new ErrorEventRepositoryImpl();
        setField(repo, "errorEventMapper", mapper);
    }

    @Test
    @DisplayName("save：insert 并回填主键")
    void save_persists() {
        ErrorEvent event = ErrorEvent.create(STUDENT_ID, SESSION_ID, null,
                "COMPUTATION", TutoringEmotion.CONFUSED, 1, "2x+4(35-x)=94");
        when(mapper.insert(any())).thenAnswer(inv -> {
            ErrorEventPo po = inv.getArgument(0);
            setField(po, "id", 2001L);
            return 1;
        });

        ErrorEvent saved = repo.save(event);

        assertEquals(2001L, saved.getId());
        verify(mapper).insert(argThat(po ->
                STUDENT_ID.equals(po.getStudentId())
                        && SESSION_ID.equals(po.getSessionId())
                        && "CONFUSED".equals(po.getEmotion())
                        && "2x+4(35-x)=94".equals(po.getStudentAnswer())));
    }

    @Test
    @DisplayName("findByStudentId：PO → 实体列表")
    void findByStudentId() {
        ErrorEvent event = ErrorEvent.create(STUDENT_ID, SESSION_ID, null,
                "COMPUTATION", TutoringEmotion.FRUSTRATED, 2, "不会");
        when(mapper.selectByStudentId(STUDENT_ID)).thenReturn(List.of(ErrorEventPo.from(event)));

        List<ErrorEvent> list = repo.findByStudentId(STUDENT_ID);

        assertEquals(1, list.size());
        assertEquals("COMPUTATION", list.get(0).getErrorType());
        assertEquals(TutoringEmotion.FRUSTRATED, list.get(0).getEmotion());
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
