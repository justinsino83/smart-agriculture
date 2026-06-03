package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.dto.StorageDataDTO;
import com.weiming.smartag.entity.Facility;
import com.weiming.smartag.entity.FacilityCamera;
import com.weiming.smartag.entity.FacilityRealtimeData;
import com.weiming.smartag.entity.FacilityStatus;
import com.weiming.smartag.mapper.FacilityCameraMapper;
import com.weiming.smartag.mapper.FacilityRealtimeDataMapper;
import com.weiming.smartag.mapper.FacilityStatusMapper;
import com.weiming.smartag.mapper.StorageRecordMapper;
import com.weiming.smartag.service.FacilityService;
import com.weiming.smartag.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "仓库管理", description = "仓库数据管理接口")
public class StorageController {

    private final StorageService storageService;
    private final FacilityService facilityService;
    private final FacilityRealtimeDataMapper facilityRealtimeDataMapper;
    private final FacilityStatusMapper facilityStatusMapper;
    private final FacilityCameraMapper facilityCameraMapper;
    private final StorageRecordMapper storageRecordMapper;

    @GetMapping("/records")
    public Result<List<com.weiming.smartag.entity.StorageRecord>> listRecords() {
        try {
            return Result.success(storageService.list());
        } catch (Exception e) {
            log.error("获取仓储记录失败", e);
            return Result.fail("获取仓储记录失败: " + e.getMessage());
        }
    }

