package com.bjutzxq.server.aspect;

import com.bjutzxq.common.Result;
import com.bjutzxq.server.context.UserIdContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 统一日志切面
 * <p>
 * 自动记录所有 Controller 请求的结构化日志，包含：
 * - 请求方法、URL、参数、用户 ID
 * - 响应状态码、耗时
 * - 异常信息
 * <p>
 * 日志格式为单行 JSON，便于日志分析和检索。
 * 敏感参数（密码、token 等）自动掩码。
 * <p>
 * 使用 @Order(0) 确保在 PermissionAspect(@Order(1)) 之前执行，
 * 从而能捕获权限不足的请求。
 */
@Slf4j
@Aspect
@Component
@Order(0)
public class LogAspect {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
        "password", "oldPassword", "newPassword", "confirmPassword",
        "token", "auth_token", "authorization", "refreshToken"
    ));
    private static final int MAX_PARAM_LENGTH = 500;

    /**
     * 环绕通知：拦截所有 Controller 方法
     */
    @Around("execution(* com.bjutzxq.server.controller..*.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求属性
        ServletRequestAttributes attrs = (ServletRequestAttributes)
            RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attrs.getRequest();
        String httpMethod = request.getMethod();
        String requestUri = request.getRequestURI();

        // 请求阶段日志（仅 INFO 级别开启时记录）
        if (log.isInfoEnabled()) {
            String queryString = request.getQueryString();
            Integer userId = UserIdContext.getCurrentUserId();
            Map<String, String> params = extractParams(request, joinPoint);
            log.info(buildLogJson("REQ", httpMethod, requestUri, queryString, userId, params, null, null, null, null));
        }

        // 执行并计时
        long startNanos = System.nanoTime();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            Integer userId = UserIdContext.getCurrentUserId();
            log.error(buildLogJson("ERR", httpMethod, requestUri, null, userId, null, elapsedMs, null, null, t));
            throw t;
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        Integer userId = UserIdContext.getCurrentUserId();

        // 响应阶段日志
        if (result instanceof Result) {
            Result<?> r = (Result<?>) result;
            Integer code = r.getCode();
            String message = r.getMessage();

            if (code == null || code == 200) {
                if (log.isInfoEnabled()) {
                    log.info(buildLogJson("RSP", httpMethod, requestUri, null, userId, null, elapsedMs, code, message, null));
                }
            } else {
                log.warn(buildLogJson("RSP", httpMethod, requestUri, null, userId, null, elapsedMs, code, message, null));
            }
        } else if (result instanceof ResponseEntity) {
            ResponseEntity<?> re = (ResponseEntity<?>) result;
            if (log.isInfoEnabled()) {
                log.info(buildLogJson("RSP", httpMethod, requestUri, null, userId, null, elapsedMs,
                    re.getStatusCode().value(), "ResponseEntity", null));
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info(buildLogJson("RSP", httpMethod, requestUri, null, userId, null, elapsedMs, null, null, null));
            }
        }

        return result;
    }

    /**
     * 提取请求参数（路径变量 + 查询参数 + @RequestBody）
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> extractParams(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        Map<String, String> params = new HashMap<>();

        // 1. 路径变量
        Object pathVars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVars instanceof Map) {
            ((Map<String, String>) pathVars).forEach((k, v) -> params.put(k, maskIfSensitive(k, v)));
        }

        // 2. 查询参数
        Map<String, String[]> paramMap = request.getParameterMap();
        if (paramMap != null && !paramMap.isEmpty()) {
            paramMap.forEach((k, v) -> {
                String value = (v != null && v.length > 0) ? v[0] : "";
                params.put(k, maskIfSensitive(k, truncate(value)));
            });
        }

        // 3. @RequestBody（从 joinPoint 参数中提取）
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Annotation[][] paramAnnotations = method.getParameterAnnotations();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < args.length; i++) {
                if (hasAnnotation(paramAnnotations[i], RequestBody.class) && args[i] != null) {
                    String bodyJson = objectMapper.writeValueAsString(args[i]);
                    params.put("@body", truncate(bodyJson));
                    break;
                }
            }
        } catch (Exception e) {
            log.trace("提取 @RequestBody 失败: {}", e.getMessage());
        }

        return params.isEmpty() ? null : params;
    }

    /**
     * 构建 JSON 日志字符串
     */
    private String buildLogJson(String type, String method, String path, String queryString,
                                 Integer userId, Map<String, String> params, Long durationMs,
                                 Integer code, String message, Throwable error) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("t", type);
            map.put("m", method);
            map.put("p", path);
            if (queryString != null && !queryString.isEmpty()) {
                map.put("q", queryString);
            }
            map.put("u", userId);
            if (params != null && !params.isEmpty()) {
                map.put("params", params);
            }
            if (durationMs != null) {
                map.put("d", durationMs);
            }
            if (code != null) {
                map.put("c", code);
            }
            if (message != null && !message.isEmpty()) {
                map.put("msg", message);
            }
            if (error != null) {
                map.put("e", error.getClass().getSimpleName() + ": " + error.getMessage());
            }
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("构建日志 JSON 失败", e);
            return "{\"t\":\"" + type + "\",\"p\":\"" + path + "\"}";
        }
    }

    /**
     * 检查参数注解数组是否包含指定注解
     */
    private boolean hasAnnotation(Annotation[] annotations, Class<? extends Annotation> annotationClass) {
        if (annotations == null) return false;
        for (Annotation a : annotations) {
            if (a.annotationType() == annotationClass) {
                return true;
            }
        }
        return false;
    }

    /**
     * 敏感字段掩码
     */
    private static String maskIfSensitive(String key, String value) {
        if (value == null) return null;
        if (SENSITIVE_KEYS.contains(key.toLowerCase())) {
            return "****";
        }
        return value;
    }

    /**
     * 截断过长参数值
     */
    private static String truncate(String value) {
        if (value == null) return null;
        if (value.length() > MAX_PARAM_LENGTH) {
            return value.substring(0, MAX_PARAM_LENGTH) + "...";
        }
        return value;
    }
}
