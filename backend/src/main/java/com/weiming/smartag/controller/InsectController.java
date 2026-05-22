package com.weiming.smartag.controller;

import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.InsectData;
import com.weiming.smartag.entity.InsectDevice;
import com.weiming.smartag.service.InsectService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 虫情数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/insect")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "虫情数据", description = "虫情监测数据管理")
public class InsectController {

    private final InsectService insectService;

    /**
     * 手动触发同步所有设备数据
     */
    @PostMapping("/sync")
    @Operation(summary = "手动同步虫情数据", description = "触发一次完整的设备数据同步")
    public Result<String> syncAllData() {
        try {
            insectService.syncAllDevicesData();
            log.info("虫情数据同步完成");
            return Result.success("同步完成");
        } catch (Exception e) {
            log.error("虫情数据同步失败", e);
            return Result.fail("同步失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备列表
     */
    @GetMapping("/devices")
    @Operation(summary = "获取虫情设备列表")
    public Result<List<InsectDevice>> getDevices() {
        try {
            List<InsectDevice> devices = insectService.getDeviceList();
            return Result.success(devices);
        } catch (Exception e) {
            log.error("获取虫情设备列表失败", e);
            return Result.fail("获取设备列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取本地保存的设备列表
     */
    @GetMapping("/devices/local")
    @Operation(summary = "获取本地虫情设备列表")
    public Result<IPage<InsectDevice>> getLocalDevices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // 参数校验
            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 20;
            
            return Result.success(insectService.getLocalDevicePage(page, size));
        } catch (Exception e) {
            log.error("获取本地虫情设备列表失败, page: {}, size: {}", page, size, e);
            return Result.fail("获取设备列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取虫情数据列表（本地数据库）
     */
    @GetMapping("/data/list")
    @Operation(summary = "获取虫情数据列表")
    public Result<IPage<InsectData>> getDataList(
            @RequestParam(required = false) String imei,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // 参数校验
            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 20;
            
            return Result.success(insectService.getLocalDataPage(imei, startDate, endDate, page, size));
        } catch (Exception e) {
            log.error("获取虫情数据列表失败, imei: {}, startDate: {}, endDate: {}, page: {}, size: {}", imei, startDate, endDate, page, size, e);
            return Result.fail("获取数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取虫情统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取虫情统计", description = "从外部API获取指定设备的虫情统计")
    public Result<List<Map<String, Object>>> getStatistics(
            @RequestParam String imei,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            if (!StringUtils.hasText(imei)) {
                return Result.fail("设备IMEI不能为空");
            }
            
            // 默认查询最近7天
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(7).toString();
            }
            if (endDate == null) {
                endDate = LocalDate.now().toString();
            }

            List<Map<String, Object>> statistics = insectService.getInsectStatistic(imei, startDate, endDate);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取虫情统计失败, imei: {}, startDate: {}, endDate: {}", imei, startDate, endDate, e);
            return Result.fail("获取统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取最新虫情数据(从外部API)
     */
    @GetMapping("/latest")
    @Operation(summary = "获取最新虫情数据")
    public Result<List<InsectData>> getLatestData(
            @RequestParam String imei,
            @RequestParam(required = false) Integer hours) {
        try {
            if (!StringUtils.hasText(imei)) {
                return Result.fail("设备IMEI不能为空");
            }
            
            if (hours == null) hours = 24;
            if (hours < 1 || hours > 720) hours = 24;
            
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(hours);

            List<InsectData> dataList = insectService.getInsectImagesByTimeRange(imei, startTime, endTime);
            return Result.success(dataList);
        } catch (Exception e) {
            log.error("获取最新虫情数据失败, imei: {}, hours: {}", imei, hours, e);
            return Result.fail("获取数据失败: " + e.getMessage());
        }
    }
}