package com.ai.edu.application.service.scheduler;

import com.ai.edu.application.service.batch.KpMaintenanceService;
import com.ai.edu.application.service.batch.KpQuestionTypeAggregationService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 知识点派生层调度触发（技术触发壳）。
 *
 * <p>调度与业务分离（DDD）：本类只负责「何时触发」，业务逻辑在 {@code application.service.batch}。
 * 未来迁大数据平台时，删除本类（连同 batch 包）即可，数据表与在线解析管线②保持不变。
 *
 * <p>当前单机部署，用 Spring {@code @Scheduled} 过渡即可（无需 XXL-Job；多实例/动态调度再评估）。
 */
@Component
public class KpBatchScheduler {

    @Resource
    private KpQuestionTypeAggregationService aggregationService;
    @Resource
    private KpMaintenanceService maintenanceService;

    /** 题型库聚合：凌晨 3:17 触发（避开整点）。 */
    @Scheduled(cron = "0 17 3 * * ?")
    public void aggregate() {
        aggregationService.aggregate();
    }

    /** 维护闭环（共现转正 → 冲突重判 → 统计回流）：凌晨 3:37 触发（避开聚合 3:17）。 */
    @Scheduled(cron = "0 37 3 * * ?")
    public void maintain() {
        maintenanceService.maintain();
    }
}
