package com.clinic.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h2>统一响应结果封装类</h2>
 *
 * <p>用于标准化 API 接口的返回格式，所有控制器层方法均通过此对象
 * 包装返回数据，确保前端能统一解析处理。</p>
 *
 * <h3>响应格式</h3>
 * <pre>{@code
 * {
 *   "code": 200,        // 状态码，200 表示成功
 *   "message": "success", // 提示信息
 *   "data": { ... }      // 响应数据体，可为空
 * }
 * }</pre>
 *
 * <h3>典型用法</h3>
 * <pre>{@code
 *   // 成功返回数据
 *   return Result.ok(userList);
 *
 *   // 成功但不返回数据
 *   return Result.ok();
 *
 *   // 业务异常
 *   return Result.error(400, "参数校验失败");
 *
 *   // 服务端异常
 *   return Result.error("系统繁忙，请稍后重试");
 * }</pre>
 *
 * @param <T> 响应数据的类型，支持任意 Java 对象
 * @author clinic
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * HTTP 状态码
     * <p>200 表示请求成功，400 表示参数错误，500 表示服务端异常</p>
     */
    private int code;

    /**
     * 响应提示消息
     * <p>成功时通常为 "success"，失败时包含具体错误原因</p>
     */
    private String message;

    /**
     * 响应数据体
     * <p>可为任意类型的业务数据，无数据时设为 {@code null}</p>
     */
    private T data;

    /**
     * 构建成功响应（含数据）
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 状态码 200、消息 "success"、携带数据的 Result 对象
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 构建成功响应（无数据）
     *
     * <p>适用于仅需告知操作成功、无需返回额外数据的场景
     * （如删除操作、更新操作等）</p>
     *
     * @param <T> 数据类型（无实际意义）
     * @return 状态码 200、消息 "success"、数据为 null 的 Result 对象
     */
    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    /**
     * 构建错误响应（指定状态码和错误消息）
     *
     * <p>用于返回可自定义状态码的业务异常，
     * 如 400（参数错误）、401（未授权）、403（无权限）等</p>
     *
     * @param code    自定义错误状态码
     * @param message 错误提示信息
     * @param <T>     数据类型（无实际意义）
     * @return 包含错误码和错误消息的 Result 对象
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 构建服务端错误响应（默认 500 状态码）
     *
     * <p>用于返回服务端内部异常，状态码固定为 500。
     * 适用于捕获未预料的运行时异常等场景。</p>
     *
     * @param message 错误提示信息
     * @param <T>     数据类型（无实际意义）
     * @return 状态码 500、携带错误消息的 Result 对象
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
