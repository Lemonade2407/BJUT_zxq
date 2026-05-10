package com.bjutzxq.server.handler;

import com.bjutzxq.common.BusinessException;
import com.bjutzxq.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 JWT 认证异常（Token 过期、无效、签名错误等）
     * @param e 异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler({
        io.jsonwebtoken.ExpiredJwtException.class,
        io.jsonwebtoken.MalformedJwtException.class,
        io.jsonwebtoken.security.SignatureException.class,
        io.jsonwebtoken.UnsupportedJwtException.class
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleJwtException(Exception e) {
        log.warn("JWT 认证异常：{}", e.getClass().getSimpleName());
        return Result.error(401, "未授权访问，请重新登录");
    }
    
    /**
     * 处理参数验证异常（@Valid 注解触发）
     * @param e 异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("参数验证失败：{}", errorMessage);
        return Result.error(400, "参数验证失败：" + errorMessage);
    }
    
    /**
     * 处理绑定异常
     * @param e 异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String errorMessage = e.getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("参数绑定失败：{}", errorMessage);
        return Result.error(400, "参数绑定失败：" + errorMessage);
    }
    
    /**
     * 处理非法参数异常
     * @param e 异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数：{}", e.getMessage());
        return Result.error(400, e.getMessage());
    }
    
    /**
     * 处理业务异常（可控制 HTTP 状态码）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletResponse response) throws IOException {
        log.warn("业务异常 ({}): {}", e.getCode(), e.getMessage());
        response.sendError(e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理运行时异常（未预期的业务逻辑异常）
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常：{}", e.getMessage(), e);
        return Result.error(500, "操作失败：" + e.getMessage());
    }
    
    /**
     * 处理其他所有异常（兜底处理）
     * @param e 异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        // 生产环境不返回详细错误信息，避免泄露系统细节
        log.error("服务器内部错误：{}", e.getMessage(), e);
        return Result.error(500, "服务器内部错误，请联系管理员");
    }
}
