package com.ccds.common.api.vo;

import com.ccds.common.api.constant.ErrorCodeConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 HTTP 响应。
 *
 * @param <T> 业务数据类型
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResultVO<T> {

    /**
     * 错误码，成功为 0。
     */
    private String code;

    /**
     * 给现场看的摘要，不含敏感原文。
     */
    private String message;

    /**
     * 业务数据，失败时为 null。
     */
    private T data;

    /**
     * 成功响应。
     *
     * @param data 业务数据，允许为 null
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> ApiResultVO<T> ok(T data) {
        return ApiResultVO.<T>builder()
                .code(ErrorCodeConstant.SUCCESS)
                .message("OK")
                .data(data)
                .build();
    }

    /**
     * 失败响应。
     *
     * @param code    错误码
     * @param message 摘要
     * @param <T>     数据类型
     * @return 失败结果
     */
    public static <T> ApiResultVO<T> fail(String code, String message) {
        return ApiResultVO.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
