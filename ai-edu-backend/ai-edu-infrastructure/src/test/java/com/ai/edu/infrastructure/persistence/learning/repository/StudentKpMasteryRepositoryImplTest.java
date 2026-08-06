package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.StudentKpMastery;
import com.ai.edu.domain.learning.model.valueobject.KpKey;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.ai.edu.infrastructure.persistence.learning.mapper.StudentKpMasteryMapper;
import com.ai.edu.infrastructure.persistence.learning.po.StudentKpMasteryPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 学生知识点掌握度仓储实现单测（mock Mapper，验证 UPSERT 桥接）。
 */
class StudentKpMasteryRepositoryImplTest {

    private static final Long STUDENT_ID = 501L;
    private static final String KP_URI = "http://edukg.org/knowledge/3.1/textbook/kp1";

    private StudentKpMasteryRepositoryImpl repo;
    private StudentKpMasteryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = mock(StudentKpMasteryMapper.class);
        repo = new StudentKpMasteryRepositoryImpl();
        setField(repo, "studentKpMasteryMapper", mapper);
    }

    @Test
    @DisplayName("upsert：PO 落库并返回实体")
    void upsert_persists() {
        StudentKpMastery mastery = StudentKpMastery.create(STUDENT_ID, KpKey.of(KP_URI), "二元一次方程组");
        mastery.applySignal(MasterySignal.of("二元一次方程组", MasterySignal.Level.PRACTICING));
        when(mapper.upsert(any())).thenReturn(1);

        StudentKpMastery result = repo.upsert(mastery);

        verify(mapper).upsert(argThat(po ->
                KP_URI.equals(po.getKpKey()) && 50 == po.getMasteryLevel()
                        && STUDENT_ID.equals(po.getStudentId())));
        assertEquals(STUDENT_ID, result.getStudentId());
        assertEquals(50, result.getMasteryLevel().getValue());
    }

    @Test
    @DisplayName("findByStudentAndKp：PO → 实体")
    void findByStudentAndKp() {
        StudentKpMastery mastery = StudentKpMastery.create(STUDENT_ID, KpKey.of(KP_URI), "二元一次方程组");
        mastery.applySignal(MasterySignal.of("二元一次方程组", MasterySignal.Level.MASTERED));
        when(mapper.selectByStudentAndKp(STUDENT_ID, KP_URI))
                .thenReturn(StudentKpMasteryPo.from(mastery));

        Optional<StudentKpMastery> found = repo.findByStudentAndKp(STUDENT_ID, KpKey.of(KP_URI));

        assertTrue(found.isPresent());
        assertEquals(75, found.get().getMasteryLevel().getValue());
    }

    @Test
    @DisplayName("findByStudentId：返回全部掌握度")
    void findByStudentId() {
        StudentKpMastery mastery = StudentKpMastery.create(STUDENT_ID, KpKey.of(KP_URI), "二元一次方程组");
        when(mapper.selectByStudentId(STUDENT_ID)).thenReturn(List.of(StudentKpMasteryPo.from(mastery)));

        List<StudentKpMastery> list = repo.findByStudentId(STUDENT_ID);

        assertEquals(1, list.size());
        assertEquals(KP_URI, list.get(0).getKpKey().getValue());
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
