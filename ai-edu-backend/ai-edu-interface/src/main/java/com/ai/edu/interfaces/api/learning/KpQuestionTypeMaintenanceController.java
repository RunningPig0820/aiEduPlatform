package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.QuestionTypeKpDTO;
import com.ai.edu.application.dto.learning.QuestionTypePageItemDTO;
import com.ai.edu.application.service.learning.KpQuestionTypeMaintenanceAppService;
import com.ai.edu.common.constant.ErrorCode;
import com.ai.edu.common.exception.BusinessException;
import com.ai.edu.domain.learning.model.entity.QuestionType;
import com.ai.edu.domain.learning.model.entity.QuestionTypeKp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题型↔知识点维护接口（ADMIN，域 B 独立逻辑 tasks 2.0.5）。
 *
 * <p>独立维护替代「obs 共现自动涌现」（聚合/挂起/澄清批处理停用）：手动配题型 + 知识点分布，
 * 入口（analyze-question / 答疑）只读查表即命中权威分布。演示：配「鸡兔同笼 → 鸡兔同笼问题(0.6)/假设法(0.4)」。
 */
@RestController
@RequestMapping("/api/kp/type")
@Tag(name = "题型维护", description = "题型↔知识点 ADMIN 维护（域 B 独立逻辑）")
public class KpQuestionTypeMaintenanceController {

    @Resource
    private KpQuestionTypeMaintenanceAppService appService;

    /** POST /api/kp/type/upsert — 建/更新题型（CANDIDATE，topic_label 幂等）。 */
    @Operation(summary = "建/更新题型", description = "topic_label 唯一，已存在幂等返回（ADMIN）")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upsert")
    public ApiResponse<QuestionTypePageItemDTO> upsertType(@RequestBody UpsertTypeRequest request) {
        if (request == null || request.getTopicLabel() == null || request.getTopicLabel().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "topicLabel 不能为空");
        }
        return ApiResponse.success(toItemDto(appService.upsertType(request.getTopicLabel())));
    }

    /** POST /api/kp/type/{id}/status — 升 STABLE（手动审核）。 */
    @Operation(summary = "题型升 STABLE", description = "手动审核替代聚合阈值判断（ADMIN）")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/status")
    public ApiResponse<QuestionTypePageItemDTO> promote(@PathVariable Long id, @RequestBody(required = false) PromoteRequest request) {
        return ApiResponse.success(toItemDto(appService.promote(id, request == null ? null : request.getDefinition())));
    }

    /** POST /api/kp/type/{id}/kp — 绑知识点分布桶（kp_uri + ratio + grade_range）。 */
    @Operation(summary = "绑定知识点分布", description = "手动维护题型↔知识点关联（ADMIN），入口查表即命中")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/kp")
    public ApiResponse<QuestionTypeKpDTO> bindKp(@PathVariable Long id, @RequestBody BindKpRequest request) {
        if (request == null || request.getKpUri() == null || request.getKpUri().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "kpUri 不能为空");
        }
        QuestionTypeKp kp = appService.bindKp(id, request.getKpUri(),
                request.getRatio() == null ? 0.0 : request.getRatio(), request.getGradeRange());
        return ApiResponse.success(QuestionTypeKpDTO.builder()
                .kpUri(kp.getKpUri()).gradeRange(kp.getGradeRange()).ratio(kp.getRatio()).build());
    }

    /** POST /api/kp/type/{id}/alias — 加变体别名（变体题型名 → canonical）。 */
    @Operation(summary = "加变体别名", description = "变体题型名收敛到 canonical 题型（ADMIN）")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/alias")
    public ApiResponse<Void> addAlias(@PathVariable Long id, @RequestBody AddAliasRequest request) {
        if (request == null || request.getAliasLabel() == null || request.getAliasLabel().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "aliasLabel 不能为空");
        }
        appService.addAlias(id, request.getAliasLabel());
        return ApiResponse.success();
    }

    private QuestionTypePageItemDTO toItemDto(QuestionType qt) {
        return QuestionTypePageItemDTO.builder()
                .id(qt.getId())
                .topicLabel(qt.getTopicLabel())
                .status(qt.getStatus() == null ? null : qt.getStatus().name())
                .hitCount(qt.getHitCount())
                .build();
    }

    /** 建/更新题型请求体。 */
    @Data
    public static class UpsertTypeRequest {
        private String topicLabel;
    }

    /** 升 STABLE 请求体（definition 可空）。 */
    @Data
    public static class PromoteRequest {
        private String definition;
    }

    /** 绑知识点分布请求体。 */
    @Data
    public static class BindKpRequest {
        private String kpUri;
        private Double ratio;
        private String gradeRange;
    }

    /** 加别名请求体。 */
    @Data
    public static class AddAliasRequest {
        private String aliasLabel;
    }
}
