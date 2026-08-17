package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.QuestionAnalysisDTO;
import com.ai.edu.application.dto.learning.QuestionAnalysisKpDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.repository.KgKnowledgePointRepository;
import com.ai.edu.domain.learning.model.contract.QuestionUnderstandResult;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import com.ai.edu.domain.learning.repository.DerivedKpObsRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.service.QuestionUnderstandingPort;
import com.ai.edu.domain.learning.service.TutoringKpResolver;
import com.ai.edu.domain.learning.service.TutoringLlmPort;
import com.ai.edu.domain.shared.service.FileStorageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 单题分析应用服务（POST /api/kp/analyze-question）——题目文本 → 题型名 → 关联知识点清单。
 *
 * <p>本期范围（前端降级 2026-08-17）：
 * ① 题型库 canonical/别名命中 → 权威分布（数据驱动）；
 * ② 题库 miss → PENDING + 挂起 PENDING obs（空可接受，前端 keyword 搜索确认兜底）。
 * <p>「题库和知识点」池约束选择（D8）已抽到 {@link KpPoolAssociateService}，
 * 本期 analyze 未接线，待独立迭代启用（题库 miss → 学段池 → LLM 从池选 top-N → 恒非空）。
 */
@Slf4j
@Service
public class KpQuestionAnalysisAppService {

    @Resource
    private QuestionUnderstandingPort questionUnderstandingPort;
    @Resource
    private TutoringKpResolver tutoringKpResolver;
    @Resource
    private QuestionTypeRepository questionTypeRepository;
    @Resource
    private QuestionTypeKpRepository questionTypeKpRepository;
    @Resource
    private KgKnowledgePointRepository kgKnowledgePointRepository;
    @Resource
    private DerivedKpObsRepository derivedKpObsRepository;
    @Resource
    private FileStorageService fileStorageService;
    @Resource
    private TutoringLlmPort tutoringLlmPort;

    public QuestionAnalysisDTO analyze(String text, Long studentId) {
        Integer grade = tutoringKpResolver.resolveStudentGrade(studentId);
        List<String> topics = questionUnderstandingPort.understand(text, grade);

        // ① 题型库 canonical/别名命中 → 权威分布（数据驱动，顺序无关）
        if (!topics.isEmpty()) {
            for (String topic : topics) {
                Optional<QuestionType> qt = questionTypeRepository.findByTopicLabelOrAlias(topic);
                if (qt.isPresent()) {
                    return catalogResult(qt.get());
                }
            }
        }

        // ② 题库 miss → PENDING + 挂起（空可接受；池约束选择待「题库和知识点」迭代接线）
        if (studentId != null && !topics.isEmpty()) {
            derivedKpObsRepository.upsertPendingIfAbsent(studentId, topics.get(0), grade);
        }
        return QuestionAnalysisDTO.pending(topics.isEmpty() ? null : topics.get(0), List.of());
    }

    /**
     * 图片题目分析（POST /api/kp/analyze-question/image）：上传 COS → Python 视觉模型看图识别题型 →
     * 复用文本路径编排（题型库命中权威 / 顺带知识点 / PENDING）。
     */
    public QuestionAnalysisDTO analyzeImage(byte[] imageData, String originalFilename, Long studentId) {
        Integer grade = tutoringKpResolver.resolveStudentGrade(studentId);

        // ① 无会话上传 COS（analyze 无会话，路径 tutoring/questions/{studentId}/analyze/{ts}.ext）
        String objectKey = uploadAnalyzeImage(studentId, imageData, originalFilename);
        String signedUrl = fileStorageService.generatePresignedUrl(objectKey, 30);

        // ② topicHint 收敛命名（题型库常用名，让视觉识别朝既有词汇收敛）
        List<String> topicHint = questionTypeRepository.findTopTopicLabels(20);

        // ③ Python 视觉模型直接看图 → 题型名 + 顺带知识点（模型 Python 侧写死）
        QuestionUnderstandResult result = tutoringLlmPort.understandQuestion(signedUrl, topicHint, grade);
        if (result == null || result.isFailed()) {
            return QuestionAnalysisDTO.pending(null, List.of()); // 识别失败 → PENDING（不报错）
        }
        List<String> topicLabels = result.getTopicLabels();

        // ④ 题型库命中 → 权威分布
        for (String topic : topicLabels) {
            Optional<QuestionType> qt = questionTypeRepository.findByTopicLabelOrAlias(topic);
            if (qt.isPresent()) {
                return catalogResult(qt.get());
            }
        }

        // ⑤ Python 顺带知识点（有则展示，镜像校验，不强求）
        List<QuestionAnalysisKpDTO> kps = result.getQuestionKps() == null ? List.of()
                : result.getQuestionKps().stream()
                        .map(this::imageKpDto)
                        .filter(Objects::nonNull)
                        .toList();
        if (!kps.isEmpty()) {
            return QuestionAnalysisDTO.resolved(topicLabels.get(0), 60, kps);
        }

        // ⑥ 无关联 → PENDING + 挂起
        if (studentId != null) {
            derivedKpObsRepository.upsertPendingIfAbsent(studentId, topicLabels.get(0), grade);
        }
        return QuestionAnalysisDTO.pending(topicLabels.get(0), List.of());
    }

