package com.ai.edu.infrastructure.persistence.edukg.util;

/**
 * Entity 实例创建工具（绕过 protected 构造函数访问限制）
 */
public final class EntityFactory {

    private EntityFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> entityClass) {
        try {
            var ctor = entityClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create entity: " + entityClass.getName(), e);
        }
    }
}
