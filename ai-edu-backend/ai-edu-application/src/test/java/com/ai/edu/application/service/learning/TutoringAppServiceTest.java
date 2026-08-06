package com.ai.edu.application.service.learning;

import com.ai.edu.application.assembler.learning.TutoringAssembler;
import com.ai.edu.application.dto.learning.GuardResult;
import com.ai.edu.application.dto.learning.StudentMasteryDTO;
import com.ai.edu.application.dto.learning.TutoringConfigDTO;
import com.ai.edu.application.dto.learning.TutoringSessionDTO;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.ActionMeta;
import com.ai.edu.domain.learning.model.contract.EvalInfo;
import com.ai.edu.domain.learning.model.contract.MasterySignalItem;
import com.ai.edu.domain.learning.model.contract.OcrResult;
import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.entity.ErrorEvent;
import com.ai.edu.domain.learning.model.entity.StudentKpMastery;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.EndReason;
import com.ai.edu.domain.learning.model.valueobject.KpKey;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import com.ai.edu.domain.learning.repository.ErrorEventRepository;
import com.ai.edu.domain.learning.repository.StudentKpMasteryRepository;
import com.ai.edu.domain.learning.repository.TutoringSessionCache;
import com.ai.edu.domain.learning.repository.TutoringSessionRepository;
import com.ai.edu.domain.learning.service.TutoringConfig;
import com.ai.edu.domain.learning.service.TutoringKpResolver;
import com.ai.edu.domain.learning.service.TutoringLlmPort;
import com.ai.edu.domain.shared.service.FileStorageService;
import com.ai.edu.domain.shared.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 答疑编排服务测试（mock TutoringLlmPort/仓储/缓存，真实护栏），覆盖 test.md 编排服务测试。
 */
class TutoringAppServiceTest {

    private static final long STUDENT_ID = 501L;
    private static final long SESSION_ID = 1001L;
    private static final String KP_URI = "http://edukg.org/kp/1";

    private TutoringAppService service;
    private TutoringSessionRepository sessionRepository;
    private StudentKpMasteryRepository masteryRepository;
    private ErrorEventRepository errorEventRepository;
    private TutoringSessionCache sessionCache;
    private TutoringLlmPort llmPort;
    private TutoringKpResolver kpResolver;
    private TutoringTranscriptArchiver transcriptArchiver;
    private FileStorageService fileStorageService;
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        service = new TutoringAppService();
        sessionRepository = mock(TutoringSessionRepository.class);
        masteryRepository = mock(StudentKpMasteryRepository.class);
        errorEventRepository = mock(ErrorEventRepository.class);
        sessionCache = mock(TutoringSessionCache.class);
        llmPort = mock(TutoringLlmPort.class);
        kpResolver = mock(TutoringKpResolver.class);
        transcriptArchiver = mock(TutoringTranscriptArchiver.class);
        fileStorageService = mock(FileStorageService.class);
        redisService = mock(RedisService.class);

        service.setTutoringSessionRepository(sessionRepository);
        service.setMasteryRepository(masteryRepository);
        service.setErrorEventRepository(errorEventRepository);
        service.setSessionCache(sessionCache);
        service.setLlmPort(llmPort);
        service.setKpResolver(kpResolver);
        service.setGuardrail(new TutoringGuardrailService());
        service.setContextAssembler(new TutoringContextAssembler());
        service.setTranscriptArchiver(transcriptArchiver);
        service.setAssembler(new TutoringAssembler());
        service.setFileStorageService(fileStorageService);
        service.setTutoringConfig(TutoringConfig.defaults());
        service.setRedisService(redisService);