    /** Python 顺带知识点 label → DTO（镜像反查 URI；不在镜像丢弃）。 */
    private QuestionAnalysisKpDTO imageKpDto(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String uri = kgKnowledgePointRepository.findByLabel(label)
                .map(KgKnowledgePoint::getUri)
                .orElse(null);
        if (uri == null) {
            return null;
        }
        return QuestionAnalysisKpDTO.builder()
                .kpUri(uri).kpLabel(label).gradeRange(null).ratio(null)
                .build();
    }

    /** 无会话图片上传 COS（analyze 无 tutoring 会话）。 */
    private String uploadAnalyzeImage(Long studentId, byte[] imageData, String originalFilename) {
        validateImageFormat(originalFilename);
        if (fileStorageService == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "文件存储未配置");
        }
        String objectKey = "tutoring/questions/" + studentId + "/analyze/"
                + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").format(LocalDateTime.now())
                + fileExtension(originalFilename);
        fileStorageService.uploadToObjectKey(objectKey, imageData, imageContentType(originalFilename));
        return objectKey;
    }

    /** 图片格式白名单（与答疑 OCR 允许集一致：jpg/jpeg/png/webp/bmp）。 */
    private void validateImageFormat(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.toLowerCase();
        if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".webp") || name.endsWith(".bmp"))) {
            throw new BusinessException(ErrorCode.TUTORING_OCR_INVALID, "仅支持 jpg/png/webp/bmp 图片");
        }
    }

    private String fileExtension(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : ".png";
    }

    private String imageContentType(String originalFilename) {
        String ext = fileExtension(originalFilename);
        return switch (ext) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".webp" -> "image/webp";
            case ".bmp" -> "image/bmp";
            default -> "image/png";
        };
    }

    /** 题型库命中：全部关联知识点分布（kpLabel 从镜像反查，gradeRange/ratio 透传），confidence=最大占比。 */
    private QuestionAnalysisDTO catalogResult(QuestionType qt) {
        List<QuestionTypeKp> kps = questionTypeKpRepository.findByQuestionTypeId(qt.getId());
        double maxRatio = kps.stream()
                .mapToDouble(k -> k.getRatio() == null ? 0.0 : k.getRatio())
                .max().orElse(0.0);
        List<QuestionAnalysisKpDTO> items = kps.stream()
                .map(k -> QuestionAnalysisKpDTO.builder()
                        .kpUri(k.getKpUri())
                        .kpLabel(kpLabelOf(k.getKpUri()))
                        .gradeRange(k.getGradeRange())
                        .ratio(k.getRatio())
                        .build())
                .toList();
        return QuestionAnalysisDTO.resolved(qt.getTopicLabel(), (int) Math.round(maxRatio * 100), items);
    }

    private String kpLabelOf(String kpUri) {
        return kgKnowledgePointRepository.findByUri(kpUri)
                .map(KgKnowledgePoint::getLabel)
                .orElse(null);
    }
}
