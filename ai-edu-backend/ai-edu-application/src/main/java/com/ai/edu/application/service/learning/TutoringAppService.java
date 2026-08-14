package com.ai.edu.application.service.learning;

import com.ai.edu.application.assembler.learning.TutoringAssembler;
import com.ai.edu.application.dto.learning.ChatMessageDTO;
import com.ai.edu.application.dto.learning.GuardResult;
import com.ai.edu.application.dto.learning.MasteryItemDTO;
import com.ai.edu.application.dto.learning.StudentMasteryDTO;
import com.ai.edu.application.dto.learning.SummaryDTO;
import com.ai.edu.application.dto.learning.TutoringConfigDTO;
import com.ai.edu.application.dto.learning.TutoringSessionDTO;
import com.ai.edu.application.dto.learning.TutoringSessionListItemDTO;
import com.ai.edu.application.dto.learning.sse.SseDoneDTO;
import com.ai.edu.application.dto.learning.sse.SseEvalDTO;
import com.ai.edu.application.dto.learning.sse.SseMasterySignalDTO;
import com.ai.edu.application.dto.learning.sse.SseMetaDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.common.exception.TutoringAgentException;
import com.ai.edu.domain.learning.model.contract.ActionMeta;
import com.ai.edu.domain.learning.model.contract.DecideContext;
import com.ai.edu.domain.learning.model.contract.EvalInfo;
import com.ai.edu.domain.learning.model.contract.GenerateContext;
import com.ai.edu.domain.learning.model.contract.MasterySignalItem;
import com.ai.edu.domain.learning.model.contract.OcrResult;
import com.ai.edu.domain.learning.model.contract.TutoringChatMessage;
import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.entity.ErrorEvent;
import com.ai.edu.domain.learning.model.entity.StudentKpMastery;
import com.ai.edu.domain.learning.model.entity.TutoringSession;
import com.ai.edu.domain.learning.model.valueobject.ActionType;
import com.ai.edu.domain.learning.model.valueobject.EndReason;
import com.ai.edu.domain.learning.model.valueobject.KpKey;
import com.ai.edu.domain.learning.model.valueobject.KpResolution;
import com.ai.edu.domain.learning.model.valueobject.MasterySignal;
import com.ai.edu.domain.learning.model.valueobject.TutoringConstants;
import com.ai.edu.domain.learning.model.valueobject.TutoringEmotion;
import com.ai.edu.domain.learning.model.valueobject.TutoringState;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.ErrorEventRepository;
import com.ai.edu.domain.learning.repository.StudentKpMasteryRepository;
import com.ai.edu.domain.learning.repository.TutoringSessionCache;
import com.ai.edu.domain.learning.repository.TutoringSessionRepository;
import com.ai.edu.domain.learning.service.TutoringConfig;
import com.ai.edu.domain.learning.service.TutoringKpResolver;
import com.ai.edu.domain.learning.service.TutoringLlmPort;
import com.ai.edu.domain.shared.service.FileStorageService;
import com.ai.edu.domain.shared.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 答疑编排服务（Java 网关主导，任务 7.3-7.9）。
 *
 * <p>一次学生消息 = 安全预检 → 组装上下文 → Python decide（非流式）→ Java 护栏校验 →
 * 落库副作用（掌握度/错误/情绪/round）→ 每轮实时整写 COS → Python generate（流式）→ SSE 透传。
 *
 * <p>终止场景（无关/学习方法/非数学/安全）直接回复置 TERMINATED，无 generate；护栏拒绝时
 * 按 fallbackType 降级（reveal 未授权 → approach），无 token 流的场景直接返回固定话术。
 */
@Slf4j
@Service
public class TutoringAppService {

    /** 轮次上限强制收尾的提示 */
    private static final String ROUND_LIMIT_REPLY = "本轮答疑已达 20 轮上限，先消化一下当前内容，有需要可以发起新一轮答疑。";
    /** Python 调用失败重试后仍失败的降级提示（会话保持 ACTIVE 不断开） */
    private static final String AGENT_ERROR_REPLY = "网络波动，请重试。";

    /** agent 事件协议阶段/文案（tutoring-agent-protocol 契约，level 恒 sub） */
    private static final String AGENT_STAGE_GUARDRAIL = "guardrail";
    private static final String AGENT_LABEL_GUARDRAIL = "安全把关";
    private static final String AGENT_STAGE_MEMORY = "memory";
    private static final String AGENT_LABEL_MEMORY = "记忆更新";
    /** 断点恢复返回的最近消息条数上限 */
    private static final int RECENT_MESSAGES_LIMIT = 50;
    /** 会话标题取首条用户消息的前 N 个字符（历史列表展示；见设计 D3） */
    private static final int SESSION_TITLE_MAX_LENGTH = 30;
    /** 图片发起（首条消息无文字）时标题兜底 */
    private static final String SESSION_TITLE_IMAGE_FALLBACK = "图片题目";
    /** 无文字无图片的极端兜底 */
    private static final String SESSION_TITLE_EMPTY_FALLBACK = "答疑会话";

    /** 11.2 会话并发锁：Redis key 前缀 + 锁 TTL（覆盖 decide 重试窗口，超时自动释放） */
    private static final String SESSION_LOCK_PREFIX = "learning:tutoring:lock:";
    private static final long SESSION_LOCK_SECONDS = 45;

    private static final ObjectMapper SSE_MAPPER = new ObjectMapper();

    /** decide meta 事件解析用宽容 ObjectMapper（容忍 Python 调试字段 reason 等未知字段）。 */
    private static final ObjectMapper ACTION_META_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** 字段名不用 {@code sessionRepository}——与 Spring Session 的 RedisSessionRepository bean 名冲突（@Resource 按名注入）。 */
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private TutoringSessionRepository tutoringSessionRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private StudentKpMasteryRepository masteryRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private DerivedKpObsRepository derivedKpObsRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private ErrorEventRepository errorEventRepository;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private TutoringSessionCache sessionCache;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private TutoringLlmPort llmPort;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private TutoringKpResolver kpResolver;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private TutoringGuardrailService guardrail;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private TutoringContextAssembler contextAssembler;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private TutoringTranscriptArchiver transcriptArchiver;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private TutoringAssembler assembler;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private FileStorageService fileStorageService;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private TutoringConfig tutoringConfig;
    @Resource
    @Setter(AccessLevel.PACKAGE)
    private RedisService redisService;

