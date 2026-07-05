package com.clinic.common;

import com.clinic.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>全局异常处理切面</h2>
 *
 * <p>通过 Spring {@code @RestControllerAdvice} 注解实现全局异常拦截，
 * 统一处理各层抛出的异常，并以标准的 {@link Result} 对象返回给前端。</p>
 *
 * <h3>异常处理优先级</h3>
 * <p>Spring 按照异常类型从具体到宽泛进行匹配，匹配顺序为：</p>
 * <ol>
 *   <li>{@link IllegalArgumentException} — 参数非法异常，返回 400</li>
 *   <li>{@link RuntimeException} — 运行时异常，返回 500</li>
 *   <li>{@link Exception} — 兜底异常处理，返回 500</li>
 * </ol>
 *
 * <h3>工作原理</h3>
 * <ul>
 *   <li>当 Controller 抛出异常时，Spring 会查找当前类及全局的 {@code @ExceptionHandler}</li>
 *   <li>找到匹配的处理器方法后，调用该方法并将异常对象作为参数传入</li>
 *   <li>处理器返回的 Result 对象会被自动序列化为 JSON 响应</li>
 * </ul>
 *
 * @author clinic
 * @version 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数非法异常
     *
     * <p>当前端传入的请求参数不满足业务校验规则时，
     * 业务代码抛出 {@link IllegalArgumentException}，由本方法统一拦截。
     * 返回 HTTP 400 状态码，消息体包含具体的参数错误说明。</p>
     *
     * <h4>典型触发场景</h4>
     * <ul>
     *   <li>请求参数为空或格式不合法</li>
     *   <li>业务逻辑中主动抛出的参数校验异常</li>
     * </ul>
     *
     * @param e 包含具体错误信息的 IllegalArgumentException 对象
     * @return Result 包含 400 状态码和错误消息
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理运行时异常
     *
     * <p>拦截所有 {@link RuntimeException} 及其子类异常（除已被更具体的
     * Handler 捕获的外），如空指针异常、类型转换异常等。
     * 返回 HTTP 500 状态码，同时在服务端日志中记录完整异常堆栈。</p>
     *
     * @param e 运行时异常对象
     * @return Result 包含 500 状态码和异常消息
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntime(RuntimeException e) {
        // 记录完整的异常堆栈，便于排查问题
        log.error("系统异常", e);
        // 将异常消息直接返回给前端
        return Result.error(e.getMessage());
    }

    /**
     * 兜底异常处理——捕获所有未被上述 Handler 处理的异常
     *
     * <p>作为最后一道防线，捕获所有非 RuntimeException 的异常
     * （如 SQLException、IOException 等受检异常）。
     * 不向客户端暴露具体的错误细节，仅返回统一的"服务器内部错误"提示，
     * 以防止敏感信息泄露。完整的异常信息仅记录在服务端日志中。</p>
     *
     * @param e 任意类型的 Exception 对象
     * @return Result 包含 500 状态码和通用错误提示
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        // 记录完整异常堆栈用于排查
        log.error("未知异常", e);
        // 返回通用错误消息，不暴露内部细节
        return Result.error("服务器内部错误");
    }
}
