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
import com.ai.edu.domain.learning.model.entity.StudentQuestionRecord;
import com.ai.edu.domain.learning.repository.QuestionTypeKpRepository;
import com.ai.edu.domain.learning.repository.QuestionTypeRepository;
import com.ai.edu.domain.learning.repository.StudentQuestionRecordRepository;
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
 * 单题分析应用服务（POST /api/kp/analyze-question）——题目文本 → 题型名 →（查题型库）关联知识点。
 *
 * <p>域 B 独立化（Decision 10，2026-08-18）：入口只到「题型」阶段——识别题型后查题型库，
 * 命中返回权威分布 / 未命中返回「仅题型 + 空知识点」；**不再自动关联**（不顺带 Python kps、
 * 不挂起 PENDING obs、不写 t_kp_derived_obs）。题型↔知识点关联由 ADMIN 维护接口手动配（见 tasks 2.0.5）。
 * <p>canonical 返回（「解一元二次方程」→「一元二次方程」）由 2.7.2 聚集 post-process 接入。
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
    private FileStorageService fileStorageService;
    @Resource
    private TutoringLlmPort tutoringLlmPort;
    @Resource
    private StudentQuestionRecordRepository questionRecordRepository;
    @Resource
    private TopicLabelAggregationService topicLabelAggregationService;

    public QuestionAnalysisDTO analyze(String text, Long studentId) {
        Integer grade = tutoringKpResolver.resolveStudentGrade(studentId);
        List<String> topics = questionUnderstandingPort.understand(text, grade);

        // ① 题型库 canonical/别名命中 → 权威分布（数据驱动，顺序无关）
        if (!topics.isEmpty()) {
            for (String topic : topics) {
                Optional<QuestionType> qt = questionTypeRepository.findByTopicLabelOrAlias(topic);
                if (qt.isPresent()) {
                    return analyzeResult(text, topic, studentId, qt.get());
                }
            }
        }

        // ② 题库 miss → 仅题型（域 B 独立化：不挂起 PENDING、不写 obs；canonical 由聚集 post-process 返回）
        if (topics.isEmpty()) {
            return QuestionAnalysisDTO.pending(null, List.of()); // 题型识别失败 → PENDING（不报错）
        }
        return analyzeResult(text, topics.get(0), studentId, null);
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
                return analyzeResult("[图片题目]", topic, studentId, qt.get());
            }
        }

        // ⑤ 题库 miss → 仅题型（域 B 独立化：不顺带 Python kps、不挂起 PENDING；canonical 由聚集 post-process 返回）
        return analyzeResult("[图片题目]", topicLabels.get(0), studentId, null);
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

    /**
     * 识别成功统一出口：题目落库（source=ai，score=null 无信号）+ 返回 DTO。
     * 命中题库 → 权威分布（canonical=权威名）；miss → 过聚集返回 canonical（2.7.2，前端查 getMastery 对上）。
     */
    private QuestionAnalysisDTO analyzeResult(String content, String topicLabel, Long studentId, QuestionType hit) {
        String canonical;
        QuestionAnalysisDTO dto;
        if (hit != null) {
            canonical = hit.getTopicLabel();
            dto = catalogResult(hit);
        } else {
            canonical = topicLabelAggregationService.aggregate(topicLabel, studentId);
            dto = QuestionAnalysisDTO.resolved(canonical, 0, List.of());
        }
        saveAnalyzeRecord(studentId, content, topicLabel, canonical);
        return dto;
    }

    /** analyze 题目落库（2.7.1）：source=ai，score=null（无对错信号，SIG-007，不参与掌握表聚合）。 */
    private void saveAnalyzeRecord(Long studentId, String content, String topicLabel, String canonical) {
        questionRecordRepository.save(StudentQuestionRecord.create("ai", studentId, content, topicLabel,
                canonical, null, 0, 0, null, LocalDateTime.now()));
        log.info("[analyze-question] 题目落库: student={}, topic={}, canonical={}", studentId, topicLabel, canonical);
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
