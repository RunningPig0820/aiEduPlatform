package com.ai.edu.common.dto.kg;

import java.util.List;

/**
 * URI 校验结果
 */
public class UriValidationResult {
    public final boolean valid;
    public final List<String> errors;

    public UriValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }
}
