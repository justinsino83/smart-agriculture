package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.LlmModel;
import com.weiming.smartag.service.LlmModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "LLM 模型配置", description = "设备预警详情与智能分析 - 多模型切换")
public class LlmModelController {

    private final LlmModelService llmModelService;

    // ============================================================
    // 前端页面下拉框使用的接口（无需鉴权，文档定义）
    // ============================================================
    @GetMapping("/models")
    @Operation(summary = "获取模型列表（下拉框）", description = "返回可用的LLM模型，供智能分析页面使用")
    public Result<?> getModels() {
        try {
            return Result.success(llmModelService.getActiveModels());
        } catch (Exception e) {
            log.error("获取LLM模型列表失败", e);
            return Result.fail("获取模型列表失败: " + e.getMessage());
        }
    }

    // ============================================================
    // 管理页面接口（CRUD）
    // ============================================================
    @GetMapping("/admin/models")
    @Operation(summary = "分页查询模型", description = "管理页面使用，支持按关键词和服务商筛选")
    public Result<Map<String, Object>> listModels(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词（modelKey/label）") @RequestParam(required = false) String keyword,
            @Parameter(description = "服务商类型") @RequestParam(required = false) String provider) {
        try {
            return Result.success(llmModelService.getModelListPage(page, size, keyword, provider));
        } catch (Exception e) {
            log.error("分页查询LLM模型失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/admin/models/{id}")
    @Operation(summary = "获取模型详情")
    public Result<LlmModel> getModelDetail(@PathVariable Long id) {
        try {
            return Result.success(llmModelService.getModelDetail(id));
        } catch (Exception e) {
            log.error("获取模型详情失败, id: {}", id, e);
            return Result.fail("获取详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/admin/models")
    @Operation(summary = "新增模型")
    public Result<?> addModel(@RequestBody LlmModel model) {
        try {
            if (!StringUtils.hasText(model.getModelKey())
                    || !StringUtils.hasText(model.getLabel())
                    || !StringUtils.hasText(model.getProvider())
                    || !StringUtils.hasText(model.getApiKey())) {
                return Result.fail("modelKey/label/provider/apiKey 均不能为空");
            }
            boolean ok = llmModelService.addModel(model);
            return ok ? Result.success() : Result.fail("新增失败");
        } catch (Exception e) {
            log.error("新增LLM模型失败", e);
            return Result.fail("新增失败: " + e.getMessage());
        }
    }

    @PutMapping("/admin/models/{id}")
    @Operation(summary = "更新模型")
    public Result<?> updateModel(@PathVariable Long id, @RequestBody LlmModel model) {
        try {
            model.setId(id);
            boolean ok = llmModelService.updateModel(model);
            return ok ? Result.success() : Result.fail("更新失败");
        } catch (Exception e) {
            log.error("更新LLM模型失败, id: {}", id, e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/admin/models/{id}")
    @Operation(summary = "删除模型")
    public Result<?> deleteModel(@PathVariable Long id) {
        try {
            boolean ok = llmModelService.deleteModel(id);
            return ok ? Result.success() : Result.fail("删除失败");
        } catch (Exception e) {
            log.error("删除LLM模型失败, id: {}", id, e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }

    @PutMapping("/admin/models/{id}/available")
    @Operation(summary = "切换可用状态")
    public Result<?> toggleAvailable(@PathVariable Long id) {
        try {
            boolean ok = llmModelService.toggleAvailable(id);
            return ok ? Result.success() : Result.fail("切换失败");
        } catch (Exception e) {
            log.error("切换LLM模型可用状态失败, id: {}", id, e);
            return Result.fail("切换失败: " + e.getMessage());
        }
    }
}
