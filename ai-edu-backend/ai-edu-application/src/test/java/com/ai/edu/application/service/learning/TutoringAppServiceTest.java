package com.ai.edu.application.service.learning;

import com.ai.edu.application.assembler.learning.TutoringAssembler;
import com.ai.edu.application.dto.learning.GuardResult;
import com.ai.edu.application.dto.learning.MasteryQueryRequest;
import com.ai.edu.application.dto.learning.StudentMasteryDTO;
import com.ai.edu.application.dto.learning.TutoringConfigDTO;
import com.ai.edu.application.dto.learning.TutoringSessionDTO;
import com.ai.edu.application.dto.learning.TutoringSessionListItemDTO;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.ActionMeta;
import com.ai.edu.domain.learning.model.contract.DecideContext;
import com.ai.edu.domain.learning.model.contract.EvalInfo;
import com.ai.edu.domain.learning.model.contract.MasterySignalItem;
import com.ai.edu.domain.learning.model.contract.OcrResult;
import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.entity.ErrorEvent;
import com.ai.edu.domain.learning.model.entity.StudentTopicMastery;
import com.ai.edu.domain.learning.model.entity.QuestionAttempt;
import com.ai.edu.domain.learning.model.entity.StudentQuestionRecord;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.EndReason;
import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import com.ai.edu.domain.learning.repository.ErrorEventRepository;
import com.ai.edu.domain.learning.repository.StudentQuestionRecordRepository;
import com.ai.edu.domain.learning.repository.StudentTopicMasteryRepository;
import com.ai.edu.domain.learning.repository.TutoringSessionCache;
import com.ai.edu.domain.learning.repository.TutoringSessionRepository;
import com.ai.edu.domain.learning.service.TutoringConfig;
import com.ai.edu.domain.learning.service.TutoringKpResolver;
import com.ai.edu.domain.learning.service.TutoringLlmPort;
import com.ai.edu.domain.shared.service.FileStorageService;
import com.ai.edu.domain.shared.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
    private StudentTopicMasteryRepository studentTopicMasteryRepository;
    private StudentQuestionRecordRepository questionRecordRepository;
    private TopicLabelAggregationService topicLabelAggregationService;
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
        studentTopicMasteryRepository = mock(StudentTopicMasteryRepository.class);
        errorEventRepository = mock(ErrorEventRepository.class);
        sessionCache = mock(TutoringSessionCache.class);
        llmPort = mock(TutoringLlmPort.class);
        kpResolver = mock(TutoringKpResolver.class);
        transcriptArchiver = mock(TutoringTranscriptArchiver.class);
        fileStorageService = mock(FileStorageService.class);
        redisService = mock(RedisService.class);

        service.setTutoringSessionRepository(sessionRepository);
        service.setStudentTopicMasteryRepository(studentTopicMasteryRepository);
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
        // 3.4 题目落库 + 聚集（结算时 persistQuestionAttempt 消费）
        questionRecordRepository = mock(StudentQuestionRecordRepository.class);
        when(questionRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        topicLabelAggregationService = mock(TopicLabelAggregationService.class);
        when(topicLabelAggregationService.aggregate(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
        service.setQuestionRecordRepository(questionRecordRepository);
        service.setTopicLabelAggregationService(topicLabelAggregationService);
        // B2 归档异步化：测试注入 immediate()，归档同步执行 → 现有 verify(transcriptArchiver.archive(...)) 不竞态
        service.setArchiveScheduler(Schedulers.immediate());

        // 会话并发锁默认放行
        when(redisService.tryLock(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(sessionCache.tryIncrementCreateCount(eq(STUDENT_ID), anyInt(), anyInt())).thenReturn(true);
        when(fileStorageService.generatePresignedUrl(anyString(), anyInt()))
                .thenAnswer(inv -> "https://cos/" + inv.getArgument(0));
        when(kpResolver.resolveReadOnly(anyString(), any())).thenReturn(KpResolution.resolved("label", KP_URI, "二元一次方程组", 100));
        when(studentTopicMasteryRepository.findByStudentId(eq(STUDENT_ID))).thenReturn(List.of());
        when(studentTopicMasteryRepository.findByStudentAndTopic(eq(STUDENT_ID), any())).thenReturn(Optional.empty());
        when(studentTopicMasteryRepository.upsert(any())).thenAnswer(inv -> inv.getArgument(0));
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
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先找已知条件\"}")));

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, "鸡兔同笼，共35头94脚，各几只？");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(guardrail) 前置
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(memory) 流尾
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        verify(sessionRepository, atLeastOnce()).save(any()); // 建会话 + 更新计数
        verify(transcriptArchiver, atLeastOnce()).archive(any(), eq(SESSION_ID), any(), anyList(), any(), any());
        verify(sessionCache, never()).clear(eq(SESSION_ID)); // ACTIVE 不清理
    }

    @Test
    @DisplayName("[B2] 归档异步提交：schedule 被调用、archive 未内联执行（SSE 不等待 COS 上传）")
    void start_archivesAsync_nonBlocking() {
        // mock 调度器只记录不执行 → transcriptArchiver.archive 绝不被同步调用
        Scheduler archiveScheduler = mock(Scheduler.class);
        service.setArchiveScheduler(archiveScheduler);
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先找已知条件\"}")));

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, "鸡兔同笼，共35头94脚，各几只？");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(guardrail) 前置
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(memory) 流尾
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();

        // 归档提交到调度器（postDecide + doOnComplete 各一次），证明非阻塞，SSE 不等待 COS
        verify(archiveScheduler, atLeast(2)).schedule(any(Runnable.class));
        verify(transcriptArchiver, never()).archive(any(), any(), any(), anyList(), any(), any());
    }

    @Test
    @DisplayName("[BUG-A] start 图片: 首条图片消息须入 Redis 缓存（ensurePersisted 因 id 非空跳过，靠 start 显式补录）")
    void start_imageFirstMessagePersistedToCache() {
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先看图\"}")));
        when(fileStorageService.uploadToObjectKey(anyString(), any(byte[].class), anyString())).thenReturn(null);
        when(fileStorageService.getUrl(anyString()))
                .thenReturn("https://cos/tutoring/questions/501/1001/20260807-000000-000.png");

        Flux<ServerSentEvent<String>> stream = service.start(STUDENT_ID, null,
                new byte[]{1, 2, 3}, "question.png");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(guardrail)
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(memory)
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        // 关键断言：图片首条消息带 image_url 入 Redis 缓存（否则后续 transcript 整写 / decide 上下文丢首条题目图）
        verify(sessionCache).appendMessage(eq(SESSION_ID), argThat(m ->
                "user".equals(m.getRole()) && m.getImageUrl() != null
                        && m.getImageUrl().startsWith("https://cos/tutoring/questions/501/1001/")));
        verify(sessionRepository, atLeastOnce()).save(any()); // 图片路径提前落库拿 sessionId
    }

    @Test
    @DisplayName("start: 无关内容（type=end, end_reason 空）→ TERMINATED 直接回复，不建会话、无 generate")
    void start_unrelatedTerminated() {
        ActionMeta end = meta("end");
        end.setSummary("我主要解答学科问题，请提出学习相关的内容");
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(end));

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
    @DisplayName("start: decide 流失败 → 不建会话，订阅期抛 TutoringAgentException")
    void start_decideFailure_noSession() {
        when(llmPort.decideStream(any())).thenReturn(Flux.error(new TutoringAgentException("decide 失败")));

        StepVerifier.create(service.start(STUDENT_ID, "鸡兔同笼"))
                .expectError(TutoringAgentException.class)
                .verify();
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("P0: decide 抛非 agent 异常（已有会话）→ 兜底终态流（meta+token+done），不 Flux.error 断连")
    void sendMessage_decideNonAgentFailure_terminal() {
        activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(Flux.error(new RuntimeException("boom")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "2x+4(35-x)=94");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
    }

    // ==================== sendMessage() ====================

    @Test
    @DisplayName("sendMessage: 正常一轮 hint → meta/token/done，round 递增")
    void sendMessage_normalRound() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"注意等式两边\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "2x+4(35-x)=94");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(guardrail) 前置
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(memory) 流尾
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        assertEquals(1, session.getRoundCount()); // round 0→1
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("P0: 落库副作用 DB 异常（非 agent）→ 降级继续，SSE 仍以 done 终态结束（防前端永久卡 SENDING）")
    void sendMessage_sideEffectDbFailure_stillTerminal() {
        TutoringSession session = activeSessionInCache();
        ActionMeta hintWithError = ActionMeta.builder()
                .type("hint")
                .eval(EvalInfo.builder().correct(false).errorType("COMPUTATION").build())
                .build();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(hintWithError));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"注意\"}")));
        when(errorEventRepository.save(any())).thenThrow(new RuntimeException("DB insert failed: 数据过长"));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "2x+4(35-x)=94");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(guardrail)
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(memory) 流尾
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
    }

    @Test
    @DisplayName("3.1 题聚合：decide 轮累计作答信号到当前题（rounds + 题型名），hint 轮 hinted=true")
    void sendMessage_accumulatesQuestionAttempt() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"注意等式两边\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "2x+4(35-x)=94").blockLast();

        QuestionAttempt attempt = session.getCurrentAttempt();
        assertEquals(1, attempt.getRoundCount());
        assertEquals(1, attempt.getRounds().size());
        assertFalse(attempt.getRounds().get(0).isCorrect(), "meta 无 eval → correct 默认 false");
        assertFalse(attempt.getRounds().get(0).isHinted(), "hint 轮但学生未求助（answerRequestCount=0）→ hinted=false（拍板：只看 answerRequestCount）");
    }

    @Test
    @DisplayName("3.2 题目文本: 首条 user 消息捕获为当前题文本（换题后首条，非最后一条用户消息）")
    void sendMessage_capturesQuestionContent() {
        TutoringSession session = activeSessionInCache();
        // sendMessage 追加新消息后 history 最后一条 user = 传入题目文本（mock listMessages 含新消息）
        when(sessionCache.listMessages(SESSION_ID)).thenReturn(List.of(
                TutoringChatMessage.user("鸡兔同笼"),
                TutoringChatMessage.ai("先找已知条件"),
                TutoringChatMessage.user("笼子里有鸡和兔共 35 个头 94 只脚")));
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"注意\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "笼子里有鸡和兔共 35 个头 94 只脚").blockLast();

        assertEquals("笼子里有鸡和兔共 35 个头 94 只脚", session.getCurrentAttempt().getContent());
        assertFalse(session.isContentCapturePending(), "捕获后 pending 复位");
    }

    @Test
    @DisplayName("3.1 题聚合：SWITCH 换题轮结算当前题（一道题一条聚合），新题从零累计")
    void sendMessage_switchSettlesAttempt() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"注意\"}")));
        service.sendMessage(STUDENT_ID, SESSION_ID, "题目一").blockLast();
        assertEquals(1, session.getCurrentAttempt().getRoundCount());

        // 换题轮 SWITCH：结算上一题，新题从零累计
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("switch")));
        service.sendMessage(STUDENT_ID, SESSION_ID, "换一道题").blockLast();

        assertEquals(0, session.getCurrentAttempt().getRoundCount(), "SWITCH 结算后新题从零累计");
        assertEquals(0, session.getRoundCount(), "switchQuestion 重置轮次计数");
    }

    @Test
    @DisplayName("sendMessage: 答案护栏拒绝 reveal（未授权）→ meta(approach, denied=reveal) + approach 生成流，count→1")
    void sendMessage_answerDeniedToApproach() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("reveal")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"思路：先设未知数\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "直接告诉我答案");

        StepVerifier.create(stream)
                .assertNext(ev -> {
                    // guardrail 前置，detail 含拒绝降级摘要
                    assertEquals("agent", ev.event());
                    assertTrue(ev.data().contains("\"stage\":\"guardrail\""), ev.data());
                    assertTrue(ev.data().contains("拒绝: reveal → 降级 approach"), ev.data());
                })
                .assertNext(ev -> {
                    assertEquals("meta", ev.event());
                    assertTrue(ev.data().contains("\"type\":\"approach\""), ev.data());
                    assertTrue(ev.data().contains("\"denied\":\"reveal\""), ev.data());
                })
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(memory)
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        assertEquals(1, session.getAnswerRequestCount());
    }

    @Test
    @DisplayName("sendMessage: AI 消息 append 携带工作流 meta（type/round/decide_reason/question_kps/eval/status，镜像 buildMeta）")
    void sendMessage_aiMessageCarriesMeta() {
        TutoringSession session = activeSessionInCache();
        ActionMeta hint = meta("hint");
        hint.setReason("学生已列方程，先给思路");
        hint.setQuestionKps(List.of("一次方程", "鸡兔同笼模型"));
        hint.setEval(EvalInfo.builder().correct(true).errorType("calc").emotion("neutral").build());
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(hint));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"注意等式两边同时减去4x\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "2x+4(35-x)=94");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        // AI 消息 meta = buildMeta 同源镜像（护栏通过 → denied 为空）
        verify(sessionCache).appendMessage(eq(SESSION_ID), argThat(m ->
                "ai".equals(m.getRole())
                        && "hint".equals(m.getType())
                        && m.getDenied() == null
                        && "学生已列方程，先给思路".equals(m.getDecideReason())
                        && Integer.valueOf(1).equals(m.getRound())            // applySideEffects 已递增 round 0→1
                        && List.of("一次方程", "鸡兔同笼模型").equals(m.getQuestionKps())
                        && m.getEval() != null && Boolean.TRUE.equals(m.getEval().getCorrect())
                        && "calc".equals(m.getEval().getErrorType())
                        && "ACTIVE".equals(m.getStatus())
                        && "注意等式两边同时减去4x".equals(m.getContent())));
    }

    @Test
    @DisplayName("sendMessage: 护栏拒绝 reveal → AI 消息 meta.type=approach + denied=reveal（历史复原与 live 一致）")
    void sendMessage_deniedAiMessageCarriesMeta() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("reveal")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"思路：先设未知数\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "直接告诉我答案");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        verify(sessionCache).appendMessage(eq(SESSION_ID), argThat(m ->
                "ai".equals(m.getRole())
                        && "approach".equals(m.getType())
                        && "reveal".equals(m.getDenied())));
    }

    @Test
    @DisplayName("sendMessage: 学生换题 decide=switch → 计数归零（round/answer），新题继续")
    void sendMessage_switchResetsCounters() {
        TutoringSession session = activeSessionInCache();
        session.recordRound();
        session.recordRound(); // round=2
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("switch")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"新题我们开始\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "换一题");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(guardrail)
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .expectNextCount(3)  // token + agent(memory) + done
                .verifyComplete();
        assertEquals(0, session.getRoundCount());
        assertEquals(TutoringState.ACTIVE, session.getStatus());
    }

    @Test
    @DisplayName("sendMessage: 上传新题目图片（新 URL）→ 换题信号 is_new_question=true + 消息带 image_url")
    void sendMessage_newImage_setsIsNewQuestion() {
        activeSessionInCache();
        when(fileStorageService.uploadToObjectKey(anyString(), any(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.getUrl(anyString())).thenAnswer(inv -> "https://cos/" + inv.getArgument(0));
        AtomicReference<DecideContext> captured = new AtomicReference<>();
        when(llmPort.decideStream(any())).thenAnswer(inv -> {
            captured.set(inv.getArgument(0));
            return decideStreamOf(meta("hint"));
        });
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"新题开始\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, null, new byte[]{1, 2, 3}, "math.png").subscribe();

        assertNotNull(captured.get());
        assertTrue(captured.get().isNewQuestion(), "新图上传轮 is_new_question 应为 true");
        // 时间戳 objectKey：按学生/会话组织
        verify(fileStorageService).uploadToObjectKey(
                argThat(k -> k.startsWith("tutoring/questions/501/1001/") && k.endsWith(".png")),
                any(), eq("image/png"));
        verify(sessionCache).appendMessage(eq(SESSION_ID), argThat(m ->
                "user".equals(m.getRole())
                        && m.getImageUrl() != null
                        && m.getImageUrl().startsWith("https://cos/tutoring/questions/501/1001/")));
    }

    @Test
    @DisplayName("sendMessage: 文字消息 → is_new_question=false（不触发换题，不传图）")
    void sendMessage_text_isNewQuestionFalse() {
        activeSessionInCache();
        AtomicReference<DecideContext> captured = new AtomicReference<>();
        when(llmPort.decideStream(any())).thenAnswer(inv -> {
            captured.set(inv.getArgument(0));
            return decideStreamOf(meta("hint"));
        });
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"继续\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "2x+4(35-x)=94").subscribe();

        assertNotNull(captured.get());
        assertFalse(captured.get().isNewQuestion(), "文字消息轮 is_new_question 应为 false");
        verify(fileStorageService, never()).uploadToObjectKey(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("start: 首条图片消息 → is_new_question=false，先落库拿 sessionId，图片存会话路径")
    void start_withImage_isNewQuestionFalse() {
        when(fileStorageService.uploadToObjectKey(anyString(), any(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.getUrl(anyString())).thenAnswer(inv -> "https://cos/" + inv.getArgument(0));
        AtomicReference<DecideContext> captured = new AtomicReference<>();
        when(llmPort.decideStream(any())).thenAnswer(inv -> {
            captured.set(inv.getArgument(0));
            return decideStreamOf(meta("hint"));
        });
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先找已知条件\"}")));

        service.start(STUDENT_ID, null, new byte[]{1, 2, 3}, "math.png").subscribe();

        assertNotNull(captured.get());
        assertFalse(captured.get().isNewQuestion(), "首条消息绝不判换题");
        // 图片发起：先落库拿 sessionId（1001）→ 图片按会话路径存，不再用 pending
        verify(sessionRepository, atLeastOnce()).save(any());
        verify(fileStorageService).uploadToObjectKey(
                argThat(k -> k.startsWith("tutoring/questions/501/1001/") && k.endsWith(".png")),
                any(), eq("image/png"));
    }

    @Test
    @DisplayName("sendMessage: eval.correct=false → 写错误事件（含情绪/学生原答）")
    void sendMessage_writesErrorEvent() {
        TutoringSession session = activeSessionInCache();
        ActionMeta action = meta("hint");
        action.setEval(EvalInfo.builder().correct(false).errorType("COMPUTATION")
                .emotion("CONFUSED").exerciseComplete(false).build());

        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(action));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"再想想\"}")));
        when(sessionCache.listMessages(SESSION_ID)).thenReturn(List.of(
                TutoringChatMessage.user("设鸡x只"), TutoringChatMessage.ai("继续"), TutoringChatMessage.user("2x+4(35-x)=94")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "2x+4(35-x)=94").blockLast();

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
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(action));
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
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(action));
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
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(action));
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
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(action));
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
        verify(llmPort, never()).decideStream(any());
    }

    @Test
    @DisplayName("sendMessage: mastery_signals 为空 → 跳过掌握度更新，不报错")
    void sendMessage_emptyMasterySignals() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("concept")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"请把题目发完整\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "这题不会").subscribe();

        verify(errorEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendMessage: decide 输出非法 type → 默认 hint 放行，不阻断")
    void sendMessage_invalidType_defaultsHint() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("garbage")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"继续引导\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "这一步怎么算");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(guardrail) 前置
                .assertNext(ev -> assertTrue(ev.data().contains("\"type\":\"hint\""), ev.data()))  // meta 默认 hint
                .expectNextCount(3)  // token + agent(memory) + done
                .verifyComplete();
    }

    @Test
    @DisplayName("AI 回复落库：流结束后 AI 回复（拼接 token）追加到 Redis 消息列表")
    void sendMessage_appendsAiReply() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(
                sse("token", "{\"content\":\"先找\"}"),
                sse("token", "{\"content\":\"已知条件\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "设鸡x只").blockLast();

        verify(sessionCache).appendMessage(eq(SESSION_ID), argThat(m ->
                "ai".equals(m.getRole()) && "先找已知条件".equals(m.getContent())));
        // 流结束后重新整写 COS（含 AI 回复的完整对话）
        verify(transcriptArchiver, atLeastOnce()).archive(any(), eq(SESSION_ID), any(), anyList(), any(), any());
    }

    @Test
    @DisplayName("sendMessage: 只透传 Python 的 token 事件（meta/done 不泄漏为假 token）")
    void sendMessage_filtersNonTokenPythonEvents() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(
                sse("meta", "{\"action_type\":\"hint\"}"),
                sse("token", "{\"content\":\"先找已知条件\"}"),
                sse("done", "{\"model_used\":\"provider/model\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "鸡兔同笼");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(guardrail) 前置
                .assertNext(ev -> assertEquals("meta", ev.event()))     // Java 自建 meta
                .assertNext(ev -> {
                    assertEquals("token", ev.event());
                    assertTrue(ev.data().contains("\"content\""), ev.data());
                    assertFalse(ev.data().contains("action_type"), ev.data());
                    assertFalse(ev.data().contains("model_used"), ev.data());
                })
                .assertNext(ev -> assertEquals("agent", ev.event()))    // agent(memory) 流尾
                .assertNext(ev -> assertEquals("done", ev.event()))     // Java 自建 done
                .verifyComplete();
    }

    @Test
    @DisplayName("sendMessage: 中继 Python generate 的 agent 事件（原样透传，不当作 token）")
    void sendMessage_relaysGenerateAgentEvents() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(
                sse("agent", "{\"level\":\"sub\",\"stage\":\"generate\",\"label\":\"生成中\",\"status\":\"processing\"}"),
                sse("token", "{\"content\":\"先找已知条件\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "鸡兔同笼");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // Java guardrail 前置
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> {
                    assertEquals("agent", ev.event());                  // Python generate 事件中继
                    assertTrue(ev.data().contains("\"stage\":\"generate\""), ev.data());
                })
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // Java memory 流尾
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
    }

    @Test
    @DisplayName("sendMessage: 中继 Python generate 的 thinking 事件（推理分片原样透传，不当作 token 累积）")
    void sendMessage_relaysGenerateThinkingEvents() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(
                sse("agent", "{\"level\":\"sub\",\"stage\":\"generate\",\"label\":\"生成中\",\"status\":\"processing\"}"),
                sse("thinking", "{\"content\":\"先考虑头数\"}"),
                sse("thinking", "{\"content\":\"再算脚数差值\"}"),
                sse("token", "{\"content\":\"先找已知条件\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "鸡兔同笼");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // Java guardrail 前置
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // Python generate 事件
                .assertNext(ev -> {
                    assertEquals("thinking", ev.event());               // thinking 推理分片原样中继
                    assertTrue(ev.data().contains("\"content\""), ev.data());
                })
                .assertNext(ev -> {
                    assertEquals("thinking", ev.event());               // 第二条 thinking
                    assertTrue(ev.data().contains("\"再算脚数差值\""), ev.data());
                })
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // Java memory 流尾
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
        // thinking 不算 AI 正文：AI 回复落库 content 只含 token 拼接，thinking 单独落库为推理分片拼接
        verify(sessionCache).appendMessage(eq(SESSION_ID), argThat(m ->
                "ai".equals(m.getRole()) && "先找已知条件".equals(m.getContent())
                        && "先考虑头数再算脚数差值".equals(m.getThinking())));
    }

    @Test
    @DisplayName("sendMessage: memory 事件 detail 含本轮掌握度信号")
    void sendMessage_memoryEventDetail() {
        TutoringSession session = activeSessionInCache();
        ActionMeta action = meta("hint");
        action.setMasterySignals(List.of(
                MasterySignalItem.builder().kpLabel("二元一次方程组").signal("practicing").build()));
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(action));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"很好\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "我解出来了");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> {
                    assertEquals("agent", ev.event());
                    assertTrue(ev.data().contains("\"stage\":\"memory\""), ev.data());
                    assertTrue(ev.data().contains("二元一次方程组 → practicing"), ev.data());
                })
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
    }

    @Test
    @DisplayName("3.4 掌握信号：作答轮累计 → END 结算 → 题目落库 + 掌握表 applyScore 累计平均")
    void sendMessage_persistsQuestionAttemptOnEnd() {
        TutoringSession session = activeSessionInCache();
        // 作答轮：hint + eval.correct=true，学生未求助（answerRequestCount=0）→ 直接答对 hinted=false
        ActionMeta hint = meta("hint");
        hint.setMasterySignals(List.of(
                MasterySignalItem.builder().kpLabel("二元一次方程组").signal("practicing").build()));
        hint.setEval(EvalInfo.builder().correct(true).build());
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(hint));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"很好\"}")));
        service.sendMessage(STUDENT_ID, SESSION_ID, "我解出来了").blockLast();
        assertEquals(1, session.getCurrentAttempt().getRounds().size(), "作答轮累计信号");

        // 收尾轮 END：结算 → 题目落库 + 掌握表累计平均（首题直接答对 1.0×0.7 = 0.70 → 70%）
        ActionMeta end = meta("end");
        end.setEndReason("COMPLETED");
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(end));
        service.sendMessage(STUDENT_ID, SESSION_ID, "谢谢老师").blockLast();

        ArgumentCaptor<StudentQuestionRecord> recCaptor = ArgumentCaptor.forClass(StudentQuestionRecord.class);
        verify(questionRecordRepository).save(recCaptor.capture());
        StudentQuestionRecord rec = recCaptor.getValue();
        assertEquals("ai", rec.getSource());
        assertEquals("二元一次方程组", rec.getTopicLabel());
        assertEquals("二元一次方程组", rec.getCanonicalLabel());
        assertEquals(0, new BigDecimal("0.70").compareTo(rec.getScore()));
        assertEquals(0, rec.getHintCount());
        verify(studentTopicMasteryRepository).upsert(argThat(m ->
                m.getTopicKey().getValue().equals("二元一次方程组")
                        && m.getTrainCount() == 1
                        && m.getMasteryLevel().getValue() == 70));
    }

    @Test
    @DisplayName("3.4 SIG-006: PENDING（题型未识别）→ 题目照常落库 canonical=null，信号不丢（等归属后聚合）")
    void sendMessage_pendingTopic_stillPersistsQuestion() {
        TutoringSession session = activeSessionInCache();
        // 作答轮：hint 无 masterySignals（题型未识别）→ topicLabel null → canonical null（PENDING）
        ActionMeta hint = meta("hint");
        hint.setEval(EvalInfo.builder().correct(true).build());
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(hint));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"很好\"}")));
        service.sendMessage(STUDENT_ID, SESSION_ID, "笼子里有鸡和兔").blockLast();

        ActionMeta end = meta("end");
        end.setEndReason("COMPLETED");
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(end));
        service.sendMessage(STUDENT_ID, SESSION_ID, "谢谢老师").blockLast();

        verify(questionRecordRepository).save(argThat(r -> r instanceof StudentQuestionRecord rec
                && rec.getCanonicalLabel() == null
                && rec.getTopicLabel() == null));  // PENDING 不锚定，题目照常落（信号不丢，V20 topic_label 可空）
        verify(studentTopicMasteryRepository, never()).upsert(any());  // 等归属后 2.6 批量聚集聚合
    }

    @Test
    @DisplayName("修复: END 轮有真实 eval（学生答对 exerciseComplete 收尾）→ 累计信号，结算 score 正确（非 0）")
    void sendMessage_endWithCorrectEval_persistsScore() {
        TutoringSession session = activeSessionInCache();
        // 直接答对收尾：END 轮 eval.correct=true 是真实作答（回归：曾被 settlingRound 跳过 → score=0 bug）
        ActionMeta end = meta("end");
        end.setEndReason("COMPLETED");
        end.setEval(EvalInfo.builder().correct(true).build());
        end.setMasterySignals(List.of(
                MasterySignalItem.builder().kpLabel("二元一次方程组").signal("mastered").build()));
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(end));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"完成\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "答案等于 2").blockLast();

        verify(questionRecordRepository).save(argThat(r -> r instanceof StudentQuestionRecord rec
                && rec.getScore() != null
                && new BigDecimal("0.70").compareTo(rec.getScore()) == 0));  // 首题直接答对 1.0×0.7
        verify(studentTopicMasteryRepository).upsert(argThat(m ->
                m.getTrainCount() == 1
                        && m.getMasteryLevel().getValue() == 70));
    }

    // ==================== requestAnswer() ====================

    @Test
    @DisplayName("requestAnswer: 第 1 次 → approach（count=1）")
    void requestAnswer_firstTimeApproach() {
        TutoringSession session = activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("reveal")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"思路\"}")));

        service.requestAnswer(STUDENT_ID, SESSION_ID).blockLast();

        assertEquals(1, session.getAnswerRequestCount());
        verify(llmPort).generate(argThat(ctx -> "approach".equals(ctx.getActionType())));
    }

    @Test
    @DisplayName("requestAnswer: 第 2 次 → reveal（count=2，放行）")
    void requestAnswer_secondTimeReveal() {
        TutoringSession session = activeSessionInCache();
        session.requestAnswer(); // count=1
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("reveal")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"答案：鸡15兔20\"}")));

        service.requestAnswer(STUDENT_ID, SESSION_ID).blockLast();

        assertEquals(2, session.getAnswerRequestCount());
        verify(llmPort).generate(argThat(ctx -> "reveal".equals(ctx.getActionType())));
    }

    @Test
    @DisplayName("B2: 第 2 次 reveal 放行后收尾 ANSWER_REVEALED（会话 ARCHIVED + 清 Redis）")
    void requestAnswer_secondReveal_archivesSession() {
        TutoringSession session = activeSessionInCache();
        session.requestAnswer(); // count=1
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("reveal")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"答案：鸡15兔20\"}")));

        service.requestAnswer(STUDENT_ID, SESSION_ID).blockLast();

        assertEquals(TutoringState.ARCHIVED, session.getStatus());
        assertEquals(EndReason.ANSWER_REVEALED, session.getEndReason());
        verify(sessionCache).clear(SESSION_ID); // 收尾后清 Redis
        verify(transcriptArchiver, atLeastOnce()).archive(any(), eq(SESSION_ID), any(), anyList(), eq(TutoringState.ARCHIVED), any());
    }

    // ==================== getSession / archive / mastery ====================

    @Test
    @DisplayName("getSession: 断点恢复返回最近消息（不再下发 transcriptUrl 签名 URL）")
    void getSession_returnsRecentMessages() throws Exception {
        TutoringSession session = activeSessionInCache();
        session.updateTranscriptUrl("tutoring/transcripts/1001.json");
        when(sessionCache.listMessages(SESSION_ID)).thenReturn(List.of(
                TutoringChatMessage.user("鸡兔同笼"),
                TutoringChatMessage.ai("先找已知条件", "先考虑头数再算脚数差值")));

        TutoringSessionDTO dto = service.getSession(STUDENT_ID, SESSION_ID);

        assertEquals(SESSION_ID, dto.getSessionId());
        assertEquals(2, dto.getRecentMessages().size());
        // 断点恢复：AI 消息带 thinking（推理过程），供前端历史"思考过程"面板
        assertEquals("先考虑头数再算脚数差值", dto.getRecentMessages().get(1).getThinking());
        // [T4] detail 响应不再含 transcriptUrl（签名 URL 不下发浏览器）
        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(dto);
        assertFalse(json.contains("transcriptUrl"), json);
    }

    // ==================== getTranscript（COS 后端代理） ====================

    @Test
    @DisplayName("getTranscript: 归属校验通过 → 读 COS 返回完整消息（含 meta）")
    void getTranscript_returnsMessages() {
        activeSessionInCache();
        List<TutoringChatMessage> expected = List.of(TutoringChatMessage.user("鸡兔同笼"),
                TutoringChatMessage.ai("先找已知条件", "先考虑头数再算脚数差值"));
        when(transcriptArchiver.readMessages(eq(STUDENT_ID), eq(SESSION_ID))).thenReturn(expected);

        List<TutoringChatMessage> messages = service.getTranscript(STUDENT_ID, SESSION_ID);

        assertEquals(2, messages.size());
        assertEquals(expected, messages);
        verify(transcriptArchiver).readMessages(STUDENT_ID, SESSION_ID);
    }

    @Test
    @DisplayName("getTranscript: COS 对象缺失（未归档/异步未完成）→ 空列表，非 50002")
    void getTranscript_objectMissing_returnsEmpty() {
        activeSessionInCache();
        when(transcriptArchiver.readMessages(eq(STUDENT_ID), eq(SESSION_ID))).thenReturn(List.of());

        List<TutoringChatMessage> messages = service.getTranscript(STUDENT_ID, SESSION_ID);

        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    @DisplayName("getTranscript: 越权（他人会话）→ 50002，不读 COS")
    void getTranscript_ownershipRejected() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        when(sessionCache.findSession(SESSION_ID)).thenReturn(Optional.of(session));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getTranscript(STUDENT_ID + 1, SESSION_ID));
        assertEquals("50002", ex.getCode());
        verify(transcriptArchiver, never()).readMessages(any(), any());
    }

    @Test
    @DisplayName("getTranscript: 会话不存在 → 50002")
    void getTranscript_sessionNotFound() {
        when(sessionCache.findSession(SESSION_ID)).thenReturn(Optional.empty());
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getTranscript(STUDENT_ID, SESSION_ID));
        assertEquals("50002", ex.getCode());
        verify(transcriptArchiver, never()).readMessages(any(), any());
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
    }

    @Test
    @DisplayName("queryStudentMastery: 映射题型掌握度列表 + 分页元信息")
    void queryStudentMastery_mapsItems() {
        StudentTopicMastery mastery = StudentTopicMastery.create(STUDENT_ID, TopicKey.of("鸡兔同笼"), "鸡兔同笼");
        mastery.applySignal(MasterySignal.of("鸡兔同笼", MasterySignal.Level.MASTERED));
        when(studentTopicMasteryRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(mastery));

        StudentMasteryDTO dto = service.queryStudentMastery(STUDENT_ID, MasteryQueryRequest.builder().build());

        assertEquals(STUDENT_ID, dto.getStudentId());
        assertEquals(1, dto.getItems().size());
        assertEquals("鸡兔同笼", dto.getItems().get(0).getTopicKey());
        assertEquals(75, dto.getItems().get(0).getMasteryLevel());
        assertEquals(1, dto.getTotal());
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
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"ok\"}")));

        service.sendMessage(STUDENT_ID, SESSION_ID, "继续").blockLast();

        verify(redisService).tryLock(anyString(), anyString(), anyLong(), any());
        verify(redisService).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("decide thinking 实时中继: thinking 先于 guardrail/meta 到达（D7 响应式中继）")
    void sendMessage_decideThinkingRelayedFirst() {
        activeSessionInCache();
        // decide 流: thinking × 2 → agent → meta → done（thinking 在 meta 前到达，须实时中继）
        when(llmPort.decideStream(any())).thenReturn(Flux.just(
                sse("thinking", "{\"content\":\"先识别题型\"}"),
                sse("thinking", "{\"content\":\"再给提示\"}"),
                sse("agent", "{\"stage\":\"decide\"}"),
                sse("meta", serializeMeta(meta("hint"))),
                sse("done", "{}")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"ok\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "鸡兔同笼");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("thinking", ev.event()))   // ① decide thinking 实时中继
                .assertNext(ev -> assertEquals("thinking", ev.event()))   // ②
                .assertNext(ev -> {                                       // ③ decide 阶段 agent 事件中继（D7 后 filter 放行 agent）
                    assertEquals("agent", ev.event());
                    assertTrue(ev.data().contains("\"stage\":\"decide\""), ev.data());
                })
                .assertNext(ev -> assertEquals("agent", ev.event()))      // ④ guardrail 前置（meta 后）
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))      // memory 流尾
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
    }

    @Test
    @DisplayName("sendMessage: 中继 Python decide 阶段全部 agent 事件（perceive/analyze/plan/decide），先于 guardrail/meta")
    void sendMessage_relaysDecideAgentEvents() {
        activeSessionInCache();
        when(llmPort.decideStream(any())).thenReturn(Flux.just(
                sse("agent", "{\"stage\":\"perceive\",\"label\":\"读取题目\",\"status\":\"processing\"}"),
                sse("agent", "{\"stage\":\"analyze\",\"label\":\"解析意图\",\"status\":\"processing\"}"),
                sse("agent", "{\"stage\":\"plan\",\"label\":\"规划引导\",\"status\":\"processing\"}"),
                sse("agent", "{\"stage\":\"decide\",\"label\":\"决策完成\",\"status\":\"done\"}"),
                sse("meta", serializeMeta(meta("hint"))),
                sse("done", "{}")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"ok\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "鸡兔同笼");

        StepVerifier.create(stream)
                .assertNext(ev -> assertTrue(ev.data().contains("\"stage\":\"perceive\""), ev.data()))
                .assertNext(ev -> assertTrue(ev.data().contains("\"stage\":\"analyze\""), ev.data()))
                .assertNext(ev -> assertTrue(ev.data().contains("\"stage\":\"plan\""), ev.data()))
                .assertNext(ev -> assertTrue(ev.data().contains("\"stage\":\"decide\""), ev.data()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // guardrail 前置
                .assertNext(ev -> assertEquals("meta", ev.event()))
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // memory 流尾
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
    }

    @Test
    @DisplayName("sendMessage: meta 携带 decideReason/questionKps/masterySignals（Agent 工作流契约）")
    void sendMessage_metaCarriesWorkflowFields() {
        TutoringSession session = activeSessionInCache();
        ActionMeta action = meta("hint");
        action.setReason("学生第 1 次要求答案,但未达放行条件,降级为思路引导");
        action.setQuestionKps(List.of("二元一次方程组", "鸡兔同笼"));
        action.setMasterySignals(List.of(
                MasterySignalItem.builder().kpLabel("二元一次方程组").signal("practicing").build()));
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(action));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"ok\"}")));

        Flux<ServerSentEvent<String>> stream = service.sendMessage(STUDENT_ID, SESSION_ID, "鸡兔同笼");

        StepVerifier.create(stream)
                .assertNext(ev -> assertEquals("agent", ev.event()))    // guardrail 前置
                .assertNext(ev -> {
                    assertEquals("meta", ev.event());
                    assertTrue(ev.data().contains("\"decideReason\":\"学生第 1 次要求答案"), ev.data());
                    assertTrue(ev.data().contains("\"questionKps\":[\"二元一次方程组\",\"鸡兔同笼\"]"), ev.data());
                    assertTrue(ev.data().contains("\"masterySignals\""), ev.data());
                    assertTrue(ev.data().contains("\"kpLabel\":\"二元一次方程组\""), ev.data());
                    assertTrue(ev.data().contains("\"signal\":\"practicing\""), ev.data());
                })
                .assertNext(ev -> assertEquals("token", ev.event()))
                .assertNext(ev -> assertEquals("agent", ev.event()))    // memory
                .assertNext(ev -> assertEquals("done", ev.event()))
                .verifyComplete();
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

    // ==================== 列表 / 删除 / 标题 ====================

    @Test
    @DisplayName("listSessions：全状态映射列表项（sessionId/title/status/subject/questionType/roundCount/updatedAt/archivedAt）")
    void listSessions_mapsAllStatus() {
        TutoringSession active = TutoringSession.start(STUDENT_ID, "math");
        active.setId(1001L);
        active.setTitle("鸡兔同笼怎么做");
        TutoringSession archived = TutoringSession.start(STUDENT_ID, "math");
        archived.setId(1002L);
        archived.setTitle("二次函数图像");
        archived.complete(EndReason.COMPLETED);
        when(sessionRepository.findListByStudentId(STUDENT_ID)).thenReturn(List.of(active, archived));

        List<TutoringSessionListItemDTO> list = service.listSessions(STUDENT_ID);

        assertEquals(2, list.size());
        TutoringSessionListItemDTO first = list.get(0);
        assertEquals(1001L, first.getSessionId());
        assertEquals("鸡兔同笼怎么做", first.getTitle());
        assertEquals("ACTIVE", first.getStatus());
        assertEquals("math", first.getSubject());
        assertEquals(0, first.getRoundCount());
        assertNotNull(first.getUpdatedAt());
        assertNull(first.getArchivedAt());
        assertEquals("ARCHIVED", list.get(1).getStatus());
        assertNotNull(list.get(1).getArchivedAt());
        verify(sessionRepository).findListByStudentId(STUDENT_ID);
    }

    @Test
    @DisplayName("deleteSession：归属校验通过 → 软删 + 清 Redis 缓存")
    void deleteSession_happyPath() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        when(sessionCache.findSession(SESSION_ID)).thenReturn(Optional.of(session));

        service.deleteSession(STUDENT_ID, SESSION_ID);

        verify(sessionRepository).softDelete(SESSION_ID);
        verify(sessionCache).clear(SESSION_ID);
    }

    @Test
    @DisplayName("deleteSession：越权（他人会话）→ 50002，不软删不改缓存")
    void deleteSession_ownershipRejected() {
        TutoringSession session = TutoringSession.start(STUDENT_ID, "math");
        session.setId(SESSION_ID);
        when(sessionCache.findSession(SESSION_ID)).thenReturn(Optional.of(session));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deleteSession(STUDENT_ID + 1, SESSION_ID));
        assertEquals("50002", ex.getCode());
        verify(sessionRepository, never()).softDelete(any());
        verify(sessionCache, never()).clear(any());
    }

    @Test
    @DisplayName("deleteSession：会话不存在 → 50002")
    void deleteSession_notFound() {
        when(sessionCache.findSession(SESSION_ID)).thenReturn(Optional.empty());
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deleteSession(STUDENT_ID, SESSION_ID));
        assertEquals("50002", ex.getCode());
        verify(sessionRepository, never()).softDelete(any());
    }

    @Test
    @DisplayName("start 文字：首条消息生成 title 截断至 ~30 字，随会话落库")
    void start_generatesTitleFromFirstMessage() {
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先找已知条件\"}")));
        String longMsg = "鸡兔同笼有35个头94只脚问鸡和兔子各有多少只这是一个经典的二元一次方程组问题";

        service.start(STUDENT_ID, longMsg).blockLast();

        ArgumentCaptor<TutoringSession> captor = ArgumentCaptor.forClass(TutoringSession.class);
        verify(sessionRepository, atLeastOnce()).save(captor.capture());
        TutoringSession saved = captor.getAllValues().get(0);
        assertNotNull(saved.getTitle());
        assertEquals(30, saved.getTitle().length());      // 截断到 SESSION_TITLE_MAX_LENGTH=30
        assertTrue(saved.getTitle().startsWith("鸡兔同笼"));
    }

    @Test
    @DisplayName("start 图片：无正文 → title 兜底「图片题目」，不阻断")
    void start_imageTitleFallback() {
        when(llmPort.decideStream(any())).thenReturn(decideStreamOf(meta("hint")));
        when(llmPort.generate(any())).thenReturn(Flux.just(sse("token", "{\"content\":\"先看图\"}")));
        when(fileStorageService.uploadToObjectKey(anyString(), any(byte[].class), anyString())).thenReturn(null);
        when(fileStorageService.getUrl(anyString()))
                .thenReturn("https://cos/tutoring/questions/501/1001/20260807-000000-000.png");

        service.start(STUDENT_ID, null, new byte[]{1, 2, 3}, "q.png").blockLast();

        ArgumentCaptor<TutoringSession> captor = ArgumentCaptor.forClass(TutoringSession.class);
        verify(sessionRepository, atLeastOnce()).save(captor.capture());
        assertEquals("图片题目", captor.getAllValues().get(0).getTitle());
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

    /** 构造 decide 流 mock：meta + done（decide 阶段 agent / thinking 事件由用例按需自拼后前置）。 */
    private Flux<ServerSentEvent<String>> decideStreamOf(ActionMeta meta) {
        return Flux.just(
                sse("meta", serializeMeta(meta)),
                sse("done", "{}"));
    }

    private String serializeMeta(ActionMeta meta) {
        try {
            return new ObjectMapper().writeValueAsString(meta);
        } catch (Exception e) {
            throw new RuntimeException("serialize meta failed", e);
        }
    }
}
