package com.clinic.common;

/**
 * 权限不足异常 —— 当前用户角色无权执行该操作。
 * 由 GlobalExceptionHandler 统一拦截，返回 HTTP 403。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
