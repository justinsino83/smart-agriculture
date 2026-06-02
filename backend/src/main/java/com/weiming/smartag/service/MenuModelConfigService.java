package com.weiming.smartag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 菜单和模型配置服务 - 支持动态读取
 */
@Slf4j
@Service
public class MenuModelConfigService {

    private final ObjectMapper objectMapper;

    // 外部配置文件路径（可选，部署时可通过参数指定）
    @Value("${smartag.config.menu-model-path:}")
    private String externalConfigPath;

    // 内部资源文件路径
    private static final String INTERNAL_CONFIG_PATH = "menu-model-config.json";

    public MenuModelConfigService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 动态获取菜单和模型配置
     * 每次调用都会重新读取文件，支持热更新
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfig() {
        try {
            // 1. 优先尝试读取外部配置文件
            if (externalConfigPath != null && !externalConfigPath.isBlank()) {
                File externalFile = new File(externalConfigPath);
                if (externalFile.exists() && externalFile.isFile()) {
                    log.info("读取外部配置文件: {}", externalConfigPath);
                    return objectMapper.readValue(externalFile, Map.class);
                } else {
                    log.warn("外部配置文件不存在: {}, 将使用内部配置", externalConfigPath);
                }
            }

            // 2. 读取内部资源文件
            log.info("读取内部配置文件: {}", INTERNAL_CONFIG_PATH);
            try (InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream(INTERNAL_CONFIG_PATH)) {
                if (inputStream == null) {
                    log.error("内部配置文件不存在: {}", INTERNAL_CONFIG_PATH);
                    return getDefaultConfig();
                }
                return objectMapper.readValue(inputStream, Map.class);
            }
        } catch (IOException e) {
            log.error("读取配置文件失败", e);
            return getDefaultConfig();
        }
    }

    /**
     * 获取默认配置（失败时的降级方案）
     */
    private Map<String, Object> getDefaultConfig() {
        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("menus", new java.util.ArrayList<>());
        defaultConfig.put("gltfs", new java.util.ArrayList<>());
        return defaultConfig;
    }
}
