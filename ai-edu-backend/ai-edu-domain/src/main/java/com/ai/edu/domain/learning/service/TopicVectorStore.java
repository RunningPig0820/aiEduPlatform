package com.ai.edu.domain.learning.service;

import com.ai.edu.domain.learning.model.contract.TopicVectorNeighbor;
import com.ai.edu.domain.learning.model.contract.TopicVectorPutRequest;

import java.util.Optional;

/**
 * 题型名向量存储端口（Java → Python 向量端点，tasks 2.3.1）。
 *
 * <p>本期只存<b>题型名向量</b>（vector_type 恒 "topic"，路由键在桥实现内部收口）；
 * Java 不碰 embedding API / COS SDK。端点失败抛 {@code TutoringAgentException}，
 * 由聚集编排降级（回退字符规则 + 原样落库，不阻塞主链路）。
 */
public interface TopicVectorStore {

    /**
     * 存题型名向量（幂等 upsert，key 相同覆盖），返回落库 key。
     *
     * <p>失败语义：端点异常 → 抛异常，调用方降级。
     */
    String putVector(TopicVectorPutRequest request);

    /**
     * 查题型名最近邻 Top-1（cosine distance，越小越相似）。
     *
     * <p>空 = 无近邻（库空 / put 后 ~10s 异步未生效——首题建锚无需立查）；
     * 端点异常 → 抛异常，调用方降级建新 canonical。
     */
    Optional<TopicVectorNeighbor> queryNearestTop1(String text);
}
