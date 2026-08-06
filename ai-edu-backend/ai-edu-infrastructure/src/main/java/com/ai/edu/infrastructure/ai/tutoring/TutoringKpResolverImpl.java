package com.ai.edu.infrastructure.ai.tutoring;

import com.ai.edu.domain.learning.service.TutoringKpResolver;
import com.ai.edu.infrastructure.persistence.edukg.mapper.KgKnowledgePointMapper;
import com.ai.edu.infrastructure.persistence.edukg.po.KgKnowledgePointPo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 知识点 label → TextbookKP URI 解析实现（kg-sync MySQL 镜像）。
 *
 * <p>解析链路：精确匹配 → LIKE 模糊 → 未命中记日志返回 null（收尾标记"待收录"，不点亮）。
 * 解析失败不影响答疑主流程。
 *
 * <p>说明：kg-sync 镜像 {@link KgKnowledgePointPo} 无 subject 列（学科在 KgTextbookPo），
 * 且 label 接地已由 Python 侧经 mastery_snapshot 完成（复用已知知识点名，降低噪声），
 * 故此处按 label 在镜像中解析即可——数学答疑场景下同名知识点歧义可接受，未命中记日志待收录。
 */
@Slf4j
@Service
public class TutoringKpResolverImpl implements TutoringKpResolver {

    @Resource
    private KgKnowledgePointMapper kgKnowledgePointMapper;

    @Override
    public String resolveLabelToUri(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        // ① 精确匹配
        KgKnowledgePointPo po = kgKnowledgePointMapper.selectByLabel(label);
        if (po != null && po.getUri() != null) {
            return po.getUri();
        }
        // ② LIKE 模糊匹配
        po = kgKnowledgePointMapper.selectByLabelLike(label);
        if (po != null && po.getUri() != null) {
            log.debug("知识点 label 模糊命中: {} -> {}", label, po.getUri());
            return po.getUri();
        }
        log.warn("知识点 label 未命中 kg 镜像（待收录，不点亮）: {}", label);
        return null;
    }
}
