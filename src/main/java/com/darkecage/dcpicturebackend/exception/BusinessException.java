package com.darkecage.dcpicturebackend.exception;

import lombok.Getter;
import org.springframework.remoting.RemoteTimeoutException;

@Getter
public class BusinessException extends RuntimeException {
    //错误码
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
