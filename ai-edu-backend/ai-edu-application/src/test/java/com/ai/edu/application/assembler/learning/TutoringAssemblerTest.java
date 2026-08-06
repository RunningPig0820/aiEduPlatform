package com.ai.edu.application.assembler.learning;

import com.ai.edu.application.dto.learning.ChatMessageDTO;
import com.ai.edu.application.dto.learning.TutoringSessionDTO;
import com.ai.edu.domain.learning.model.contract.MasterySignalItem;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TutoringAssembler 转换测试。
 */
class TutoringAssemblerTest {

    private final TutoringAssembler assembler = new TutoringAssembler();

    @Test
    @DisplayName("toSessionDTO 映射领域会话（状态名、无 questionContent）")
    void toSessionDTO_shouldMap() {
        TutoringSession session = TutoringSession.start(1001L, "math");
        session.complete(com.ai.edu.domain.learning.model.valueobject.EndReason.COMPLETED);

        TutoringSessionDTO dto = assembler.toSessionDTO(session,
                List.of(ChatMessageDTO.builder().role("user").content("鸡兔同笼").build()),
                null);

        assertEquals(session.getId(), dto.getSessionId());
        assertEquals(TutoringState.ARCHIVED.name(), dto.getStatus());
        assertEquals("math", dto.getSubject());
        assertEquals(1, dto.getRecentMessages().size());
        assertEquals("鸡兔同笼", dto.getRecentMessages().get(0).getContent());
        assertNull(dto.getSummary());
    }

    @Test
    @DisplayName("toMasterySignals 转换 signal 小写并跳过空 label")
    void toMasterySignals_shouldConvertAndSkipDirty() {
        List<MasterySignal> signals = assembler.toMasterySignals(List.of(
                MasterySignalItem.builder().kpLabel("二元一次方程组").signal("practicing").build(),
                MasterySignalItem.builder().kpLabel("").signal("mastered").build(),
                MasterySignalItem.builder().kpLabel("  ").signal("mastered").build()
        ));

        assertEquals(1, signals.size());
        assertEquals("二元一次方程组", signals.get(0).getKpLabel());
        assertEquals(MasterySignal.Level.PRACTICING, signals.get(0).getSignal());
    }

    @Test
    @DisplayName("toMasterySignals 空/null 输入返回空列表")
    void toMasterySignals_shouldHandleEmpty() {
        assertTrue(assembler.toMasterySignals(null).isEmpty());
        assertTrue(assembler.toMasterySignals(List.of()).isEmpty());
    }
}
