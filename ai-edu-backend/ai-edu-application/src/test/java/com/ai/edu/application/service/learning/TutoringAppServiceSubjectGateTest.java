package com.ai.edu.application.service.learning;

import com.ai.edu.application.assembler.learning.TutoringAssembler;
import com.ai.edu.domain.learning.model.contract.SubjectClassifyRequest;
import com.ai.edu.domain.learning.model.contract.SubjectClassifyResult;
import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.repository.ErrorEventRepository;
import com.ai.edu.domain.learning.repository.StudentQuestionRecordRepository;
import com.ai.edu.domain.learning.repository.StudentTopicMasteryRepository;
import com.ai.edu.domain.learning.repository.TutoringSessionCache;
import com.ai.edu.domain.learning.repository.TutoringSessionRepository;
import com.ai.edu.domain.learning.service.SubjectClassifyPort;
import com.ai.edu.domain.learning.service.TutoringConfig;
import com.ai.edu.domain.learning.service.TutoringKpResolver;
import com.ai.edu.domain.learning.service.TutoringLlmPort;
import com.ai.edu.domain.shared.service.FileStorageService;
import com.ai.edu.domain.shared.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学科门（subject-classify 分流，tasks 2.2-2.6）编排层测试：
 * 拍题非数学跳过（不建会话/零写入/返回提示）、数学放行、换题非数学跳过、classify 失败降级、重复幂等。
 *
 * <p>覆盖 test.md 3.2 GATE 全部用例（GATE-001 ~ GATE-006）。
 */
class TutoringAppServiceSubjectGateTest {

    private static final long STUDENT_ID = 501L;
    private static final long SESSION_ID = 1001L;
    private static final String OUT_OF_SCOPE_MSG = "目前仅支持数学答疑，换一道数学题试试吧。";

    private TutoringAppService service;
    private TutoringSessionRepository sessionRepository;
    private StudentTopicMasteryRepository studentTopicMasteryRepository;
    private StudentQuestionRecordRepository questionRecordRepository;
    private ErrorEventRepository errorEventRepository;
    private TutoringSessionCache sessionCache;
    private TutoringLlmPort llmPort;
    private SubjectClassifyPort subjectClassifyPort;
    private FileStorageService fileStorageService;
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        service = new TutoringAppService();
        sessionRepository = mock(TutoringSessionRepository.class);
        studentTopicMasteryRepository = mock(StudentTopicMasteryRepository.class);
        errorEventRepository = mock(ErrorEventRepository.class);
        sessionCache = mock(TutoringSessionCache.class);
        llmPort = mock(TutoringLlmPort.class);
        subjectClassifyPort = mock(SubjectClassifyPort.class);
        fileStorageService = mock(FileStorageService.class);
        redisService = mock(RedisService.class);

        service.setTutoringSessionRepository(sessionRepository);
        service.setStudentTopicMasteryRepository(studentTopicMasteryRepository);
        service.setErrorEventRepository(errorEventRepository);
        service.setSessionCache(sessionCache);
        service.setLlmPort(llmPort);
        service.setSubjectClassifyPort(subjectClassifyPort);
        service.setKpResolver(mock(TutoringKpResolver.class));
        service.setGuardrail(new TutoringGuardrailService());
        service.setContextAssembler(new TutoringContextAssembler());
        service.setTranscriptArchiver(mock(TutoringTranscriptArchiver.class));
        service.setAssembler(new TutoringAssembler());
        service.setFileStorageService(fileStorageService);
        service.setTutoringConfig(TutoringConfig.defaults());
        service.setRedisService(redisService);
        questionRecordRepository = mock(StudentQuestionRecordRepository.class);
        when(questionRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.setQuestionRecordRepository(questionRecordRepository);
        service.setTopicLabelAggregationService(mock(TopicLabelAggregationService.class));
        service.setArchiveScheduler(Schedulers.immediate());

        when(redisService.tryLock(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(sessionCache.tryIncrementCreateCount(eq(STUDENT_ID), anyInt(), anyInt())).thenReturn(true);
        when(fileStorageService.uploadToObjectKey(anyString(), any(byte[].class), anyString())).thenReturn(null);
        when(fileStorageService.getUrl(anyString()))
                .thenAnswer(inv -> "https://cos/" + inv.getArgument(0));
        when(studentTopicMasteryRepository.findByStudentId(eq(STUDENT_ID))).thenReturn(List.of());
        when(sessionRepository.save(any())).thenAnswer(inv -> {
            TutoringSession s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(SESSION_ID);
            }
            return s;
        });
    }

    // ==================== GATE-001 / GATE-005：拍题非数学跳过 ====================

    @Test
    @DisplayName("GATE-001: 拍题物理题（文字）→ 不建会话/不调 decide/generate/零写入，返回「仅支持数学」提示流")
    void start_physicsText_skips() {
        mockClassify("physics");

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, "自由落体运动的问题");

        StepVerifier.create(stream)
                .assertNext(ev -> {
                    assertEquals("meta", ev.event());
                    assertTrue(ev.data().contains("\"sessionId\":null"), ev.data());
                    assertTrue(ev.data().contains("\"type\":\"hint\""), ev.data());
                    // 非数学跳过流 meta 不带 subject（subject=null，前端只展示提示语、隐藏学科行）
                    assertTrue(ev.data().contains("\"subject\":null"), ev.data());
                })
                .assertNext(ev -> {
                    assertEquals("token", ev.event());
                    assertTrue(ev.data().contains(OUT_OF_SCOPE_MSG), ev.data());
                })
                .assertNext(ev -> {
                    assertEquals("done", ev.event());
                    assertTrue(ev.data().contains("\"sessionId\":null"), ev.data());
                })
                .verifyComplete();
        assertNoWrite();
        verify(llmPort, never()).decideStream(any());
        verify(llmPort, never()).generate(any());
    }

