package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weiming.smartag.entity.LlmModel;
import com.weiming.smartag.mapper.LlmModelMapper;
import com.weiming.smartag.service.LlmModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LlmModelServiceImpl extends ServiceImpl<LlmModelMapper, LlmModel> implements LlmModelService {

    @Override
    public List<Map<String, Object>> getActiveModels() {
        // 按排序字段排序，返回所有可用的模型
        List<LlmModel> models = lambdaQuery()
                .eq(LlmModel::getAvailable, 1)
                .orderByAsc(LlmModel::getSortOrder)
                .orderByDesc(LlmModel::getId)
                .list();

        // 转换为前端需要的字段名（按文档定义的字段）
        List<Map<String, Object>> result = new ArrayList<>();
        for (LlmModel model : models) {
            Map<String, Object> item = new HashMap<>();
            item.put("key", model.getModelKey());
            item.put("label", model.getLabel());
            item.put("provider", model.getProvider());
            item.put("apiKey", model.getApiKey());
            item.put("baseUrl", model.getBaseUrl());
            item.put("modelId", model.getModelId());
            item.put("available", model.getAvailable() == 1);
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getModelListPage(int page, int size, String keyword, String provider) {
        Map<String, Object> result = new HashMap<>();

        Page<LlmModel> pageObj = new Page<>(page, size);

        LambdaQueryWrapper<LlmModel> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(LlmModel::getModelKey, keyword)
                    .or()
                    .like(LlmModel::getLabel, keyword));
        }
        if (StringUtils.hasText(provider)) {
            wrapper.eq(LlmModel::getProvider, provider);
        }
        wrapper.orderByAsc(LlmModel::getSortOrder)
                .orderByDesc(LlmModel::getId);

        Page<LlmModel> pageResult = page(pageObj, wrapper);

        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());

        return result;
    }

    @Override
    public LlmModel getModelDetail(Long id) {
        return getById(id);
    }

    @Override
    public boolean addModel(LlmModel model) {
        // 校验 model_key 唯一
        Long existCount = lambdaQuery()
                .eq(LlmModel::getModelKey, model.getModelKey())
                .count();
        if (existCount > 0) {
            throw new RuntimeException("modelKey已存在：" + model.getModelKey());
        }

        if (model.getAvailable() == null) {
            model.setAvailable(1);
        }
        if (model.getSortOrder() == null) {
            model.setSortOrder(0);
        }
        model.setCreateTime(LocalDateTime.now());
        model.setUpdateTime(LocalDateTime.now());

        return save(model);
    }

    @Override
    public boolean updateModel(LlmModel model) {
        if (model.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        // 校验 model_key 唯一（排除自己）
        Long existCount = lambdaQuery()
                .eq(LlmModel::getModelKey, model.getModelKey())
                .ne(LlmModel::getId, model.getId())
                .count();
        if (existCount > 0) {
            throw new RuntimeException("modelKey已存在：" + model.getModelKey());
        }

        model.setUpdateTime(LocalDateTime.now());
        return updateById(model);
    }

    @Override
    public boolean deleteModel(Long id) {
        return removeById(id);
    }

    @Override
    public boolean toggleAvailable(Long id) {
        LlmModel model = getById(id);
        if (model == null) {
            throw new RuntimeException("模型不存在：id=" + id);
        }
        int newVal = (model.getAvailable() != null && model.getAvailable() == 1) ? 0 : 1;
        model.setAvailable(newVal);
        model.setUpdateTime(LocalDateTime.now());
        return updateById(model);
    }
}
