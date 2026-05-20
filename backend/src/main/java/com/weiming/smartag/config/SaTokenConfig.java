package com.weiming.smartag.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.weiming.smartag.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token权限配置
 */
@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
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
                        "/api/device/**"
                );
    }

    @Bean
    public SaServletFilter saServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .setAuth(obj -> {
                })
                .setError(e -> {
                    log.error("Sa-Token认证失败: {}", e.getMessage());
                    SaHolder.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");
                    return JSON.toJSONString(Result.error(401, "未登录或登录已过期"));
                });
    }
}
