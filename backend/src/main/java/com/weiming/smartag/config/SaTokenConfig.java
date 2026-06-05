package com.weiming.smartag.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.weiming.smartag.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token权限配置
 */
@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 不需要认证的接口路径
     */
    private static final String[] EXCLUDE_PATHS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/captcha",
            "/ws/**",
            "/api/dashboard/**",
            "/api/energy/**",
            "/api/weather/**",
            "/api/soil/**",
            "/api/irrigation/**",
            "/api/insect/**",
            "/api/device/**",
            "/api/drying/**",
            "/api/storage/**",
            "/api/testfield/**",
            "/api/overview",
            "/api/llm/**",
            "/error"
    };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 跳过 OPTIONS 请求
            SaRequest request = SaHolder.getRequest();
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                return;
            }
            StpUtil.checkLogin();
        }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }

    @Bean
    public SaServletFilter saServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .setAuth(obj -> {
                })
                .setError(e -> {
                    log.error("Sa-Token认证失败: {}", e.getMessage());
                    SaHolder.getResponse().setStatus(401);
                    SaHolder.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");
                    return JSON.toJSONString(Result.error(401, "未登录或登录已过期"));
                });
    }
}
