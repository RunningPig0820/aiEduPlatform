package com.ai.edu.domain.learning.model.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 掌握度快照项（mastery_snapshot 单项：{kp_key, label, mastery_level}，Java→Python 契约 snake_case）。
 *
 * <p><b>label 必带</b>——Python 用它做 label 接地（优先复用已知知识点名，降低 Java label→URI 解析噪声）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** TextbookKP URI */
    @JsonProperty("kp_key")
    private String kpKey;

    /** 知识点名（label 接地用，必带） */
    private String label;

    /** 掌握度分值 0-100 */
    @JsonProperty("mastery_level")
    private int masteryLevel;
}
