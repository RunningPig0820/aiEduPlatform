package com.ai.edu.interfaces.api.learning;

import com.ai.edu.application.dto.ApiResponse;
import com.ai.edu.application.dto.learning.ConfirmKpAliasDTO;
import com.ai.edu.application.dto.learning.PendingKpAliasDTO;
import com.ai.edu.application.service.learning.KpAppService;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 挂起审核接口（/api/kg/aliases/pending）——列出挂起观测 + 人工确认归属，仅 ADMIN/TEACHER 可访问。
 */
@RestController
@RequestMapping("/api/kg/aliases")
@Tag(name = "挂起审核", description = "派生观测挂起清单与人工确认")
public class KpAliasReviewController {

    @Resource
    private KpAppService kpAppService;

    /** GET /api/kg/aliases/pending — 列出挂起（PENDING/HUMAN_REVIEW）观测。 */
    @Operation(summary = "挂起清单", description = "列出待审核的派生观测（PENDING/HUMAN_REVIEW）")
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ApiResponse<List<PendingKpAliasDTO>> listPending() {
        return ApiResponse.success(kpAppService.listPending());
    }

    /** POST /api/kg/aliases/pending/{id}/confirm — 确认挂起观测归属知识点 URI。 */
    @Operation(summary = "确认挂起", description = "确认观测归属知识点，观测转 RESOLVED，回流题型库统计")
    @PostMapping("/pending/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ApiResponse<ConfirmKpAliasDTO> confirm(@PathVariable Long id,
                                                  @RequestBody ConfirmRequest request) {
        return ApiResponse.success(kpAppService.confirm(id, request.getKpUri()));
    }

    /** 确认请求体。 */
    @Data
    public static class ConfirmRequest {
        @JsonProperty("kp_uri")
        private String kpUri;
    }
}
