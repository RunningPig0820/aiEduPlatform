/**
 * 调度触发包（技术触发壳，与业务分离）。
 *
 * <p>本包只放 {@code @Scheduled} 触发方法——「何时执行」的技术关注点，
 * 业务逻辑在 {@code application.service.batch}（「做什么」）。二者分离：
 * 业务服务可独立单测、手动调用、未来整体迁移大数据平台；调度壳是过渡实现，
 * 迁大数据时删除本包即可。
 *
 * <p>当前单机部署用 Spring {@code @Scheduled} 足够；引入 XXL-Job 的触发条件是
 * 多实例部署（怕重复执行）或运营需后台动态改调度。
 */
package com.ai.edu.application.service.scheduler;
