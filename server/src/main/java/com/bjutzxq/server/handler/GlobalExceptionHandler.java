package com.bjutzxq.server.handler;

import com.bjutzxq.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理运行时异常
     * @param e 异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.warn("运行时异常：{}", e.getMessage());
        return Result.error(500, e.getMessage());
    }
    
    /**
     * 处理 IllegalArgumentException（非法参数异常）
     * @param e 异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数异常：{}", e.getMessage());
        return Result.error(400, e.getMessage());
    }
    
    /**
     * 处理其他所有异常
     * @param e 异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        // TODO: 生产环境不应返回详细错误信息，避免泄露系统细节
        log.error("服务器内部错误：{}", e.getMessage(), e);
        return Result.error(500, "服务器内部错误：" + e.getMessage());
    }
}
