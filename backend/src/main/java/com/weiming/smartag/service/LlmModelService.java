package com.weiming.smartag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weiming.smartag.entity.LlmModel;

import java.util.List;
import java.util.Map;

/**
 * LLM 模型配置服务
 */
public interface LlmModelService extends IService<LlmModel> {

    /**
     * 获取所有可用的模型（供前端页面下拉框使用）
     */
    List<Map<String, Object>> getActiveModels();

    /**
     * 分页获取模型列表（管理页面）
     */
    Map<String, Object> getModelListPage(int page, int size, String keyword, String provider);

    /**
     * 获取模型详情
     */
    LlmModel getModelDetail(Long id);

    /**
     * 新增模型
     */
    boolean addModel(LlmModel model);

    /**
     * 更新模型
     */
    boolean updateModel(LlmModel model);

    /**
     * 删除模型
     */
    boolean deleteModel(Long id);

    /**
     * 切换模型可用状态
     */
    boolean toggleAvailable(Long id);
}
