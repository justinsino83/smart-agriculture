package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.Facility;
import com.weiming.smartag.service.FacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/facility")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "设施管理", description = "设施管理相关接口")
public class FacilityController {

    private final FacilityService facilityService;

    @GetMapping("/list")
    @Operation(summary = "获取设施列表", description = "分页获取所有设施")
    public Result<Map<String, Object>> getFacilityList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "设施类型：1-试验田，2-仓库，3-烘干车间") @RequestParam(required = false) Integer type) {
        try {
            Page<Facility> pageParam = new Page<>(page, size);
            LambdaQueryWrapper<Facility> wrapper = new LambdaQueryWrapper<>();
            if (type != null) {
                wrapper.eq(Facility::getType, type);
            }
            wrapper.orderByAsc(Facility::getId);
            
            Page<Facility> result = facilityService.page(pageParam, wrapper);
            
            Map<String, Object> data = new HashMap<>();
            data.put("list", result.getRecords());
            data.put("total", result.getTotal());
            data.put("page", result.getCurrent());
            data.put("size", result.getSize());
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取设施列表失败", e);
            return Result.fail("获取设施列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有设施", description = "获取所有设施列表（不分页）")
    public Result<List<Facility>> getAllFacilities(
            @Parameter(description = "设施类型：1-试验田，2-仓库，3-烘干车间") @RequestParam(required = false) Integer type) {
        try {
            LambdaQueryWrapper<Facility> wrapper = new LambdaQueryWrapper<>();
            if (type != null) {
                wrapper.eq(Facility::getType, type);
            }
            wrapper.orderByAsc(Facility::getId);
            return Result.success(facilityService.list(wrapper));
        } catch (Exception e) {
            log.error("获取所有设施失败", e);
            return Result.fail("获取所有设施失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取设施详情", description = "根据ID获取设施详情")
    public Result<Facility> getFacilityById(@PathVariable Long id) {
        try {
            Facility facility = facilityService.getById(id);
            if (facility == null) {
                return Result.fail("设施不存在");
            }
            return Result.success(facility);
        } catch (Exception e) {
            log.error("获取设施详情失败", e);
            return Result.fail("获取设施详情失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "新增设施", description = "创建新设施")
    public Result<Boolean> addFacility(@RequestBody Facility facility) {
        try {
            boolean success = facilityService.save(facility);
            return Result.success(success);
        } catch (Exception e) {
            log.error("新增设施失败", e);
            return Result.fail("新增设施失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新设施", description = "更新设施信息")
    public Result<Boolean> updateFacility(@PathVariable Long id, @RequestBody Facility facility) {
        try {
            facility.setId(id);
            boolean success = facilityService.updateById(facility);
            return Result.success(success);
        } catch (Exception e) {
            log.error("更新设施失败", e);
            return Result.fail("更新设施失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除设施", description = "删除指定设施")
    public Result<Boolean> deleteFacility(@PathVariable Long id) {
        try {
            boolean success = facilityService.removeById(id);
            return Result.success(success);
        } catch (Exception e) {
            log.error("删除设施失败", e);
            return Result.fail("删除设施失败: " + e.getMessage());
        }
    }
}
