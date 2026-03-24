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
    public static final String CODE_INVALID = "20005";
    public static final String CODE_EXPIRED = "20006";
    public static final String CODE_TOO_FREQUENT = "20007";
    public static final String PHONE_NOT_REGISTERED = "20008";
    public static final String PHONE_ALREADY_REGISTERED = "20009";
    public static final String PASSWORD_SAME_AS_OLD = "20010";
    public static final String OLD_PASSWORD_WRONG = "20011";

    // 题库模块 3xxxx
    public static final String QUESTION_NOT_FOUND = "30001";
    public static final String KNOWLEDGE_POINT_NOT_FOUND = "30002";

    // 作业模块 4xxxx
    public static final String HOMEWORK_NOT_FOUND = "40001";
    public static final String HOMEWORK_ALREADY_SUBMITTED = "40002";

    // 学习模块 5xxxx
    public static final String ERROR_RECORD_NOT_FOUND = "50001";

    // LLM模块 6xxxx
    public static final String LLM_SERVICE_UNAVAILABLE = "60001";
    public static final String LLM_MODEL_NOT_ALLOWED = "60002";
    public static final String LLM_CALL_FAILED = "60003";
    public static final String LLM_TIMEOUT = "60004";
    public static final String LLM_INVALID_PARAMS = "60005";
}