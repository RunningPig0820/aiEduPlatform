package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.contract.SubjectClassifyRequest;
import com.ai.edu.domain.learning.model.contract.SubjectClassifyResult;

/**
 * 学科分类端口（Java → Python subject-classify，tasks 2.1）。
 *
 * <p>学科无关分类器：decide 之前判定题目学科（文本 + 图片），非 math 由编排层跳过。
 * 默认实现 {@code SubjectClassifyClient}（infra，WebClient 调 Python stateless 端点）。
 *
 * <p><b>失败语义</b>：绝不抛异常——异常/超时/空响应 → 返回空 subject，
 * 由编排层按 math 放行（宁可漏拦非数学题，不误拦数学题，见 design Risks）。
 */
public interface SubjectClassifyPort {

    /**
     * 判定题目学科。
     *
     * @param request 请求（content / image_url 至少一个非空）
     * @return 识别结果；失败/空 → {@link SubjectClassifyResult#isEmpty()}（subject=null）
     */
    SubjectClassifyResult classify(SubjectClassifyRequest request);
}