    /** COS transcript 归档调度器：单线程 FIFO（先"部分写"、后"完整写"顺序执行，防止完整写被部分写覆盖）。
     *  默认 daemon 单线程，SSE 响应不等待 COS 上传；测试注入 {@link Schedulers#immediate()} 保持
     *  现有 {@code verify(transcriptArchiver.archive(...))} 同步断言不竞态。 */
    private static final Scheduler DEFAULT_ARCHIVE_SCHEDULER = Schedulers.newSingle("tutoring-archive", true);
    @Setter(AccessLevel.PACKAGE)
    private Scheduler archiveScheduler = DEFAULT_ARCHIVE_SCHEDULER;

    // ==================== 对外入口 ====================

    /** 发起答疑（SSE，类型先行流式）：首条学生消息进历史 → decide → 护栏 → 建会话 → generate（文字题）。 */
    public Flux<ServerSentEvent<String>> start(Long studentId, String message) {
        return start(studentId, message, null, null);
    }

    /**
     * 发起答疑（SSE）：文字或图片题目。图片上传 COS 作为首条消息（图片消息带 image_url 进 history）。
     * <p>首条消息恒非换题（is_new_question=false）——会话开始无旧题可换，Python 绝不能判 switch。
     */
    public Flux<ServerSentEvent<String>> start(Long studentId, String message, byte[] imageData, String originalFilename) {
        // 7.9 会话创建频率限制
        ensureCreateAllowed(studentId);
        TutoringSession session = TutoringSession.start(studentId, "math");
        // 会话标题：首条用户消息内容前 ~30 字（历史列表展示，见设计 D3）；须在首次落库前设置，
        // 图片路径（save 拿 sessionId）与文字路径（ensurePersisted）都随会话一起持久化。
        session.setTitle(buildSessionTitle(message, imageData));
        if (imageData != null && imageData.length > 0) {
            // 图片发起：先落库拿 sessionId，题目图按会话路径组织（tutoring/questions/{studentId}/{sessionId}/）。
            // decide 终止/失败由编排处理（TERMINATED 或保持 ACTIVE 可续），不留 pending 临时目录。
            tutoringSessionRepository.save(session);
            sessionCache.saveSession(session);
        }
        List<TutoringChatMessage> history = new ArrayList<>();
        history.add(buildUserMessage(studentId, session.getId(), message, imageData, originalFilename));
        if (session.getId() != null) {
            // [BUG-A] 图片路径已提前落库拿 sessionId，orchestrate 的 ensurePersisted 因 id 非空会跳过补缓存；
            // 首条消息须显式入 Redis，否则后续 transcript 整写 / decide 上下文（sendMessage 用 Redis 组装 history）
            // 会丢失首条题目图。文字路径 id 为空走 ensurePersisted，不在此分支。
            for (TutoringChatMessage msg : history) {
                sessionCache.appendMessage(session.getId(), msg);
            }
        }
        return orchestrate(session, history, false, () -> {});
    }

    /** 发送学生回答（SSE）：追加消息 → decide → 护栏 → 落库副作用 → COS 整写 → generate 透传（文字）。 */
    public Flux<ServerSentEvent<String>> sendMessage(Long studentId, Long sessionId, String content) {
        return sendMessage(studentId, sessionId, content, null, null);
    }

    /**
     * 发送学生消息（SSE）：文字或图片。
     * <p><b>换题信号</b>：本轮上传了新的题目图片（新 URL 首次出现在 history）→ decide 请求带
     * is_new_question=true（仅"新上传"这一轮；后续答题轮不置）。Python 见 true 直接返回 switch，Java 重置计数。
     * <p>11.2 同一会话并发消息经 Redis 锁串行化（锁保护 decide+副作用临界区，流式在锁外）。
     */
    public Flux<ServerSentEvent<String>> sendMessage(Long studentId, Long sessionId, String content,
                                                     byte[] imageData, String originalFilename) {
        return withSessionLockReactive(sessionId, unlock -> {
            TutoringSession session = loadActiveSession(sessionId, studentId);
            boolean isNewQuestion = false;
            if (imageData != null && imageData.length > 0) {
                List<TutoringChatMessage> history = sessionCache.listMessages(sessionId);
                String url = uploadQuestionImage(session.getStudentId(), sessionId, imageData, originalFilename);
                // 换题判定（Java 侧权威）：新图 URL 未在 history 中出现 = 本轮新增题目图 → 换题信号
                isNewQuestion = !historyContainsImageUrl(history, url);
                sessionCache.appendMessage(sessionId, TutoringChatMessage.userWithImage(content, url));
            } else {
                sessionCache.appendMessage(sessionId, TutoringChatMessage.user(content));
            }
            List<TutoringChatMessage> newHistory = sessionCache.listMessages(sessionId);
            return orchestrate(session, newHistory, isNewQuestion, unlock);
        });
    }

    /** 请求答案（SSE）：合成"请把答案给我"消息，交由 decide + 答案护栏处理（第 1 次思路 / 第 2 次答案）。 */
    public Flux<ServerSentEvent<String>> requestAnswer(Long studentId, Long sessionId) {
        return withSessionLockReactive(sessionId, unlock -> {
            TutoringSession session = loadActiveSession(sessionId, studentId);
            sessionCache.appendMessage(sessionId, TutoringChatMessage.user("请把答案给我"));
            List<TutoringChatMessage> history = sessionCache.listMessages(sessionId);
            return orchestrate(session, history, false, unlock);
        });
    }

    /** 断点恢复：查询会话状态 + 最近消息（Redis 过期则提示学生重述题目，完整对话恒在 COS）。 */
    public TutoringSessionDTO getSession(Long studentId, Long sessionId) {
        TutoringSession session = loadSession(sessionId, studentId);
        List<TutoringChatMessage> messages = sessionCache.listMessages(sessionId);
        List<ChatMessageDTO> recent = messages.stream()
                .skip(Math.max(0, messages.size() - RECENT_MESSAGES_LIMIT))
                .map(m -> ChatMessageDTO.builder().role(m.getRole()).content(m.getContent())
                        .imageUrl(m.getImageUrl()).thinking(m.getThinking()).createdAt(m.getCreatedAt()).build())
                .toList();
        TutoringSessionDTO dto = assembler.toSessionDTO(session, recent, null);
        return dto;
    }

