package com.ccds.common.web;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.common.api.vo.ApiResultVO;

import lombok.extern.slf4j.Slf4j;

/**
 * 将业务异常转为错误码，不向客户端暴露堆栈。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MSG_PARAM_INVALID = "参数不合法";

    private static final String MSG_SYSTEM_ERROR = "系统繁忙，请稍后重试";

    /**
     * 业务失败。
     *
     * @param ex 业务异常
     * @return 错误结果
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResultVO<Void>> handleBiz(BizException ex) {
        HttpStatus status = resolveStatus(ex.getCode());
        return ResponseEntity.status(status).body(ApiResultVO.fail(ex.getCode(), ex.getMessage()));
    }

    /**
     * 入参校验失败。
     *
     * @param ex 校验异常
     * @return 错误结果
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResultVO<Void>> handleValid(Exception ex) {
        log.warn("param invalid {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResultVO.fail(ErrorCodeConstant.PARAM_INVALID, MSG_PARAM_INVALID));
    }

    /**
     * 未捕获故障。
     *
     * @param ex 异常
     * @return 错误结果
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResultVO<Void>> handleOther(Exception ex) {
        log.error("system error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResultVO.fail(ErrorCodeConstant.SYSTEM_ERROR, MSG_SYSTEM_ERROR));
    }

    private HttpStatus resolveStatus(String code) {
        if (ErrorCodeConstant.AUTH_UNAUTHORIZED.equals(code)
                || ErrorCodeConstant.AUTH_REFRESH_INVALID.equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ErrorCodeConstant.AUTH_LOCKED.equals(code)
                || ErrorCodeConstant.AUTH_MUST_CHANGE_PASSWORD.equals(code)
                || ErrorCodeConstant.ROSTER_STATION_FORBIDDEN.equals(code)
                || ErrorCodeConstant.ROSTER_WRITE_FORBIDDEN.equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (ErrorCodeConstant.PARAM_INVALID.equals(code)
                || ErrorCodeConstant.AUTH_PASSWORD_INVALID.equals(code)
                || ErrorCodeConstant.AUTH_LOGIN_FAILED.equals(code)
                || ErrorCodeConstant.ROSTER_NAME_DUPLICATE.equals(code)
                || ErrorCodeConstant.ROSTER_NFC_DUPLICATE.equals(code)
                || ErrorCodeConstant.ROSTER_IMPORT_INVALID.equals(code)
                || ErrorCodeConstant.ROSTER_PROFILE_NOT_FOUND.equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
