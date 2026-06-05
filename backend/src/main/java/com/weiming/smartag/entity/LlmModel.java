package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM 模型配置
 */
@Data
@TableName("llm_model")
public class LlmModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模型唯一标识，如 deepseek-chat / doubao-pro-32k
     */
    private String modelKey;

    /**
     * 下拉框显示的中文名称
     */
    private String label;

    /**
     * 服务商类型：deepseek / doubao / volc-ark / ark
     */
    private String provider;

    /**
     * 模型服务商的 API Key
     */
    private String apiKey;

    /**
     * 接口域名和版本前缀，不含 /chat/completions
     */
    private String baseUrl;

    /**
     * 调用 /chat/completions 时的 model 字段
     */
    private String modelId;

    /**
     * 是否可用：0-不可用，1-可用
     */
    private Integer available;

    /**
     * 排序，越小越靠前
     */
    private Integer sortOrder;

    /**
     * 备注说明
     */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