        // 会话并发锁默认放行
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(sessionCache.tryIncrementCreateCount(eq(STUDENT_ID), anyInt(), anyInt())).thenReturn(true);
        when(masteryRepository.findByStudentId(eq(STUDENT_ID))).thenReturn(List.of());
        when(fileStorageService.generatePresignedUrl(anyString(), anyInt()))
                .thenAnswer(inv -> "https://cos/" + inv.getArgument(0));
        when(kpResolver.resolveLabelToUri(anyString())).thenReturn(KP_URI);
        when(masteryRepository.findByStudentAndKp(eq(STUDENT_ID), any())).thenReturn(Optional.empty());
        when(masteryRepository.upsert(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transcriptArchiver.archive(any(), any(), any(), anyList(), any(), any()))
                .thenReturn("tutoring/transcripts/" + STUDENT_ID + "/" + SESSION_ID + ".json");
        when(sessionRepository.save(any())).thenAnswer(inv -> {
            TutoringSession s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(SESSION_ID);
            }
            return s;
        });
    }

    // ==================== start() ====================

    @Test
    @DisplayName("start: decide=hint → 护栏通过 → 建会话 → meta/token/done，round 落库")
    void start_normalHintFlow() {
        when(llmPort.decide(any())).thenReturn(meta("hint"));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先找已知条件\"}")));

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, "鸡兔同笼，共35头94脚，各几只？");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        verify(sessionRepository, atLeastOnce()).save(any()); // 建会话 + 更新计数
        verify(transcriptArchiver, atLeastOnce()).archive(any(), eq(SESSION_ID), any(), anyList(), any(), any());
        verify(sessionCache, never()).clear(eq(SESSION_ID)); // ACTIVE 不清理
    }

    @Test
    @DisplayName("start: 无关内容（type=end, end_reason 空）→ TERMINATED 直接回复，不建会话、无 generate")
    void start_unrelatedTerminated() {
        ActionMeta end = meta("end");
        end.setSummary("我主要解答学科问题，请提出学习相关的内容");
        when(llmPort.decide(any())).thenReturn(end);

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, "今天天气怎么样");

        StepVerifier.create(stream)
                .assertNext(ev -> {
                    assertEquals("meta", ev.event());
                    assertTrue(ev.data().contains("TERMINATED"), ev.data());
                    assertTrue(ev.data().contains("请提出学习相关的内容"), ev.data());
                })
                .verifyComplete();
        verify(sessionRepository, never()).save(any());
        verify(llmPort, never()).generate(any());
    }

    @Test
    @DisplayName("start: 创建过于频繁 → 40003")
    void start_createTooFrequent() {
        when(sessionCache.tryIncrementCreateCount(eq(STUDENT_ID), anyInt(), anyInt())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.start(STUDENT_ID, "鸡兔同笼"));
        assertEquals("50004", ex.getCode());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("start: decide 失败重试后仍失败 → 不建会话，抛 TutoringAgentException")
    void start_decideFailure_noSession() {
        when(llmPort.decide(any())).thenThrow(new TutoringAgentException("decide 失败"));

        assertThrows(TutoringAgentException.class, () -> service.start(STUDENT_ID, "鸡兔同笼"));
        verify(sessionRepository, never()).save(any());
    }

    // ==================== sendMessage() ====================

    @Test
    @DisplayName("sendMessage: 正常一轮 hint → meta/token/done，round 递增")
    void sendMessage_normalRound() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decide(any())).thenReturn(meta("hint"));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"注意等式两边\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "2x+4(35-x)=94");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        assertEquals(1, session.getRoundCount()); // round 0→1
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("sendMessage: 答案护栏拒绝 reveal（未授权）→ meta(approach, denied=reveal) + approach 生成流，count→1")
    void sendMessage_answerDeniedToApproach() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decide(any())).thenReturn(meta("reveal"));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"思路：先设未知数\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "直接告诉我答案");

        StepVerifier.create(stream)
                .assertNext(ev -> {
                    assertEquals("meta", ev.event());
                    assertTrue(ev.data().contains("\"type\":\"approach\""), ev.data());
                    assertTrue(ev.data().contains("\"denied\":\"reveal\""), ev.data());
                })
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        assertEquals(1, session.getAnswerRequestCount());
    }

    @Test
    @DisplayName("sendMessage: 学生换题 decide=switch → 计数归零（round/answer），新题继续")
    void sendMessage_switchResetsCounters() {
        TutoringSession session = activeSessionInCache();
        session.recordRound();
        session.recordRound(); // round=2
        when(llmPort.decide(any())).thenReturn(meta("switch"));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"新题我们开始\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "换一题");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .expectNextCount(2)
                .verifyComplete();
        assertEquals(0, session.getRoundCount());
        assertEquals(TutoringState.ACTIVE, session.getStatus());
        verify(masteryRepository, never()).upsert(any()); // 旧题知识点不点亮
    }

    @Test
    @DisplayName("sendMessage: eval.correct=false → 写错误事件（含情绪/学生原答）")
    void sendMessage_writesErrorEvent() {
        TutoringSession session = activeSessionInCache();
        ActionMeta action = meta("hint");
        action.setEval(EvalInfo.builder().correct(false).errorType("COMPUTATION")
                .emotion("CONFUSED").exerciseComplete(false).build());

        when(llmPort.decide(any())).thenReturn(action);
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"再想想\"}")));
        when(sessionCache.listMessages(SESSION_ID)).thenReturn(List.of(
                TutoringChatMessage.user("设鸡x只"), TutoringChatMessage.ai("继续"), TutoringChatMessage.user("2x+4(35-x)=94")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "2x+4(35-x)=94").subscribe();

        verify(errorEventRepository).save(argThat(e ->
                "COMPUTATION".equals(e.getErrorType())
                        && "2x+4(35-x)=94".equals(e.getStudentAnswer())
                        && e.getStudentId().equals(STUDENT_ID)));
    }

    @Test
    @DisplayName("B3: switch 轮 eval.correct=false 不写错误事件（模型默认值非真实错误）")
    void sendMessage_switchRound_noErrorEvent() {
        TutoringSession session = activeSessionInCache();
        ActionMeta action = meta("switch");
        action.setEval(EvalInfo.builder().correct(false).build());
        when(llmPort.decide(any())).thenReturn(action);
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"新题开始\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "换一题").subscribe();

        verify(errorEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("B3: 首问 hint 轮 correct=false 但 error_type=null 不写错误事件（模型默认值非真实错误）")
    void sendMessage_hintNoErrorType_noErrorEvent() {
        TutoringSession session = activeSessionInCache();
        ActionMeta action = meta("hint");
        action.setEval(EvalInfo.builder().correct(false).errorType(null).build());
        when(llmPort.decide(any())).thenReturn(action);
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先找已知条件\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "一件商品打八折后240元，原价多少？").subscribe();

        verify(errorEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("B3: reveal 被拦成 approach 轮（原 type=reveal）不写错误事件")
    void sendMessage_revealDenied_noErrorEvent() {
        TutoringSession session = activeSessionInCache();
        ActionMeta action = meta("reveal");
        action.setEval(EvalInfo.builder().correct(false).build());
        when(llmPort.decide(any())).thenReturn(action);
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"思路\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "直接告诉我答案").subscribe();

        verify(errorEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("B3: end 轮 eval.correct=false 不写错误事件")
    void sendMessage_endRound_noErrorEvent() {
        TutoringSession session = activeSessionInCache();
        ActionMeta action = meta("end");
        action.setEndReason("COMPLETED");
        action.setEval(EvalInfo.builder().correct(false).build());
        when(llmPort.decide(any())).thenReturn(action);
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"解出了\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "我解出来了").subscribe();

        verify(errorEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendMessage: 会话已 ARCHIVED → 40002，不计数")
    void sendMessage_endedSession() {
        TutoringSession session = activeSessionInCache();
        session.complete(com.ai.edu.domain.learning.model.valueobject.EndReason.COMPLETED);
        when(sessionCache.findSession(SESSION_ID)).thenReturn(Optional.of(session));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendMessage(STUDENT_ID, SESSION_ID, "继续"));
        assertEquals("50003", ex.getCode());
        verify(llmPort, never()).decide(any());
    }

    @Test
    @DisplayName("sendMessage: mastery_signals 为空 → 跳过掌握度更新，不报错")
    void sendMessage_emptyMasterySignals() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decide(any())).thenReturn(meta("concept"));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"请把题目发完整\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "这题不会").subscribe();

        verify(masteryRepository, never()).upsert(any());
        verify(errorEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendMessage: decide 输出非法 type → 默认 hint 放行，不阻断")
    void sendMessage_invalidType_defaultsHint() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decide(any())).thenReturn(meta("garbage"));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"继续引导\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "这一步怎么算");

        StepVerifier.create(stream)
                .assertNext(ev -> assertTrue(ev.data().contains("\"type\":\"hint\""), ev.data()))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("AI 回复落库：流结束后 AI 回复（拼接 token）追加到 Redis 消息列表")
    void sendMessage_appendsAiReply() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decide(any())).thenReturn(meta("hint"));
        when(llmPort.generate(any())).thenReturn(Flux.just(
                sse("token", "{\"content\":\"先找\"}"),
                sse("token", "{\"content\":\"已知条件\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "设鸡x只").subscribe();

        verify(sessionCache).appendMessage(eq(SESSION_ID), argThat(m ->
                "ai".equals(m.getRole()) && "先找已知条件".equals(m.getContent())));
        // 流结束后重新整写 COS（含 AI 回复的完整对话）
        verify(transcriptArchiver, atLeastOnce()).archive(any(), eq(SESSION_ID), any(), anyList(), any(), any());
    }

    @Test
    @DisplayName("sendMessage: 只透传 Python 的 token 事件（meta/done 不泄漏为假 token）")
    void sendMessage_filtersNonTokenPythonEvents() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decide(any())).thenReturn(meta("hint"));
        when(llmPort.generate(any())).thenReturn(Flux.just(
                sse("meta", "{\"action_type\":\"hint\"}"),
                sse("token", "{\"content\":\"先找已知条件\"}"),
                sse("done", "{\"model_used\":\"provider/model\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "鸡兔同笼");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("meta", ev.event())) // Java 自建 meta
                .assertNext(ev -> {
                    assertEquals("token", ev.event());
                    assertTrue(ev.data().contains("\"content\""), ev.data());
                    assertFalse(ev.data().contains("action_type"), ev.data());
                    assertFalse(ev.data().contains("model_used"), ev.data());
                })
                .assertNext(ev -> assertEquals("done", ev.event())) // Java 自建 done
                .verifyComplete();
    }

    @Test
    @DisplayName("sendMessage: 掌握度信号命中 → UPSERT（signal=practicing → 50）")
    void sendMessage_appliesMasterySignal() {
        TutoringSession session = activeSessionInCache();
        ActionMeta action = meta("hint");
        action.setMasterySignals(List.of(
                MasterySignalItem.builder().kpLabel("二元一次方程组").signal("practicing").build()));
        when(llmPort.decide(any())).thenReturn(action);
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"很好\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "我解出来了").subscribe();

        verify(masteryRepository).upsert(argThat(m ->
                m.getKpKey().getValue().equals(KP_URI)
                        && m.getMasteryLevel().getValue() == 50
                        && m.getKpLabel().equals("二元一次方程组")));
    }

    // ==================== requestAnswer() ====================

    @Test
    @DisplayName("requestAnswer: 第 1 次 → approach（count=1）")
    void requestAnswer_firstTimeApproach() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decide(any())).thenReturn(meta("reveal"));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"思路\"}")));

        service.requestAnswer(STUDENT_ID, SESSION_ID).subscribe();

        assertEquals(1, session.getAnswerRequestCount());
        verify(llmPort).generate(argThat(ctx -> "approach".equals(ctx.getActionType())));
    }

    @Test
    @DisplayName("requestAnswer: 第 2 次 → reveal（count=2，放行）")
    void requestAnswer_secondTimeReveal() {
        TutoringSession session = activeSessionInCache();
        session.requestAnswer(); // count=1
        when(llmPort.decide(any())).thenReturn(meta("reveal"));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"答案：鸡15兔20\"}")));

        service.requestAnswer(STUDENT_ID, SESSION_ID).subscribe();

        assertEquals(2, session.getAnswerRequestCount());
        verify(llmPort).generate(argThat(ctx -> "reveal".equals(ctx.getActionType())));
    }

    @Test
    @DisplayName("B2: 第 2 次 reveal 放行后收尾 ANSWER_REVEALED（会话 ARCHIVED + 清 Redis）")
    void requestAnswer_secondReveal_archivesSession() {
        TutoringSession session = activeSessionInCache();
        session.requestAnswer(); // count=1
        when(llmPort.decide(any())).thenReturn(meta("reveal"));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"答案：鸡15兔20\"}")));

        service.requestAnswer(STUDENT_ID, SESSION_ID).subscribe();

        assertEquals(TutoringState.ARCHIVED, session.getStatus());
        assertEquals(EndReason.ANSWER_REVEALED, session.getEndReason());
        verify(sessionCache).clear(SESSION_ID); // 收尾后清 Redis
        verify(transcriptArchiver, atLeastOnce()).archive(any(), eq(SESSION_ID), any(), anyList(), eq(TutoringState.ARCHIVED), any());
    }

    // ==================== getSession / archive / mastery ====================

    @Test
    @DisplayName("getSession: 断点恢复返回最近消息 + transcriptUrl 签名 URL")
    void getSession_returnsRecentMessages() {
        TutoringSession session = activeSessionInCache();
        session.updateTranscriptUrl("tutoring/transcripts/1001.json");
        when(sessionCache.listMessages(SESSION_ID)).thenReturn(List.of(
                TutoringChatMessage.user("鸡兔同笼"), TutoringChatMessage.ai("先找已知条件")));

        TutoringSessionDTO dto = service.getSession(STUDENT_ID, SESSION_ID);

        assertEquals(SESSION_ID, dto.getSessionId());
        assertEquals(2, dto.getRecentMessages().size());
        assertEquals("https://cos/tutoring/transcripts/1001.json", dto.getTranscriptUrl());
    }

    @Test
    @DisplayName("archive: 主动收尾 → ARCHIVED + ABANDONED + COS 终态写 + 清 Redis")
    void archive_endsSession() {
        TutoringSession session = activeSessionInCache();
        when(sessionCache.listMessages(SESSION_ID)).thenReturn(List.of(TutoringChatMessage.user("先不做了")));

        TutoringSessionDTO dto = service.archive(STUDENT_ID, SESSION_ID);

        assertEquals(TutoringState.ARCHIVED.name(), dto.getStatus());
        assertEquals("ABANDONED", session.getEndReason().name());
        verify(transcriptArchiver).archive(any(), eq(SESSION_ID), any(), anyList(), eq(TutoringState.ARCHIVED), any());
        verify(sessionCache).clear(SESSION_ID);
        verify(masteryRepository, never()).upsert(any()); // ABANDONED 不提升掌握度
    }

    @Test
    @DisplayName("getStudentMastery: 映射掌握度列表")
    void getStudentMastery_mapsItems() {
        StudentKpMastery mastery = StudentKpMastery.create(STUDENT_ID, KpKey.of(KP_URI), "二元一次方程组");
        mastery.applySignal(com.ai.edu.domain.learning.model.valueobject.MasterySignal.of(
                "二元一次方程组", com.ai.edu.domain.learning.model.valueobject.MasterySignal.Level.MASTERED));
        when(masteryRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(mastery));

        StudentMasteryDTO dto = service.getStudentMastery(STUDENT_ID);

        assertEquals(STUDENT_ID, dto.getStudentId());
        assertEquals(1, dto.getItems().size());
        assertEquals(KP_URI, dto.getItems().get(0).getKpKey());
        assertEquals(75, dto.getItems().get(0).getMasteryLevel());
    }

    // ==================== ocr() ====================

    @Test
    @DisplayName("ocr: 空图片 → 50006 无效图片")
    void ocr_emptyImageInvalid() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.ocr(new byte[0], "q.png"));
        assertEquals("50006", ex.getCode());
        verify(llmPort, never()).recognize(any(), anyString());
    }

    @Test
    @DisplayName("ocr: 非 jpg/png 扩展名 → 50006")
    void ocr_badExtensionInvalid() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.ocr(new byte[]{1, 2, 3}, "q.gif"));
        assertEquals("50006", ex.getCode());
        verify(llmPort, never()).recognize(any(), anyString());
    }

    @Test
    @DisplayName("ocr: 成功 → 返回识别结果")
    void ocr_success() {
        when(llmPort.recognize(any(), eq("q.png")))
                .thenReturn(OcrResult.builder().text("鸡兔同笼").confidence(0.92).build());

        OcrResult result = service.ocr(new byte[]{1, 2, 3}, "q.png");

        assertEquals("鸡兔同笼", result.getText());
        verify(llmPort).recognize(any(), eq("q.png"));
    }

    @Test
    @DisplayName("ocr: Python 识别失败（重试后）→ 50005 agent 失败")
    void ocr_agentFailure() {
        when(llmPort.recognize(any(), anyString()))
                .thenThrow(new TutoringAgentException("识别失败"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.ocr(new byte[]{1, 2, 3}, "q.png"));
        assertEquals("50005", ex.getCode());
    }

    // ==================== 11.1 配置 / 11.2 并发锁 ====================

    @Test
    @DisplayName("sendMessage: 会话锁被占 → 抛 10000 会话繁忙，不追加消息")
    void sendMessage_busySessionLock() {
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sendMessage(STUDENT_ID, SESSION_ID, "继续"));
        assertEquals("10000", ex.getCode());
        verify(sessionCache, never()).appendMessage(anyLong(), any());
        verify(sessionCache, never()).listMessages(anyLong());
    }

    @Test
    @DisplayName("sendMessage: 会话锁正常获取 → 释放锁")
    void sendMessage_releasesLock() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decide(any())).thenReturn(meta("hint"));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"ok\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "继续").subscribe();

        verify(redisService).tryLock(anyString(), anyString(), anyLong(), any());
        verify(redisService).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("ocr: ocr.enabled=false → 50006 拍照未开启")
    void ocr_disabledByConfig() {
        TutoringConfig disabled = mock(TutoringConfig.class);
        when(disabled.ocrEnabled()).thenReturn(false);
        service.setTutoringConfig(disabled);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.ocr(new byte[]{1}, "q.png"));
        assertEquals("50006", ex.getCode());
        verify(llmPort, never()).recognize(any(), anyString());
    }

    @Test
    @DisplayName("getTutoringConfig: 返回 ocrEnabled 开关")
    void getTutoringConfig_returnsOcrEnabled() {
        TutoringConfigDTO dto = service.getTutoringConfig();

        assertEquals(true, dto.getOcrEnabled());
    }

    // ==================== helpers ====================

    private TutoringSession activeSessionInCache() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        when(sessionCache.findSession(SESSION_ID)).thenReturn(Optional.of(session));
        when(sessionCache.listMessages(SESSION_ID)).thenReturn(List.of(
                TutoringChatMessage.user("鸡兔同笼"), TutoringChatMessage.ai("先找已知条件")));
        return session;
    }

    private ActionMeta meta(String type) {
        return ActionMeta.builder().type(type).build();
    }

    private ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.<String>builder().event(event).data(data).build();
    }
}
