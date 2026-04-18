package com.ai.edu.common.util;

import com.ai.edu.common.dto.kg.UriValidationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * URI 格式校验工具类
 */
public final class UriValidator {

    private UriValidator() {}

    /**
     * 校验多组 URI 合法性
     * @param uriGroups 每组包含 (label, uris) 的键值对
     * @return 校验结果
     */
    @SafeVarargs
    public static UriValidationResult validateAllUris(Map<String, List<String>>... uriGroups) {
        List<String> allErrors = new ArrayList<>();
        boolean allValid = true;

        for (Map<String, List<String>> group : uriGroups) {
            for (Map.Entry<String, List<String>> entry : group.entrySet()) {
                UriValidationResult result = validateUris(entry.getValue(), entry.getKey());
                if (!result.valid) { allValid = false; allErrors.addAll(result.errors); }
            }
        }

        return new UriValidationResult(allValid, allErrors);
    }

    public static UriValidationResult validateUris(List<String> uris, String nodeType) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String uri : uris) {
            if (uri == null || uri.isBlank()) {
                errors.add(String.format("[%s] URI is null or blank", nodeType));
                continue;
            }
            if (!uri.contains(":") || uri.length() < 10) {
                errors.add(String.format("[%s] Invalid URI format: %s", nodeType, uri));
                continue;
            }
            if (seen.contains(uri)) {
                errors.add(String.format("[%s] Duplicate URI: %s", nodeType, uri));
            }
            seen.add(uri);
        }

        return new UriValidationResult(errors.isEmpty(), errors);
    }
}
