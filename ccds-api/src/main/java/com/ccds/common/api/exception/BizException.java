package com.ccds.common.api.exception;

import lombok.Getter;

/**
 * 业务异常。对外转错误码，禁止携带口令等敏感原文。
 *
 * @author ccds
 * @since 0.1.0
 */
@Getter
public class BizException extends RuntimeException {

    /**
     * 对外错误码。
     */
    private final String code;

    /**
     * 构造业务异常。
     *
     * @param code    错误码
     * @param message 给现场看的摘要
     */
    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造带原因的业务异常。
     *
     * @param code    错误码
     * @param message 给现场看的摘要
     * @param cause   原因
     */
    public BizException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