    /**
     * 获取会话完整 transcript（后端代理读 COS，前端零 COS 直连）：归属校验 → 读 COS 反序列化消息。
     * 对象缺失（未归档 / B2 异步归档未完成）→ 空列表（code 00000，非 50002），前端兜底 recentMessages。
     */
    public List<TutoringChatMessage> getTranscript(Long studentId, Long sessionId) {
        TutoringSession session = loadSession(sessionId, studentId);  // 不存在/越权/已软删 → 50002
        return transcriptArchiver.readMessages(session.getStudentId(), session.getId());
    }

    /** 会话历史列表：该学生全部会话（含已归档/终止，updated_at 倒序，不含软删）。 */
    public List<TutoringSessionListItemDTO> listSessions(Long studentId) {
        return tutoringSessionRepository.findListByStudentId(studentId).stream()
                .map(assembler::toListItemDTO)
                .toList();
    }

    /**
     * 删除会话：归属校验（loadSession，非本人/不存在/已软删 → 50002）→ 软删（is_deleted=1）→
     * 清 Redis 缓存（session+messages+active 三 key）。COS transcript/题目图片保留（可恢复）。
     */
    public void deleteSession(Long studentId, Long sessionId) {
        loadSession(sessionId, studentId);   // 归属/存在校验（越权 404 不泄露存在性）
        tutoringSessionRepository.softDelete(sessionId);
        sessionCache.clear(sessionId);
    }

    /** 主动收尾：end_reason=ABANDONED，掌握度不提升 + COS 终态写 + 清 Redis。 */
    public TutoringSessionDTO archive(Long studentId, Long sessionId) {
        return withSessionLock(sessionId, () -> {
            TutoringSession session = loadActiveSession(sessionId, studentId);
            List<TutoringChatMessage> history = sessionCache.listMessages(sessionId);
            guardrail.onEnd(session, EndReason.ABANDONED);
            String objectKey = transcriptArchiver.archive(session.getStudentId(), session.getId(),
                    session.getCreatedAt(), history, session.getStatus(), null);
            session.updateTranscriptUrl(objectKey);
            persistSession(session);
            sessionCache.clear(sessionId);
            return assembler.toSessionDTO(session, null, null);
        });
    }

    /** 查询学生知识点掌握度（图谱叠加数据源）。status/confidence 从派生观测关联（掌握度表只存确定的）。 */
    public StudentMasteryDTO getStudentMastery(Long studentId) {
        List<StudentKpMastery> list = masteryRepository.findByStudentId(studentId);
        Map<String, Integer> confidenceByKp = derivedKpObsRepository.findByStudentId(studentId).stream()
                .filter(o -> o.getKpUri() != null)
                .collect(Collectors.toMap(
                        DerivedKpObs::getKpUri,
                        DerivedKpObs::getConfidence,
                        Math::max));
        return StudentMasteryDTO.builder()
                .studentId(studentId)
                .items(list.stream()
                        .map(m -> {
                            String kpKey = m.getKpKey() == null ? null : m.getKpKey().getValue();
                            return MasteryItemDTO.builder()
                                    .kpKey(kpKey)
                                    .kpLabel(m.getKpLabel())
                                    .masteryLevel(m.getMasteryLevel() == null ? 0 : m.getMasteryLevel().getValue())
                                    .status("RESOLVED")
                                    .confidence(confidenceByKp.get(kpKey))
                                    .updatedAt(m.getUpdatedAt())
                                    .build();
                        })
                        .toList())
                .build();
    }

    // ==================== OCR 前置 ====================

