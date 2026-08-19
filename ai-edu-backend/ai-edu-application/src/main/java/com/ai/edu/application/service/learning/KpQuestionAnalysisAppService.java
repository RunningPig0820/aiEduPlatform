package com.ai.edu.application.service.learning;

import com.ai.edu.application.dto.learning.QuestionAnalysisDTO;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.learning.model.contract.QuestionUnderstandResult;
import com.ai.edu.domain.learning.service.QuestionUnderstandingPort;
import com.ai.edu.domain.learning.service.TutoringLlmPort;
import com.ai.edu.domain.shared.service.FileStorageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 题型分析应用服务（纯 Python 直通小工具）。
 *
 * <p>文本/图片 → Python 识别 → 题型名直接返回展示模型能力。**无业务功能**：
 * 不题库命中、不 canonical 聚集、不落 record 表、不写观测。图片上传 COS 仅为传输
 * （Python question-understand 契约接收 imageUrl），识别失败返回 PENDING 不报错。
 */
@Service
public class KpQuestionAnalysisAppService {

    @Resource
    private QuestionUnderstandingPort questionUnderstandingPort;
    @Resource
    private TutoringLlmPort tutoringLlmPort;
    @Resource
    private FileStorageService fileStorageService;

    /** 文本题型分析：文本 → Python 理解 → 题型名（纯展示，不落库）。 */
    public QuestionAnalysisDTO analyze(String text, Long studentId) {
        List<String> topics = questionUnderstandingPort.understand(text, null);
        if (topics == null || topics.isEmpty()) {
            return QuestionAnalysisDTO.pending(null, List.of()); // 识别失败 → PENDING（不报错）
        }
        return QuestionAnalysisDTO.resolved(topics.get(0), 0, List.of());
    }

    /** 图片题型分析：上传 COS（传输）→ Python 视觉模型看图 → 题型名（纯展示，不落库）；
     *  返回原题图 URL（1h 有效）供前端展示原题。 */
    public QuestionAnalysisDTO analyzeImage(byte[] imageData, String originalFilename, Long studentId) {
        String objectKey = uploadAnalyzeImage(studentId, imageData, originalFilename);
        // Python 视觉模型用短时签名拉图；前端展示原题用较长有效期 URL
        String pythonUrl = fileStorageService.generatePresignedUrl(objectKey, 30);
        String previewUrl = fileStorageService.generatePresignedUrl(objectKey, 3600);
        QuestionUnderstandResult result = tutoringLlmPort.understandQuestion(pythonUrl, null, null);
        QuestionAnalysisDTO dto = (result == null || result.isFailed())
                ? QuestionAnalysisDTO.pending(null, List.of()) // 识别失败 → PENDING（不报错）
                : QuestionAnalysisDTO.resolved(result.getTopicLabels().get(0), 0, List.of());
        dto.setImageUrl(previewUrl);
        return dto;
    }

    /** 无会话图片上传 COS（analyze 无 tutoring 会话，路径 tutoring/questions/{studentId}/analyze/{ts}.ext）。 */
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
}
