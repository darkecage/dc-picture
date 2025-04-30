package com.darkecage.dcpicturebackend.common;

import com.darkecage.dcpicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * @title: 全局响应封装类
 * @author: darkecage
 * @date: 2025/4/30 3:19
 */
@Data
public class BaseResponse<T> implements Serializable {
    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode, T data) {
        this(errorCode.getCode(), data, errorCode.getMessage());
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