    @Test
    @DisplayName("GATE-005: 重复发物理题 → 每次均跳过，零写入，不建会话")
    void start_physicsRepeated_idempotent() {
        mockClassify("physics");

        service.start(STUDENT_ID, "自由落体运动的问题").blockLast();
        service.start(STUDENT_ID, "自由落体运动的问题").blockLast();

        verify(subjectClassifyPort, times(2)).classify(any());
        verify(sessionRepository, never()).save(any());
        verify(llmPort, never()).decideStream(any());
        assertNoWrite();
    }

    @Test
    @DisplayName("拍题物理题（图片）→ 不建会话/零写入，返回提示（subject-check 预检上传不落库）")
    void start_physicsImage_skips() {
        mockClassify("physics");
        when(fileStorageService.getUrl(anyString()))
                .thenAnswer(inv -> "https://cos/" + inv.getArgument(0));

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, null,
                new byte[]{1, 2, 3}, "physics.png");

        StepVerifier.create(stream)
                .assertNext(ev -> {
                    assertEquals("meta", ev.event());
                    assertTrue(ev.data().contains("\"sessionId\":null"), ev.data());
                })
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        assertNoWrite();
        verify(llmPort, never()).decideStream(any());
    }

    // ==================== GATE-002 / GATE-006：拍题数学放行 ====================

    @Test
    @DisplayName("GATE-002: 拍题数学题（文字）→ 建会话 + decide + 落库")
    void start_mathText_flows() {
        mockClassify("math");
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先找已知条件\"}")));

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, "鸡兔同笼，共35头94脚，各几只？");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(guardrail)
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(memory)
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        verify(sessionRepository, org.mockito.Mockito.atLeastOnce()).save(any());
        verify(llmPort, org.mockito.Mockito.atLeastOnce()).decideStream(any());
    }

    @Test
    @DisplayName("GATE-006: 会话 subject 记录真实值（classify=math → subject=math，非硬编码默认）")
    void start_math_subjectRecorded() {
        mockClassify("math");
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先找已知条件\"}")));

        service.start(STUDENT_ID, "鸡兔同笼").blockLast();

        ArgumentCaptor<TutoringSession> captor = ArgumentCaptor.forClass(TutoringSession.class);
        verify(sessionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        TutoringSession saved = captor.getAllValues().get(0);
        assertEquals("math", saved.getSubject(), "会话 subject 应为 classify 结果 math");
        verify(subjectClassifyPort).classify(any());
    }

    // ==================== subject 字段（meta 契约收尾） ====================

    @Test
    @DisplayName("GATE-007: 拍题建会话正常轮 meta 带 subject（= 会话真实 subject，非硬编码）")
    void start_math_metaCarriesSubject() {
        mockClassify("math");
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先找已知条件\"}")));

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, "鸡兔同笼，共35头94脚，各几只？");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(guardrail)
                .assertNext(ev -> {
                    assertEquals("meta", ev.event());
                    assertTrue(ev.data().contains("\"subject\":\"math\""), ev.data());
                })
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(memory)
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
    }

    @Test
    @DisplayName("GATE-008: 发消息/换题走 switch 正常轮 meta 带 subject（= 会话真实 subject）")
    void sendMessage_metaCarriesSubject() {
        activeSessionInCache();   // subject=math
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("switch")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"新题我们开始\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "换一题");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(guardrail)
                .assertNext(ev -> {
                    assertEquals("meta", ev.event());
                    assertTrue(ev.data().contains("\"subject\":\"math\""), ev.data());
                })
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(memory)
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
    }

    @Test
    @DisplayName("GATE-009: subject 缺失不报错——非数学跳过流序列化正常，subject 为 null（前端隐藏该行）")
    void subjectMissing_noError_subjectNull() {
        mockClassify("physics");

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, "自由落体运动的问题");

        // 流正常结束（meta 序列化不抛异常），meta.subject=null（可空字段，前端隐藏学科行）
        StepVerifier.create(stream)
                .assertNext(ev -> {
                    assertEquals("meta", ev.event());
                    assertTrue(ev.data().contains("\"subject\":null"), ev.data());
                })
                .expectNextCount(2)   // token + done
                .verifyComplete();
        assertNoWrite();
        verify(llmPort, never()).decideStream(any());
    }

    // ==================== GATE-003：换题非数学跳过 ====================

    @Test
    @DisplayName("GATE-003: 数学会话中传化学题图 → 新题跳过（不结算/不记录），返回提示，原会话不受影响")
    void sendMessage_newChemistryImage_skips() {
        TutoringSession session = activeSessionInCache();
        mockClassify("chemistry");
        when(fileStorageService.getUrl(anyString()))
                .thenAnswer(inv -> "https://cos/" + inv.getArgument(0));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, null,
                new byte[]{1, 2, 3}, "chem.png");

        StepVerifier.create(stream)
                .assertNext(ev -> {
                    assertEquals("meta", ev.event());
                    assertTrue(ev.data().contains("\"sessionId\":" + SESSION_ID), ev.data());
                    assertTrue(ev.data().contains("\"type\":\"hint\""), ev.data());
                })
                .assertNext(ev -> {
                    assertEquals("token", ev.event());
                    assertTrue(ev.data().contains(OUT_OF_SCOPE_MSG), ev.data());
                })
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        // 不追加新题消息（原会话 history 不受影响）、不调 decide、不结算旧题
        verify(sessionCache, never()).appendMessage(eq(SESSION_ID), argThat(m -> m.getImageUrl() != null));
        verify(llmPort, never()).decideStream(any());
        verify(questionRecordRepository, never()).save(any());
        verify(studentTopicMasteryRepository, never()).upsert(any());
        assertTrue(session.isActive(), "原会话保持 ACTIVE");
        assertNull(session.getCurrentAttempt(), "新题未进入，无聚合");
        // 锁已释放（临界区跳过）
        verify(redisService).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("GATE-003b: 纯文本轮（非新题）不重复判学科——只对「新题进入」判")
    void sendMessage_textRound_noClassify() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"继续\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "2x+4(35-x)=94").blockLast();

        verify(subjectClassifyPort, never()).classify(any());
        verify(llmPort, org.mockito.Mockito.atLeastOnce()).decideStream(any());
        assertEquals(1, session.getRoundCount());
    }

    // ==================== GATE-004：classify 失败降级 ====================

    @Test
    @DisplayName("GATE-004a: classify 返回空 subject → 按 math 放行，正常答疑")
    void classifyEmpty_mathPassThrough() {
        mockClassify(null);   // 空结果
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先找已知条件\"}")));

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, "鸡兔同笼");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        verify(sessionRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("GATE-004b: classify 抛异常 → 按 math 放行，不阻断答疑主链路")
    void classifyThrows_mathPassThrough() {
        when(subjectClassifyPort.classify(any())).thenThrow(new RuntimeException("classify down"));
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先找已知条件\"}")));

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, "鸡兔同笼");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        verify(sessionRepository, org.mockito.Mockito.atLeastOnce()).save(any());
        verify(llmPort, org.mockito.Mockito.atLeastOnce()).decideStream(any());
    }

    // ==================== helpers ====================

    private void mockClassify(String subject) {
        when(subjectClassifyPort.classify(any())).thenReturn(new SubjectClassifyResult(subject));
    }

    private TutoringSession activeSessionInCache() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        when(sessionCache.findSession(SESSION_ID)).thenReturn(Optional.of(session));
        when(sessionCache.listMessages(SESSION_ID)).thenReturn(List.of(
                TutoringChatMessage.user("鸡兔同笼"), TutoringChatMessage.ai("先找已知条件")));
        return session;
    }

    private void assertNoWrite() {
        verify(sessionRepository, never()).save(any());
        verify(questionRecordRepository, never()).save(any());
        verify(studentTopicMasteryRepository, never()).upsert(any());
        verify(errorEventRepository, never()).save(any());
    }

    private com.ai.edu.domain.learning.model.contract.ActionMeta meta(String type) {
        return com.ai.edu.domain.learning.model.contract.ActionMeta.builder().type(type).build();
    }

    private ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.<String>builder().event(event).data(data).build();
    }

    private Flux<ServerSentEvent<String>> decideStreamOf(com.ai.edu.domain.learning.model.contract.ActionMeta meta) {
        return Flux.just(sse("meta", serializeMeta(meta)), sse("done", "{}"));
    }

    private String serializeMeta(com.ai.edu.domain.learning.model.contract.ActionMeta meta) {
        try {
            return new ObjectMapper().writeValueAsString(meta);
        } catch (Exception e) {
            throw new RuntimeException("serialize meta failed", e);
        }
    }
}
