package com.ai.edu.common.exception;

import com.ai.edu.common.constant.ErrorCode;

/**
 * 实体未找到异常
 */
public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String entityName, Object id) {
        super(ErrorCode.ENTITY_NOT_FOUND,
                String.format("%s not found with id: %s", entityName, id));
    }

    public EntityNotFoundException(String message) {
        super(ErrorCode.ENTITY_NOT_FOUND, message);
    }
}