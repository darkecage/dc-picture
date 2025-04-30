package com.darkecage.dcpicturebackend.exception;

/**
 * @title: 异常处理工具类
 * @author: darkecage
 * @date: 2025/4/30 2:53
 */
public class ThrowUtils {
    /**
     * @title: 条件成立抛异常
     * @author: darkecage
     * @date: 2025/4/30 2:56
     * @param: condition
     * @param: runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * @title: 条件成立抛异常
     * @author: darkecage
     * @date: 2025/4/30 2:56
     * @param: condition
     * @param: errorCode
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * @title: 条件成立抛异常
     * @author: darkecage
     * @date: 2025/4/30 2:58
     * @param: condition
     * @param: errorCode
     * @param: massage
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String massage) {
        throwIf(condition, new BusinessException(errorCode, massage));
    }
}
