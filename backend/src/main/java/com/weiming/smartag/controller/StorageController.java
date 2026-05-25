package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.entity.Facility;
import com.weiming.smartag.entity.FacilityRealtimeData;
import com.weiming.smartag.entity.FacilityStatus;
import com.weiming.smartag.entity.StorageRecord;
import com.weiming.smartag.mapper.FacilityRealtimeDataMapper;
import com.weiming.smartag.mapper.FacilityStatusMapper;
import com.weiming.smartag.service.DevicePushService;
import com.weiming.smartag.service.FacilityService;
import com.weiming.smartag.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "仓库管理", description = "仓库数据管理接口")
public class StorageController {

    private final StorageService storageService;
    private final DevicePushService devicePushService;
    private final FacilityService facilityService;
    private final FacilityRealtimeDataMapper facilityRealtimeDataMapper;
    private final FacilityStatusMapper facilityStatusMapper;

    @GetMapping("/records")
    public Result<List<StorageRecord>> listRecords() {
        try {
            return Result.success(storageService.list());
        } catch (Exception e) {
            log.error("获取仓储记录失败", e);
            return Result.fail("获取仓储记录失败: " + e.getMessage());
        }
    }

    @GetMapping("/stock")
    @Operation(summary = "获取库存列表", description = "分页获取在库库存列表")
    public Result<Map<String, Object>> getStockList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        try {
            return Result.success(storageService.getStockListPage(page, size));
        } catch (Exception e) {
            log.error("获取在库列表失败", e);
            return Result.fail("获取在库列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/in")
    public Result<?> storageIn(@RequestBody StorageRecord record) {
        try {
            if (record == null) {
                return Result.fail("入库数据不能为空");
            }
            if (!StringUtils.hasText(record.getBatchNo())) {
                return Result.fail("批次号不能为空");
            }
            boolean success = storageService.storageIn(record);
            return success ? Result.success("入库成功") : Result.error("入库失败");
        } catch (Exception e) {
            log.error("入库失败, record: {}", record, e);
            return Result.fail("入库失败: " + e.getMessage());
        }
    }

    @PostMapping("/out/{batchNo}")
    public Result<?> storageOut(@PathVariable String batchNo) {
        try {
            if (!StringUtils.hasText(batchNo)) {
                return Result.fail("批次号不能为空");
            }
            boolean success = storageService.storageOut(batchNo);
            return success ? Result.success("出库成功") : Result.error("出库失败，批次号不存在");
        } catch (Exception e) {
            log.error("出库失败, batchNo: {}", batchNo, e);
            return Result.fail("出库失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        try {
            return Result.success(storageService.getStatistics());
        } catch (Exception e) {
            log.error("获取库存统计失败", e);
            return Result.fail("获取库存统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/overview")
    @Operation(summary = "获取仓储概览数据", description = "获取总库存、今日出入库、预警数量等概览数据")
    public Result<Map<String, Object>> getOverview() {
        try {
            return Result.success(storageService.getOverview());
        } catch (Exception e) {
            log.error("获取仓储概览失败", e);
            return Result.fail("获取仓储概览失败: " + e.getMessage());
        }
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getEntryExitTrend(
            @RequestParam(defaultValue = "7") int days) {
        try {
            if (days <= 0 || days > 365) {
                days = 7;
            }
            return Result.success(storageService.getEntryExitTrend(days));
        } catch (Exception e) {
            log.error("获取出入库趋势失败, days: {}", days, e);
            return Result.fail("获取出入库趋势失败: " + e.getMessage());
        }
    }

    @GetMapping("/sensors")
    @Operation(summary = "获取仓库传感器数据", description = "获取仓库的环境监测、设备状态、库存统计等综合数据")
    public Result<Map<String, Object>> getWarehouseSensors(
            @Parameter(description = "设施ID")
            @RequestParam(required = false) Long facilityId) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 获取设施信息
            Facility facility = null;
            if (facilityId != null) {
                facility = facilityService.getById(facilityId);
            }
            if (facility == null) {
                facility = facilityService.lambdaQuery()
                        .eq(Facility::getType, 2)
                        .last("LIMIT 1")
                        .one();
            }
            result.put("facility", facility);

            if (facility != null) {
                // 环境数据
                Map<String, Object> envData = devicePushService.getDashboardOverview(null);
                result.put("environment", envData.get("environment"));

                // 实时传感器数据 - 从数据库读取
                FacilityRealtimeData realtimeData = facilityRealtimeDataMapper.selectOne(
                        new LambdaQueryWrapper<FacilityRealtimeData>()
                                .eq(FacilityRealtimeData::getFacilityId, facility.getId())
                                .orderByDesc(FacilityRealtimeData::getCollectTime)
                                .last("LIMIT 1")
                );
                if (realtimeData != null) {
                    Map<String, Object> realtimeSensor = new HashMap<>();
                    realtimeSensor.put("innerTemperature", realtimeData.getInnerTemperature());
                    realtimeSensor.put("innerHumidity", realtimeData.getInnerHumidity());
                    realtimeSensor.put("grainTemperature", realtimeData.getGrainTemperature());
                    realtimeSensor.put("ammoniaConcentration", realtimeData.getAmmoniaConcentration());
                    result.put("realtimeSensor", realtimeSensor);
                }

                // 设施状态 - 从数据库读取
                FacilityStatus status = facilityStatusMapper.selectOne(
                        new LambdaQueryWrapper<FacilityStatus>()
                                .eq(FacilityStatus::getFacilityId, facility.getId())
                                .last("LIMIT 1")
                );
                if (status != null) {
                    // 仓位状态
                    Map<String, Object> positionStatus = new HashMap<>();
                    positionStatus.put("currentCapacity", status.getCurrentCapacity());
                    positionStatus.put("availableCapacity", status.getAvailableCapacity());
                    result.put("positionStatus", positionStatus);

                    // 设备状态
                    Map<String, Object> deviceStatus = new HashMap<>();
                    deviceStatus.put("ventilation", status.getVentilationStatus());
                    deviceStatus.put("humidityControl", status.getHumidityControlStatus());
                    deviceStatus.put("doorStatus", status.getDoorStatus());
                    deviceStatus.put("fireWaterPressure", status.getFireWaterPressure());
                    deviceStatus.put("securityInspection", status.getSecurityInspection());
                    result.put("deviceStatus", deviceStatus);

                    // 库存状态
                    Map<String, Object> stockStatus = new HashMap<>();
                    stockStatus.put("grainType", status.getStockGrainType());
                    stockStatus.put("stockWeight", status.getStockWeight());
                    stockStatus.put("entryBatchCount", status.getEntryBatchCount());
                    stockStatus.put("abnormalAlertCount", status.getAbnormalAlertCount());
                    result.put("stockStatus", stockStatus);
                }
            }

            // 库存统计
            Map<String, Object> stockStats = storageService.getStatistics();
            result.put("stockStats", stockStats);

            // 该设施的库存列表
            List<StorageRecord> stockList = new ArrayList<>();
            if (facility != null) {
                stockList = storageService.lambdaQuery()
                        .eq(StorageRecord::getFacilityId, facility.getId())
                        .eq(StorageRecord::getStatus, 0)
                        .list();
            }
            result.put("stockList", stockList);

            // 仓库视频监控
            Map<String, Object> videoMonitor = new HashMap<>();
            videoMonitor.put("channel1", "camera_001");
            videoMonitor.put("channel2", "camera_002");
            videoMonitor.put("channel3", "camera_003");
            result.put("videoMonitor", videoMonitor);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取仓库传感器数据失败, facilityId: {}", facilityId, e);
            return Result.fail("获取仓库数据失败: " + e.getMessage());
        }
    }
}