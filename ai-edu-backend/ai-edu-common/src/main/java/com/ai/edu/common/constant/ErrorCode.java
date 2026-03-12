package com.ai.edu.common.constant;

/**
 * 错误码常量
 */
public final class ErrorCode {

    private ErrorCode() {}

    // 通用错误 1xxxx
    public static final String SUCCESS = "00000";
    public static final String SYSTEM_ERROR = "10000";
    public static final String PARAM_ERROR = "10001";
    public static final String ENTITY_NOT_FOUND = "10002";
    public static final String INVALID_PARAMS = "10003";
    public static final String UNAUTHORIZED = "10004";

    // 用户模块 2xxxx
    public static final String USER_NOT_FOUND = "20001";
    public static final String USER_ALREADY_EXISTS = "20002";
    public static final String INVALID_CREDENTIALS = "20003";
    public static final String PERMISSION_DENIED = "20004";

    // 题库模块 3xxxx
    public static final String QUESTION_NOT_FOUND = "30001";
    public static final String KNOWLEDGE_POINT_NOT_FOUND = "30002";

    // 作业模块 4xxxx
    public static final String HOMEWORK_NOT_FOUND = "40001";
    public static final String HOMEWORK_ALREADY_SUBMITTED = "40002";

    // 学习模块 5xxxx
    public static final String ERROR_RECORD_NOT_FOUND = "50001";
}