    @GetMapping("/stock")
    @Operation(summary = "获取库存列表", description = "分页获取库存列表")
    public Result<java.util.Map<String, Object>> getStockList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词（品种/批次号）") @RequestParam(required = false) String keyword) {
        try {
            return Result.success(storageService.getStockListPage(page, size, keyword));
        } catch (Exception e) {
            log.error("获取在库列表失败", e);
            return Result.fail("获取在库列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/in")
    public Result<?> storageIn(@RequestBody com.weiming.smartag.entity.StorageRecord record) {
        try {
            if (record == null) {
                return Result.fail("入库数据不能为空");
            }
            if (!org.springframework.util.StringUtils.hasText(record.getBatchNo())) {
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
            if (!org.springframework.util.StringUtils.hasText(batchNo)) {
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
    public Result<java.util.Map<String, Object>> getStatistics() {
        try {
            return Result.success(storageService.getStatistics());
        } catch (Exception e) {
            log.error("获取库存统计失败", e);
            return Result.fail("获取库存统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/overview")
    @Operation(summary = "获取仓储概览数据", description = "获取总库存、今日出入库、预警数量等概览数据")
    public Result<java.util.Map<String, Object>> getOverview() {
        try {
            return Result.success(storageService.getOverview());
        } catch (Exception e) {
            log.error("获取仓储概览失败", e);
            return Result.fail("获取仓储概览失败: " + e.getMessage());
        }
    }

    @GetMapping("/trend")
    public Result<List<java.util.Map<String, Object>>> getEntryExitTrend(
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
    public Result<StorageDataDTO> getWarehouseSensors(
            @Parameter(description = "设施ID")
            @RequestParam(required = false) Long facilityId) {
        try {
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

            if (facility == null) {
                return Result.error("未找到仓库设施");
            }

            // 构建返回数据
            StorageDataDTO.StorageDataDTOBuilder builder = StorageDataDTO.builder();

            // 获取设施实时传感器数据
            FacilityRealtimeData realtimeData = facilityRealtimeDataMapper.selectOne(
                    new LambdaQueryWrapper<FacilityRealtimeData>()
                            .eq(FacilityRealtimeData::getFacilityId, facility.getId())
                            .orderByDesc(FacilityRealtimeData::getCollectTime)
                            .last("LIMIT 1"));

            // 获取设施状态
            FacilityStatus status = facilityStatusMapper.selectOne(
                    new LambdaQueryWrapper<FacilityStatus>()
                            .eq(FacilityStatus::getFacilityId, facility.getId())
                            .last("LIMIT 1"));

            // 获取库存统计信息
            List<com.weiming.smartag.entity.StorageRecord> stockList = storageRecordMapper.selectList(
                    new LambdaQueryWrapper<com.weiming.smartag.entity.StorageRecord>()
                            .eq(com.weiming.smartag.entity.StorageRecord::getFacilityId, facility.getId())
                            .eq(com.weiming.smartag.entity.StorageRecord::getStatus, 0));
            
            BigDecimal totalWeight = BigDecimal.ZERO;
            java.util.Set<String> batchNoSet = new java.util.HashSet<>();
            
            if (stockList != null && !stockList.isEmpty()) {
                for (com.weiming.smartag.entity.StorageRecord record : stockList) {
                    if (record.getQuantity() != null) {
                        totalWeight = totalWeight.add(BigDecimal.valueOf(record.getQuantity()));
                    } else if (record.getWeight() != null) {
                        // weight单位是kg，转换为t
                        totalWeight = totalWeight.add(BigDecimal.valueOf(record.getWeight()).divide(BigDecimal.valueOf(1000), 2, BigDecimal.ROUND_HALF_UP));
                    }
                    if (record.getBatchNo() != null && !record.getBatchNo().isEmpty()) {
                        batchNoSet.add(record.getBatchNo());
                    }
                }
            }
            Long batchCount = (long) batchNoSet.size();

            // 1. 基础数据
            builder.baseData(buildBaseData(realtimeData, status, totalWeight));

            // 2. 仓库传感器数据
            builder.storageSensorData(buildStorageSensorData(realtimeData));

            // 3. 库位与位置数据
            builder.locationData(buildLocationData(status, totalWeight, batchCount));

            // 4. 仓库设备数据
            builder.deviceData(buildDeviceData(status));

            // 5. 库存状态数据
            builder.stockStatus(buildStockStatus(status, totalWeight, batchCount));

            // 6. 仓库监控视频数据
            builder.videoMonitorData(buildVideoMonitorData(facility.getId()));

            return Result.success(builder.build());

        } catch (Exception e) {
            log.error("获取仓库传感器数据失败, facilityId: {}", facilityId, e);
            return Result.fail("获取仓库数据失败: " + e.getMessage());
        }
    }

    private StorageDataDTO.BaseData buildBaseData(FacilityRealtimeData realtimeData, FacilityStatus status, BigDecimal totalWeight) {
        return StorageDataDTO.BaseData.builder()
                .innerTemperature((realtimeData != null && realtimeData.getInnerTemperature() != null
                        ? realtimeData.getInnerTemperature() : new BigDecimal("18.2")) + "°C")
                .currentCapacity((status != null && status.getCurrentCapacity() != null
                        ? status.getCurrentCapacity() : new BigDecimal("82")) + "%")
                .stockWeight((totalWeight != null && totalWeight.compareTo(BigDecimal.ZERO) > 0
                        ? totalWeight : new BigDecimal("670")) + "t")
                .availableLocation((status != null && status.getAvailableCapacity() != null
                        ? status.getAvailableCapacity() : new BigDecimal("18")) + "%")
                .build();
    }

    private StorageDataDTO.StorageSensorData buildStorageSensorData(FacilityRealtimeData realtimeData) {
        return StorageDataDTO.StorageSensorData.builder()
                .innerTemperature(buildSensorItem(
                        realtimeData != null && realtimeData.getInnerTemperature() != null
                                ? realtimeData.getInnerTemperature() : new BigDecimal("18.2"),
                        "°C",
                        "正常"))
                .innerHumidity(buildSensorItem(
                        realtimeData != null && realtimeData.getInnerHumidity() != null
                                ? realtimeData.getInnerHumidity() : new BigDecimal("58"),
                        "%",
                        "正常"))
                .grainTemperature(buildSensorItem(
                        realtimeData != null && realtimeData.getGrainTemperature() != null
                                ? realtimeData.getGrainTemperature() : new BigDecimal("17.2"),
                        "°C",
                        "正常"))
                .ammoniaConcentration(buildSensorItem(
                        realtimeData != null && realtimeData.getAmmoniaConcentration() != null
                                ? realtimeData.getAmmoniaConcentration() : new BigDecimal("19.5"),
                        "ppm",
                        "正常"))
                .build();
    }

    private StorageDataDTO.LocationData buildLocationData(FacilityStatus status, BigDecimal totalWeight, Long batchCount) {
        return StorageDataDTO.LocationData.builder()
                .currentCapacity(buildSensorItem(
                        status != null && status.getCurrentCapacity() != null
                                ? status.getCurrentCapacity() : new BigDecimal("82"),
                        "%",
                        "正常"))
                .availableCapacity(buildSensorItem(
                        status != null && status.getAvailableCapacity() != null
                                ? status.getAvailableCapacity() : new BigDecimal("18"),
                        "%",
                        "正常"))
                .totalStock(buildSensorItem(
                        totalWeight != null && totalWeight.compareTo(BigDecimal.ZERO) > 0
                                ? totalWeight : new BigDecimal("670"),
                        "t",
                        "正常"))
                .stockBatch(buildSensorItem(
                        batchCount != null ? BigDecimal.valueOf(batchCount) : new BigDecimal("13"),
                        "批",
                        "正常"))
                .build();
    }

    private StorageDataDTO.DeviceData buildDeviceData(FacilityStatus status) {
        return StorageDataDTO.DeviceData.builder()
                .ventilation(StorageDataDTO.DeviceItem.builder()
                        .status(status != null && status.getVentilationStatus() != null
                                ? status.getVentilationStatus() : "正常")
                        .build())
                .humidityControl(StorageDataDTO.DeviceItem.builder()
                        .status(status != null && status.getHumidityControlStatus() != null
                                ? status.getHumidityControlStatus() : "运行")
                        .build())
                .doorStatus(StorageDataDTO.DeviceItem.builder()
                        .status(status != null && status.getDoorStatus() != null
                                ? status.getDoorStatus() : "锁定")
                        .build())
                .fireWaterPressure(buildSensorItem(
                        status != null && status.getFireWaterPressure() != null
                                ? status.getFireWaterPressure() : new BigDecimal("0.42"),
                        "MPa",
                        "正常"))
                .securityInspection(StorageDataDTO.DeviceItem.builder()
                        .status(status != null && status.getSecurityInspection() != null
                                ? status.getSecurityInspection() : "正常")
                        .build())
                .build();
    }

    private StorageDataDTO.StockStatus buildStockStatus(FacilityStatus status, BigDecimal totalWeight, Long batchCount) {
        return StorageDataDTO.StockStatus.builder()
                .grainType(StorageDataDTO.DeviceItem.builder()
                        .status(status != null && status.getStockGrainType() != null
                                ? status.getStockGrainType() : "玉米")
                        .build())
                .stockWeight(buildSensorItem(
                        totalWeight != null && totalWeight.compareTo(BigDecimal.ZERO) > 0
                                ? totalWeight : new BigDecimal("670"),
                        "t",
                        "正常"))
                .entryBatchCount(buildSensorItem(
                        batchCount != null ? BigDecimal.valueOf(batchCount) : new BigDecimal("10"),
                        "批",
                        "正常"))
                .abnormalAlertCount(buildSensorItem(
                        status != null && status.getAbnormalAlertCount() != null
                                ? BigDecimal.valueOf(status.getAbnormalAlertCount()) : new BigDecimal("2"),
                        "条",
                        "正常"))
                .build();
    }

    private StorageDataDTO.VideoMonitorData buildVideoMonitorData(Long facilityId) {
        List<StorageDataDTO.CameraItem> cameras = new ArrayList<>();

        // 获取摄像头数据
        List<FacilityCamera> cameraList = facilityCameraMapper.selectList(
                new LambdaQueryWrapper<FacilityCamera>()
                        .eq(FacilityCamera::getFacilityId, facilityId)
                        .eq(FacilityCamera::getStatus, 1)
                        .orderByAsc(FacilityCamera::getSortOrder));

        String currentCameraId = null;

        if (cameraList != null && !cameraList.isEmpty()) {
            for (FacilityCamera camera : cameraList) {
                cameras.add(StorageDataDTO.CameraItem.builder()
                        .cameraId(camera.getCameraId())
                        .cameraName(camera.getCameraName())
                        .streamUrl(camera.getStreamUrl())
                        .position(camera.getPosition())
                        .build());
            }
            currentCameraId = cameraList.get(0).getCameraId();
        } else {
            // 如果没有数据，使用默认模拟数据
            cameras.add(StorageDataDTO.CameraItem.builder()
                    .cameraId("storage_cam_001")
                    .cameraName("仓库东门")
                    .streamUrl("rtsp://192.168.1.100:554/storage" + facilityId + "/1")
                    .position("东门入口")
                    .build());
            cameras.add(StorageDataDTO.CameraItem.builder()
                    .cameraId("storage_cam_002")
                    .cameraName("仓库中心")
                    .streamUrl("rtsp://192.168.1.100:554/storage" + facilityId + "/2")
                    .position("中心区域")
                    .build());
            cameras.add(StorageDataDTO.CameraItem.builder()
                    .cameraId("storage_cam_003")
                    .cameraName("仓库西门")
                    .streamUrl("rtsp://192.168.1.100:554/storage" + facilityId + "/3")
                    .position("西门入口")
                    .build());
            currentCameraId = "storage_cam_001";
        }

        return StorageDataDTO.VideoMonitorData.builder()
                .cameras(cameras)
                .currentCameraId(currentCameraId)
                .build();
    }

    private StorageDataDTO.SensorItem buildSensorItem(BigDecimal value, String unit, String status) {
        return StorageDataDTO.SensorItem.builder()
                .value(value)
                .status(status)
                .unit(unit)
                .build();
    }

    @PostMapping("/stock-in")
    @Operation(summary = "入库登记", description = "新增库存入库记录")
    public Result<?> stockIn(@RequestBody com.weiming.smartag.entity.StorageRecord record) {
        try {
            if (record == null) {
                return Result.fail("入库数据不能为空");
            }
            boolean success = storageService.storageIn(record);
            return success ? Result.success("入库成功") : Result.error("入库失败");
        } catch (Exception e) {
            log.error("入库失败, record: {}", record, e);
            return Result.fail("入库失败: " + e.getMessage());
        }
    }

    @PostMapping("/stock-out")
    @Operation(summary = "出库登记", description = "执行库存出库操作")
    public Result<?> stockOut(@RequestBody java.util.Map<String, String> params) {
        try {
            String batchNo = params.get("batchNo");
            if (!org.springframework.util.StringUtils.hasText(batchNo)) {
                return Result.fail("批次号不能为空");
            }
            boolean success = storageService.storageOut(batchNo);
            return success ? Result.success("出库成功") : Result.error("出库失败，批次号不存在");
        } catch (Exception e) {
            log.error("出库失败, params: {}", params, e);
            return Result.fail("出库失败: " + e.getMessage());
        }
    }

    @GetMapping("/alerts")
    @Operation(summary = "获取库存预警", description = "获取所有库存预警信息")
    public Result<List<java.util.Map<String, Object>>> getAlerts() {
        try {
            return Result.success(storageService.getAlerts());
        } catch (Exception e) {
            log.error("获取库存预警失败", e);
            return Result.fail("获取库存预警失败: " + e.getMessage());
        }
    }

    @GetMapping("/stock/{stockId}")
    @Operation(summary = "获取库存详情", description = "根据ID获取库存详情")
    public Result<com.weiming.smartag.entity.StorageRecord> getStockDetail(@PathVariable Long stockId) {
        try {
            return Result.success(storageService.getStockDetail(stockId));
        } catch (Exception e) {
            log.error("获取库存详情失败, stockId: {}", stockId, e);
            return Result.fail("获取库存详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/stock/{stockId}/trace")
    @Operation(summary = "获取库存追溯信息", description = "根据库存ID获取追溯信息")
    public Result<java.util.Map<String, Object>> getTrace(@PathVariable Long stockId) {
        try {
            return Result.success(storageService.getTrace(stockId));
        } catch (Exception e) {
            log.error("获取库存追溯信息失败, stockId: {}", stockId, e);
            return Result.fail("获取库存追溯信息失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/stock/{stockId}")
    @Operation(summary = "删除库存记录", description = "根据ID删除库存记录")
    public Result<?> deleteStock(@PathVariable Long stockId) {
        try {
            boolean success = storageService.deleteStock(stockId);
            return success ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除库存记录失败, stockId: {}", stockId, e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }
}
