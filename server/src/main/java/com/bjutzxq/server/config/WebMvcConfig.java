package com.bjutzxq.server.config;

import com.bjutzxq.server.interceptor.JwtUserIdInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Arrays;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    /**
     * 创建 JWT 用户ID提取拦截器 Bean
     */
    @Bean
    public JwtUserIdInterceptor jwtUserIdInterceptor() {
        return new JwtUserIdInterceptor();
    }
    
    /**
     * 配置跨域过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的域名
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "https://bjut-zxq.cn"
        ));
        
        // 允许的方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 允许的请求头
        config.setAllowedHeaders(Arrays.asList("*"));
        
        // 允许携带凭证
        config.setAllowCredentials(true);
        
        // 预检请求缓存时间
        config.setMaxAge(3600L);
        
        // 应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
    
    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 用户ID提取拦截器：从 JWT Token 提取用户ID并存入 ThreadLocal
        registry.addInterceptor(jwtUserIdInterceptor())
                .addPathPatterns("/**")  // 拦截所有请求（context-path 会自动处理）
                .excludePathPatterns(
                        "/auth/login",      // 排除登录接口（无需 Token）
                        "/auth/register",   // 排除注册接口（无需 Token）
                        "/auth/captcha"   // 排除验证码接口（无需 Token）
                );
    }
}