    /**
     * 拍题识别（OCR 前置步骤）：图片 → Python /api/ocr/recognize → {text, confidence}。
     *
     * <p>仅校验图片有效性与格式（jpg/png），识别质量依赖识别服务，结果必须经学生确认/修改后再进答疑。
     * 无效图片 → 50006；Python 调用失败且重试后仍失败 → 50005。
     *
     * @param imageData        图片字节
     * @param originalFilename 原始文件名（jpg/jpeg/png）
     */
    public OcrResult ocr(byte[] imageData, String originalFilename) {
        // 11.1 ocr.enabled 开关：关闭时拍照识别不可用（前端隐藏拍照入口，仅手打/粘贴）
        if (!config().ocrEnabled()) {
            throw new BusinessException(ErrorCode.TUTORING_OCR_INVALID, "拍照识别未开启，请手动输入题目");
        }
        if (imageData == null || imageData.length == 0) {
            throw new BusinessException(ErrorCode.TUTORING_OCR_INVALID, "图片为空");
        }
        String name = originalFilename == null ? "" : originalFilename.toLowerCase();
        // 与 Python /api/ocr/recognize 允许集对齐（jpg/png/webp/bmp）
        if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".webp") || name.endsWith(".bmp"))) {
            throw new BusinessException(ErrorCode.TUTORING_OCR_INVALID, "仅支持 jpg/png/webp/bmp 图片");
        }
        try {
            return llmPort.recognize(imageData, originalFilename);
        } catch (TutoringAgentException e) {
            throw e; // 50005 Python 调用失败（含重试后）
        } catch (Exception e) {
            log.error("OCR 识别异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.TUTORING_AGENT_FAILED, "识别失败，请重试");
        }
    }

    /** 答疑配置端口（未注入时回退默认值，保持测试/默认行为一致）。 */
    private TutoringConfig config() {
        return tutoringConfig == null ? TutoringConfig.defaults() : tutoringConfig;
    }

    /** 前端能力开关（ocr.enabled → 前端据此显示/隐藏拍照入口）。 */
    public TutoringConfigDTO getTutoringConfig() {
        return TutoringConfigDTO.builder().ocrEnabled(config().ocrEnabled()).build();
    }

    // ==================== 11.2 会话并发锁 ====================

    /**
     * 同一会话串行化：Redis 锁（SET NX EX）包裹临界区（decide + 落库副作用），
     * 防并发双发导致 round/消息计数错乱。流式 generate 在锁外执行（不持锁长流）。
     *
     * <p>锁未获取（并发）→ 抛"会话繁忙"；未注入 RedisService（纯单元测试）时直通。
     */
    private <T> T withSessionLock(Long sessionId, Supplier<T> action) {
        if (redisService == null) {
            return action.get();
        }
        String lockKey = SESSION_LOCK_PREFIX + sessionId;
        String lockValue = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redisService.tryLock(lockKey, lockValue, SESSION_LOCK_SECONDS, TimeUnit.SECONDS))) {
            log.warn("[tutoring] 会话并发，拒绝本次消息, sessionId={}", sessionId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "会话繁忙，请稍后再试");
        }
        try {
            return action.get();
        } finally {
            redisService.unlock(lockKey, lockValue);
        }
    }

    /**
     * 同一会话串行化（D7 响应式版）：锁在订阅前获取，decide+副作用临界区结束后由 orchestrate 释放
     * （unlock 参数 + doFinally 兜底，幂等），generate 在锁外执行。sendMessage/requestAnswer 使用。
     * <p>锁被占（并发）→ 同步抛"会话繁忙"（不回流，保持原有 4xx 语义）；同步构建阶段异常 → 立即释放锁后重抛。
     */
    private Flux<ServerSentEvent<String>> withSessionLockReactive(Long sessionId,
            Function<Runnable, Flux<ServerSentEvent<String>>> action) {
        if (redisService == null) {
            return action.apply(() -> {});
        }
        String lockKey = SESSION_LOCK_PREFIX + sessionId;
        String lockValue = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redisService.tryLock(lockKey, lockValue, SESSION_LOCK_SECONDS, TimeUnit.SECONDS))) {
            log.warn("[tutoring] 会话并发，拒绝本次消息, sessionId={}", sessionId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "会话繁忙，请稍后再试");
        }
        AtomicBoolean released = new AtomicBoolean(false);
        Runnable unlock = () -> {
            if (released.compareAndSet(false, true)) {
                redisService.unlock(lockKey, lockValue);
            }
        };
        try {
            return action.apply(unlock);
        } catch (Exception e) {
            // 同步构建阶段异常（loadActiveSession/appendMessage/ctx 组装）→ 立即释放锁后重抛
            unlock.run();
            throw e;
        }
    }

    // ==================== 编排核心 ====================

    /**
     * decide（流式中继）→ meta 到达 → postDecide（护栏+副作用+generate）的统一编排（start/sendMessage/requestAnswer 共用）。
     *
     * <p><b>D7 响应式管线</b>：decide 不再同步 block——SSE 响应在 decide 一开始就建立，
     * Python decide 的 thinking + agent 事件实时中继前端（不入库），meta 到达后走 postDecide。
     * 时序：agent*(decide: perceive/analyze/plan/decide) → thinking*(decide) → agent(guardrail) → meta
     * → agent(generate) → thinking*(generate) → token* → agent(memory) → done。
     * decide agent 事件供前端"Agent 工作流"面板的本轮意图解析 live 展示。
     *
     * @param isNewQuestion 本轮学生是否上传了新的题目图片（换题信号，见 DecideContext.is_new_question）
     * @param unlock        并发锁释放函数（decide+副作用临界区结束后调用；错误路径 doFinally 兜底，幂等）
     */
    private Flux<ServerSentEvent<String>> orchestrate(TutoringSession session, List<TutoringChatMessage> history,
                                                      boolean isNewQuestion, Runnable unlock) {
        List<StudentKpMastery> masteryList = masteryRepository.findByStudentId(session.getStudentId());
        DecideContext ctx = contextAssembler.buildDecideContext(session, history, masteryList, isNewQuestion);

        // decide 响应式中继：thinking + decide 阶段 agent 事件（perceive/analyze/plan/decide）实时透传；
        // meta 到达 → metaSink；error 事件 / 流结束无 meta → 按失败
        Sinks.One<ActionMeta> metaSink = Sinks.one();
        AtomicBoolean metaReceived = new AtomicBoolean(false);
        Flux<ServerSentEvent<String>> decideThinking = llmPort.decideStream(ctx)
                .doOnNext(e -> {
                    if ("meta".equals(e.event())) {
                        metaReceived.set(true);
                        metaSink.tryEmitValue(readActionMeta(e.data()));
                    } else if ("error".equals(e.event())) {
                        metaSink.tryEmitError(new TutoringAgentException("答疑决策服务暂不可用"));
                    }
                })
                .doOnComplete(() -> {
                    // 流正常结束仍未收到 meta（空流 / 仅 agent+error 事件）→ 按 agent 失败处理，不重试
                    if (!metaReceived.get()) {
                        metaSink.tryEmitError(new TutoringAgentException("答疑决策服务暂不可用"));
                    }
                })
                .filter(e -> "thinking".equals(e.event()) || "agent".equals(e.event()));   // 中继 thinking + decide agent(perceive/analyze/plan/decide)，done 丢弃

        // meta 到达 → postDecide（同步护栏+副作用+guardrail+generate）→ 释放锁（generate 在锁外）
        Mono<Flux<ServerSentEvent<String>>> tail = metaSink.asMono()
                .map(action -> {
                    Flux<ServerSentEvent<String>> result = postDecide(session, action, history);
                    unlock.run();   // decide+副作用临界区结束，generate 在锁外
                    return result;
                });

        return Flux.concat(decideThinking, Mono.from(tail).flatMapMany(f -> f))
                .doFinally(sig -> unlock.run())               // 错误/取消路径兜底释放锁（幂等）
                .onErrorResume(e -> handleDecideFailure(session, e));
    }

    /** meta 到达后的护栏校验 + 落库副作用 + guardrail/generate 流（原 orchestrate 同步段，D7 抽取）。 */
    private Flux<ServerSentEvent<String>> postDecide(TutoringSession session, ActionMeta action,
                                                     List<TutoringChatMessage> history) {
        // 1. 安全终止（decide safety_flag）
        if (Boolean.TRUE.equals(action.getSafetyFlag())) {
            return terminate(session, action);
        }

        // 2. 护栏校验
        GuardResult guard = guardrail.validate(action, session);

        // 3. 终止类 end（无关/学习方法/非数学：type=end 且 end_reason 为空 → TERMINATED + 直接回复）
        if (guard.isAllowed() && isTerminationEnd(action)) {
            return terminate(session, action);
        }

        // 4. 轮次护栏拒绝（fallback=end）→ 强制收尾 ROUND_LIMIT，固定话术，无 generate
        if (!guard.isAllowed() && guard.getFallbackType() == ActionType.END) {
            return endByRoundLimit(session, action, history);
        }

        // 5. 放行 type（护栏拒绝时用 fallbackType，如 reveal→approach）
        ActionType allowedType = guard.isAllowed()
                ? ActionType.fromCodeOrDefault(action.getType())
                : guard.getFallbackType();

        // 6. 新会话落库 + 消息入缓存；已有会话复用
        ensurePersisted(session, history);

        // 7. 落库副作用（掌握度/错误/情绪/round/换题/收尾）
        String lastUserContent = lastUserContent(history);
        applySideEffects(session, action, allowedType, lastUserContent);
        tutoringSessionRepository.save(session);
        sessionCache.saveSession(session);

        // 8. 每轮实时整写 COS（幂等整写，首次即回填 transcript_url）——异步提交，首条 SSE 不等待 COS
        archiveAsync(session, history, action.getSummary());

        // 9. 流式（agent 事件协议）：agent(guardrail) → meta → agent(generate) → token* → agent(memory) → done
        //     guardrail 事件在护栏通过后、generate 前；会话结束（ARCHIVED/TERMINATED）后清 Redis
        return Flux.concat(
                Flux.just(agentEvent(AGENT_STAGE_GUARDRAIL, AGENT_LABEL_GUARDRAIL, "done", guardDetail(guard, action))),
                buildStream(session, action, allowedType, guard, history))
                .doOnComplete(() -> clearCacheIfEnded(session));
    }

    /**
     * decide 失败处理（D7）：start 阶段（id==null）重抛由接口层映射；已有会话回"网络波动"，会话保持 ACTIVE。
     * 仅 TutoringAgentException 走降级，其他异常原样上抛（非 agent 故障，不吞）。
     */
    private Flux<ServerSentEvent<String>> handleDecideFailure(TutoringSession session, Throwable e) {
        if (session.getId() == null) {
            return Flux.error(e);
        }
        if (e instanceof TutoringAgentException) {
            log.warn("[tutoring] Python agent 调用失败，回复网络波动, sessionId={}", session.getId());
            return friendlyErrorStream(session);
        }
        return Flux.error(e);
    }

    /** 解析 decide meta 事件 data → ActionMeta（宽容 ObjectMapper，容忍 Python 调试字段 reason 等未知字段）。 */
    private ActionMeta readActionMeta(String data) {
        try {
            return ACTION_META_MAPPER.readValue(data, ActionMeta.class);
        } catch (Exception e) {
            throw new TutoringAgentException("答疑决策服务暂不可用", e);
        }
    }

    // ==================== 副作用 ====================

    /** 落库副作用：情绪/答案计数/换题/收尾/轮次 + 掌握度信号 + 错误事件。 */
    private void applySideEffects(TutoringSession session, ActionMeta action,
                                  ActionType allowedType, String lastUserContent) {
        if (action.getEval() != null) {
            session.setLastEmotion(TutoringEmotion.fromCode(action.getEval().getEmotion()));
        }
        // 答案计数：学生表达要答案（decide 输出 reveal）即计数，第 1 次被拦成思路 / 第 2 次放行
        if (ActionType.REVEAL == ActionType.fromCode(action.getType())) {
            session.requestAnswer();
        }
        if (allowedType == ActionType.SWITCH) {
            session.switchQuestion();
        }
        if (allowedType == ActionType.END) {
            guardrail.onEnd(session, EndReason.fromCode(action.getEndReason()));
        }
        // B2: 第 2 次要答案放行 reveal（count≥1）→ 给完整答案后收尾 ANSWER_REVEALED（api.md 契约，防止答案反复要）
        if (allowedType == ActionType.REVEAL) {
            guardrail.onEnd(session, EndReason.ANSWER_REVEALED);
        }
        // 引导类（hint/approach）消耗轮次
        if (allowedType == ActionType.HINT || allowedType == ActionType.APPROACH) {
            session.recordRound();
        }
        applyMasteryAndErrors(session, action, allowedType, lastUserContent);
    }

    /** 掌握度 UPSERT（label→URI，未命中记日志不点亮）+ eval.correct=false 写错误事件。 */
    private void applyMasteryAndErrors(TutoringSession session, ActionMeta action,
                                       ActionType allowedType, String lastUserContent) {
        boolean completed = (allowedType == ActionType.END)
                && EndReason.COMPLETED == EndReason.fromCode(action.getEndReason());
        if (action.getMasterySignals() != null) {
            for (MasterySignalItem item : action.getMasterySignals()) {
                if (item.getKpLabel() == null || item.getKpLabel().isBlank()) {
                    continue;
                }
                KpResolution resolution = kpResolver.resolve(item.getKpLabel(), session.getStudentId());
                if (!resolution.isResolved() || resolution.getUri() == null) {
                    log.warn("[tutoring] 知识点 label 未命中 URI，不点亮: {}", item.getKpLabel());
                    continue;
                }
                KpKey kpKey = KpKey.of(resolution.getUri());
                MasterySignal signal = MasterySignal.fromCode(item.getKpLabel(), item.getSignal());
                StudentKpMastery mastery = masteryRepository.findByStudentAndKp(session.getStudentId(), kpKey)
                        .orElseGet(() -> StudentKpMastery.create(session.getStudentId(), kpKey, item.getKpLabel()));
                mastery.applySignal(signal);
                if (completed) {
                    mastery.raiseByCorrection();
                }
                // evidence 列是 JSON 类型，写入合法 JSON（原 "session_N" 非法 JSON 导致 MySQL 拒绝）
                mastery.recordSession(session.getId(), "{\"session_id\":" + session.getId() + "}");
                masteryRepository.upsert(mastery);
            }
        }
        // B3: 错误事件门控——仅 decide 原始 type 为 hint/approach（真实评估学生作答）且
        // 模型明确诊断出 error_type 才算学生错误。switch/end/reveal/concept 轮及首问 hint 轮
        // 的 correct=false 是模型默认值（error_type=null），非真实学生错误，不写。
        ActionType originalType = ActionType.fromCodeOrDefault(action.getType());
        if (action.getEval() != null && Boolean.FALSE.equals(action.getEval().getCorrect())
                && (originalType == ActionType.HINT || originalType == ActionType.APPROACH)
                && action.getEval().getErrorType() != null) {
            errorEventRepository.save(ErrorEvent.create(
                    session.getStudentId(), session.getId(),
                    firstKpUri(action),
                    action.getEval().getErrorType(),
                    TutoringEmotion.fromCode(action.getEval().getEmotion()),
                    session.getRoundCount(),
                    lastUserContent));
        }
    }

    // ==================== 流式构建 ====================

    private Flux<ServerSentEvent<String>> buildStream(TutoringSession session, ActionMeta action,
                                                      ActionType allowedType, GuardResult guard,
                                                      List<TutoringChatMessage> history) {
        SseMetaDTO meta = buildMeta(session, action, allowedType, guard);
        GenerateContext genCtx = contextAssembler.buildGenerateContext(history, allowedType, action);
        StringBuilder aiReply = new StringBuilder();
        StringBuilder thinkingBuf = new StringBuilder();
        Flux<ServerSentEvent<String>> tokenStream = llmPort.generate(genCtx)
                // 透传 Python 的 token（正文，累积 AI 回复）+ agent 事件（如 generate 阶段）
                // + thinking 事件（模型推理分片，前端"思考过程"面板按 chunk 拼接，同时累积落库）；
                // 其 meta/done 丢弃（Java 自建 meta/done）
                .filter(pyEvent -> "token".equals(pyEvent.event()) || "agent".equals(pyEvent.event())
                        || "thinking".equals(pyEvent.event()))
                .map(pyEvent -> {
                    if ("token".equals(pyEvent.event())) {
                        String content = extractTokenContent(pyEvent.data());
                        if (content != null) {
                            aiReply.append(content);
                        }
                        return ServerSentEvent.<String>builder()
                                .event("token").data(pyEvent.data()).build();
                    }
                    if ("thinking".equals(pyEvent.event())) {
                        // thinking 推理分片：原样中继给前端 + 累积进 thinkingBuf（历史消息落库用）
                        String content = extractTokenContent(pyEvent.data());
                        if (content != null) {
                            thinkingBuf.append(content);
                        }
                        return ServerSentEvent.<String>builder()
                                .event("thinking").data(pyEvent.data()).build();
                    }
                    // agent 事件原样中继（event: agent, data 为协议 JSON）
                    return ServerSentEvent.<String>builder()
                            .event("agent").data(pyEvent.data()).build();
                })
                .onErrorResume(e -> Flux.just(contentToken(AGENT_ERROR_REPLY)));
        SseDoneDTO done = buildDone(session, action);
        return Flux.concat(
                Flux.just(metaEvent(meta)),
                tokenStream,
                // Java 落库已完成（applySideEffects + archiveTranscript 在 buildStream 前执行），发"记忆更新"收尾信号
                Flux.just(agentEvent(AGENT_STAGE_MEMORY, AGENT_LABEL_MEMORY, "done", memoryDetail(action))),
                Flux.just(doneEvent(done)))
                // AI 回复落库：流结束后把完整回复（+ 推理过程 thinking）追加到 Redis 消息列表，
                // 并重新整写 COS（恒为完整对话，含 AI 回复与思考过程）
                .doOnComplete(() -> {
                    if (aiReply.length() > 0) {
                        String thinking = thinkingBuf.length() > 0 ? thinkingBuf.toString() : null;
                        // AI 消息补工作流 meta（与 buildMeta 同源镜像，值域见设计 D2）：
                        // 历史列表复原时前端 deriveTurnFlow 依赖这些字段，与 live SSE 渲染逐字段一致。
                        sessionCache.appendMessage(session.getId(),
                                TutoringChatMessage.builder()
                                        .role("ai")
                                        .content(aiReply.toString())
                                        .thinking(thinking)
                                        .createdAt(LocalDateTime.now())
                                        .type(allowedType.name().toLowerCase())
                                        .denied(guard.isAllowed() ? null
                                                : ActionType.fromCodeOrDefault(action.getType()).name().toLowerCase())
                                        .decideReason(action.getReason())
                                        .round(session.getRoundCount())
                                        .questionKps(action.getQuestionKps())
                                        .eval(action.getEval())
                                        .status(session.getStatus() == null ? null : session.getStatus().name())
                                        .build());
                        // 异步归档：消息列表同步捕获（含刚 append 的 AI 消息），
                        // 规避 clearCacheIfEnded 清缓存后异步重读为空
                        List<TutoringChatMessage> completeMessages = sessionCache.listMessages(session.getId());
                        archiveAsync(session, completeMessages, action.getSummary());
                    }
                });
    }

    /** 从 token 事件 data（{"content":"..."}）提取正文；解析失败返回 null（跳过，不阻断透传）。 */
    private String extractTokenContent(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            JsonNode node = SSE_MAPPER.readTree(data);
            JsonNode content = node.get("content");
            return content == null || content.isNull() ? null : content.asText();
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private SseMetaDTO buildMeta(TutoringSession session, ActionMeta action,
                                 ActionType allowedType, GuardResult guard) {
        SseMetaDTO meta = SseMetaDTO.builder()
                .sessionId(session.getId())
                .status(session.getStatus() == null ? null : session.getStatus().name())
                .type(allowedType.name().toLowerCase())
                .roundCount(session.getRoundCount())
                .answerRequestCount(session.getAnswerRequestCount())
                .newQuestion(action.getNewQuestion())
                .degraded(Boolean.TRUE.equals(action.getDegraded()))
                .build();
        if (action.getEval() != null) {
            meta.setEval(toSseEval(action.getEval()));
        }
        // 决策自由文本（Python reason，前端"为什么"hover 补充，可空）
        meta.setDecideReason(action.getReason());
        // 题目涉及知识点（decide 读题分析，可空）
        meta.setQuestionKps(action.getQuestionKps());
        // 掌握度信号（修复 meta.eval.masterySignals 恒空缺口：前端改读 meta.masterySignals）
        if (action.getMasterySignals() != null && !action.getMasterySignals().isEmpty()) {
            meta.setMasterySignals(action.getMasterySignals().stream()
                    .map(s -> SseMasterySignalDTO.builder()
                            .kpLabel(s.getKpLabel())
                            .signal(s.getSignal())
                            .build())
                    .toList());
        }
        if (!guard.isAllowed()) {
            meta.setDenied(ActionType.fromCodeOrDefault(action.getType()).name().toLowerCase());
            meta.setReason(guard.getDeniedReason());
        }
        return meta;
    }

    private SseDoneDTO buildDone(TutoringSession session, ActionMeta action) {
        return SseDoneDTO.builder()
                .sessionId(session.getId())
                .status(session.getStatus() == null ? null : session.getStatus().name())
                .roundCount(session.getRoundCount())
                .eval(action.getEval() == null ? null : toSseEval(action.getEval()))
                .summary(assembler.toSummary(action.getSummary()))
                .endReason(action.getEndReason())
                .build();
    }

    private SseEvalDTO toSseEval(EvalInfo eval) {
        return SseEvalDTO.builder()
                .correct(eval.getCorrect())
                .errorType(eval.getErrorType())
                .emotion(eval.getEmotion())
                .exerciseComplete(eval.getExerciseComplete())
                .build();
    }

    // ==================== 终止 / 轮次上限 / 降级 ====================

    /**
     * 终止场景（无关/学习方法/非数学/安全）：置 TERMINATED，回复在 meta.reply，无 token 流。
     * start() 阶段终止不建会话（避免无关内容污染会话表）；sendMessage 阶段置 TERMINATED + 清缓存。
     */
    private Flux<ServerSentEvent<String>> terminate(TutoringSession session, ActionMeta action) {
        String reply = (action.getSummary() != null && !action.getSummary().isBlank())
                ? action.getSummary() : "该内容超出答疑范围，请提出学习相关问题。";
        log.info("[tutoring] 终止会话: sessionId={}, reason=terminated", session.getId());
        if (session.getId() == null) {
            return Flux.just(metaEvent(SseMetaDTO.builder()
                    .status("TERMINATED").type("end").reply(reply).build()));
        }
        session.terminate(null);
        persistSession(session);
        sessionCache.clear(session.getId());
        return Flux.just(metaEvent(SseMetaDTO.builder()
                .sessionId(session.getId()).status("TERMINATED").type("end").reply(reply).build()));
    }

    /** 轮次护栏拒绝（round≥20）：强制收尾 ROUND_LIMIT，固定话术，无 generate。 */
    private Flux<ServerSentEvent<String>> endByRoundLimit(TutoringSession session, ActionMeta action,
                                                          List<TutoringChatMessage> history) {
        guardrail.onEnd(session, EndReason.ROUND_LIMIT);
        persistSession(session);
        archiveAsync(session, history, action.getSummary());
        SseMetaDTO meta = SseMetaDTO.builder()
                .sessionId(session.getId()).status(session.getStatus().name())
                .type("end").roundCount(session.getRoundCount())
                .answerRequestCount(session.getAnswerRequestCount())
                .build();
        SseDoneDTO done = SseDoneDTO.builder()
                .sessionId(session.getId()).status(session.getStatus().name())
                .roundCount(session.getRoundCount()).endReason(EndReason.ROUND_LIMIT.name())
                .build();
        sessionCache.clear(session.getId());
        return Flux.just(metaEvent(meta), contentToken(ROUND_LIMIT_REPLY), doneEvent(done));
    }

    /** Python 调用失败（已有会话）：meta + "网络波动，请重试" token + done，会话保持 ACTIVE。 */
    private Flux<ServerSentEvent<String>> friendlyErrorStream(TutoringSession session) {
        SseMetaDTO meta = SseMetaDTO.builder()
                .sessionId(session.getId()).status(session.getStatus().name())
                .type("hint").roundCount(session.getRoundCount()).build();
        SseDoneDTO done = SseDoneDTO.builder()
                .sessionId(session.getId()).status(session.getStatus().name())
                .roundCount(session.getRoundCount()).build();
        return Flux.just(metaEvent(meta), contentToken(AGENT_ERROR_REPLY), doneEvent(done));
    }

    // ==================== 持久化 / 归档 / 频率 ====================

    /** 新会话（start）落库 + 消息入缓存；已有会话（sendMessage）消息已在缓存中。 */
    private void ensurePersisted(TutoringSession session, List<TutoringChatMessage> history) {
        if (session.getId() != null) {
            return;
        }
        tutoringSessionRepository.save(session);
        sessionCache.saveSession(session);
        if (history != null) {
            for (TutoringChatMessage message : history) {
                sessionCache.appendMessage(session.getId(), message);
            }
        }
    }

    private void persistSession(TutoringSession session) {
        tutoringSessionRepository.save(session);
        sessionCache.saveSession(session);
    }

    /** 每轮对话实时整写 COS（幂等整写，按学生分目录）；首次写即回填 transcript_url + 刷新 Redis 快照。 */
    private void archiveTranscript(TutoringSession session, List<TutoringChatMessage> history, String summaryText) {
        String objectKey = transcriptArchiver.archive(session.getStudentId(), session.getId(),
                session.getCreatedAt(), history, session.getStatus(), summaryText);
        if (session.getTranscriptUrl() == null) {
            session.updateTranscriptUrl(objectKey);
            tutoringSessionRepository.updateTranscriptUrl(session.getId(), objectKey);
            // 刷新 Redis 快照，否则 ACTIVE 会话 GET 读到旧快照（transcriptUrl=null）
            sessionCache.saveSession(session);
        }
    }

    /** 归档异步化（B2 会话拆分修复）：提交到单线程调度器，SSE 响应不等待 COS 上传。 */
    private void archiveAsync(TutoringSession session, List<TutoringChatMessage> messages, String summaryText) {
        archiveScheduler.schedule(() -> archiveTranscript(session, messages, summaryText));
    }

    private void clearCacheIfEnded(TutoringSession session) {
        if (session.getStatus() != null && session.getStatus() != TutoringState.ACTIVE) {
            sessionCache.clear(session.getId());
        }
    }

    /** 会话创建频率限制：窗口内 > 上限 → 50004（配置 ai-edu.tutoring.create-limit）。 */
    private void ensureCreateAllowed(Long studentId) {
        boolean allowed = sessionCache.tryIncrementCreateCount(studentId,
                config().createWindowMinutes(), config().createLimit());
        if (!allowed) {
            throw new BusinessException(ErrorCode.TUTORING_CREATE_FREQUENT, "创建会话过于频繁，请稍后再试");
        }
    }

    // ==================== 会话加载 / 工具 ====================

    private TutoringSession loadActiveSession(Long sessionId, Long studentId) {
        TutoringSession session = loadSession(sessionId, studentId);
        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.TUTORING_SESSION_ENDED, "会话已结束或已归档");
        }
        return session;
    }

    private TutoringSession loadSession(Long sessionId, Long studentId) {
        TutoringSession session = sessionCache.findSession(sessionId)
                .orElseGet(() -> tutoringSessionRepository.findById(sessionId).orElse(null));
        if (session == null) {
            throw new BusinessException(ErrorCode.TUTORING_SESSION_NOT_FOUND, "会话不存在");
        }
        if (!session.getStudentId().equals(studentId)) {
            throw new BusinessException(ErrorCode.TUTORING_SESSION_NOT_FOUND, "会话不存在");
        }
        return session;
    }

    /** 终止类 end：type=end 且 end_reason 为空（无关/学习方法/非数学，回复在 summary）。 */
    private boolean isTerminationEnd(ActionMeta action) {
        return ActionType.END == ActionType.fromCode(action.getType())
                && (action.getEndReason() == null || action.getEndReason().isBlank());
    }

    private KpKey firstKpUri(ActionMeta action) {
        if (action.getMasterySignals() == null) {
            return null;
        }
        for (MasterySignalItem item : action.getMasterySignals()) {
            if (item.getKpLabel() != null && !item.getKpLabel().isBlank()) {
                String uri = kpResolver.resolveLabelToUri(item.getKpLabel());
                if (uri != null) {
                    return KpKey.of(uri);
                }
            }
        }
        return null;
    }

    private String lastUserContent(List<TutoringChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).getRole())) {
                return history.get(i).getContent();
            }
        }
        return null;
    }

    // ==================== 图片消息 / 换题信号 ====================

    /** 学生消息构建：文字或图片（图片上传 COS 取 URL；sessionId 为空 = start 未落库，用 pending 目录）。 */
    private TutoringChatMessage buildUserMessage(Long studentId, Long sessionId, String content,
                                                 byte[] imageData, String originalFilename) {
        if (imageData == null || imageData.length == 0) {
            return TutoringChatMessage.user(content);
        }
        String url = uploadQuestionImage(studentId, sessionId, imageData, originalFilename);
        return TutoringChatMessage.userWithImage(content, url);
    }

    /** 会话标题：首条用户消息内容前 {@value #SESSION_TITLE_MAX_LENGTH} 字；图片题无文字兜底「图片题目」。 */
    private String buildSessionTitle(String message, byte[] imageData) {
        String text = (message == null || message.isBlank())
                ? (imageData == null || imageData.length == 0 ? SESSION_TITLE_EMPTY_FALLBACK : SESSION_TITLE_IMAGE_FALLBACK)
                : message.trim();
        return text.length() <= SESSION_TITLE_MAX_LENGTH ? text : text.substring(0, SESSION_TITLE_MAX_LENGTH);
    }

    /**
     * 题目图片上传 COS：按学生/会话组织 + 时间戳命名
     * {@code tutoring/questions/{studentId}/{sessionId}/{yyyyMMdd-HHmmss-SSS}.ext}，
     * 便于按时间排序/清理、与对话 transcript 同会话结构。返回可访问 URL（存进消息 image_url）。
     */
    private String uploadQuestionImage(Long studentId, Long sessionId, byte[] imageData, String originalFilename) {
        validateImageFormat(originalFilename);
        if (fileStorageService == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "文件存储未配置");
        }
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "图片上传需先建立会话");
        }
        String objectKey = "tutoring/questions/" + studentId + "/" + sessionId + "/"
                + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").format(LocalDateTime.now())
                + fileExtension(originalFilename);
        fileStorageService.uploadToObjectKey(objectKey, imageData, imageContentType(originalFilename));
        return fileStorageService.getUrl(objectKey);
    }

    /** 取扩展名（含点，如 .png）；无扩展名回退 .png。 */
    private String fileExtension(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : ".png";
    }

    /** 图片格式白名单（与 OCR 允许集一致：jpg/jpeg/png/webp/bmp）。 */
    private void validateImageFormat(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.toLowerCase();
        if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".webp") || name.endsWith(".bmp"))) {
            throw new BusinessException(ErrorCode.TUTORING_OCR_INVALID, "仅支持 jpg/png/webp/bmp 图片");
        }
    }

    private String imageContentType(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.toLowerCase();
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        if (name.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "application/octet-stream";
    }

    /** 换题信号判定（Java 侧权威）：该图 URL 是否已在历史中出现过。新 URL = 本轮新增题目图。 */
    private boolean historyContainsImageUrl(List<TutoringChatMessage> history, String url) {
        if (history == null || url == null) {
            return false;
        }
        return history.stream().anyMatch(m -> url.equals(m.getImageUrl()));
    }

    // ==================== SSE 序列化 ====================

    private ServerSentEvent<String> metaEvent(SseMetaDTO meta) {
        return ServerSentEvent.<String>builder().event("meta").data(write(meta)).build();
    }

    private ServerSentEvent<String> doneEvent(SseDoneDTO done) {
        return ServerSentEvent.<String>builder().event("done").data(write(done)).build();
    }

    private ServerSentEvent<String> contentToken(String content) {
        return ServerSentEvent.<String>builder().event("token").data(write(Map.of("content", content))).build();
    }

    /** agent 事件（tutoring-agent-protocol 协议：{level, stage, label, status, detail}，level 恒 sub）。 */
    private ServerSentEvent<String> agentEvent(String stage, String label, String status, String detail) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("level", "sub");
        data.put("stage", stage);
        data.put("label", label);
        data.put("status", status);
        if (detail != null) {
            data.put("detail", detail);
        }
        return ServerSentEvent.<String>builder().event("agent").data(write(data)).build();
    }

    /** guardrail 事件 detail：放行类型或拒绝降级摘要。 */
    private String guardDetail(GuardResult guard, ActionMeta action) {
        String type = ActionType.fromCodeOrDefault(action.getType()).name().toLowerCase();
        if (guard.isAllowed()) {
            return "放行: " + type;
        }
        return "拒绝: " + type + " → 降级 " + guard.getFallbackType().name().toLowerCase();
    }

    /** memory 事件 detail：汇总本轮 mastery 信号（如 "二元一次方程组 → 练习中"）；无信号为 null。 */
    private String memoryDetail(ActionMeta action) {
        if (action.getMasterySignals() == null || action.getMasterySignals().isEmpty()) {
            return null;
        }
        return action.getMasterySignals().stream()
                .map(s -> s.getKpLabel() + " → " + s.getSignal())
                .reduce((a, b) -> a + "；" + b)
                .orElse(null);
    }

    private String write(Object value) {
        try {
            return SSE_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("SSE 事件序列化失败: " + e.getMessage(), e);
        }
    }
}
