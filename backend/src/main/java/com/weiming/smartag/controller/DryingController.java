package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weiming.smartag.common.Result;
import com.weiming.smartag.dto.DryingTowerDataDTO;
import com.weiming.smartag.entity.DryingBatch;
import com.weiming.smartag.entity.Facility;
import com.weiming.smartag.entity.FacilityRealtimeData;
import com.weiming.smartag.entity.FacilityStatus;
import com.weiming.smartag.mapper.FacilityRealtimeDataMapper;
import com.weiming.smartag.mapper.FacilityStatusMapper;
import com.weiming.smartag.service.DevicePushService;
import com.weiming.smartag.service.DryingService;
import com.weiming.smartag.service.FacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/drying")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "烘干车间管理", description = "烘干车间数据管理接口")
public class DryingController {

    private final DryingService dryingService;
    private final DevicePushService devicePushService;
    private final FacilityService facilityService;
    private final FacilityRealtimeDataMapper facilityRealtimeDataMapper;
    private final FacilityStatusMapper facilityStatusMapper;

    @GetMapping("/batches")
    public Result<List<DryingBatch>> listBatches() {
        try {
            return Result.success(dryingService.list());
        } catch (Exception e) {
            log.error("获取烘干批次列表失败", e);
            return Result.fail("获取批次列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/batches/recent")
    public Result<List<Map<String, Object>>> getRecentBatches(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (limit < 1 || limit > 100) {
                limit = 10;
            }
            return Result.success(dryingService.getRecentBatches(limit));
        } catch (Exception e) {
            log.error("获取最近烘干批次失败, limit: {}", limit, e);
            return Result.fail("获取批次失败: " + e.getMessage());
        }
    }

    @GetMapping("/batch/{batchId}")
    public Result<DryingBatch> getBatch(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            return Result.success(dryingService.getById(batchId));
        } catch (Exception e) {
            log.error("获取烘干批次详情失败, batchId: {}", batchId, e);
            return Result.fail("获取批次详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch")
    public Result<?> createBatch(@RequestBody DryingBatch batch) {
        try {
            if (batch == null) {
                return Result.fail("批次数据不能为空");
            }
            boolean success = dryingService.save(batch);
            return success ? Result.success("创建成功") : Result.error("创建失败");
        } catch (Exception e) {
            log.error("创建烘干批次失败, batch: {}", batch, e);
            return Result.fail("创建批次失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch/{batchId}/start")
    public Result<?> startBatch(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            boolean success = dryingService.startBatch(batchId);
            return success ? Result.success("启动成功") : Result.error("启动失败");
        } catch (Exception e) {
            log.error("启动烘干批次失败, batchId: {}", batchId, e);
            return Result.fail("启动批次失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch/{batchId}/pause")
    public Result<?> pauseBatch(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            boolean success = dryingService.pauseBatch(batchId);
            return success ? Result.success("暂停成功") : Result.error("暂停失败");
        } catch (Exception e) {
            log.error("暂停烘干批次失败, batchId: {}", batchId, e);
            return Result.fail("暂停批次失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch/{batchId}/resume")
    public Result<?> resumeBatch(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            boolean success = dryingService.resumeBatch(batchId);
            return success ? Result.success("恢复成功") : Result.error("恢复失败");
        } catch (Exception e) {
            log.error("恢复烘干批次失败, batchId: {}", batchId, e);
            return Result.fail("恢复批次失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch/{batchId}/stop")
    public Result<?> stopBatch(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            boolean success = dryingService.stopBatch(batchId);
            return success ? Result.success("停止成功") : Result.error("停止失败");
        } catch (Exception e) {
            log.error("停止烘干批次失败, batchId: {}", batchId, e);
            return Result.fail("停止批次失败: " + e.getMessage());
        }
    }

    @GetMapping("/batch/{batchId}/curve")
    public Result<Map<String, Object>> getCurveData(@PathVariable Long batchId) {
        try {
            if (batchId == null || batchId <= 0) {
                return Result.fail("批次ID必须大于0");
            }
            return Result.success(dryingService.getCurveData(batchId));
        } catch (Exception e) {
            log.error("获取工艺曲线数据失败, batchId: {}", batchId, e);
            return Result.fail("获取曲线数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        try {
            return Result.success(dryingService.getStatistics());
        } catch (Exception e) {
            log.error("获取烘干统计失败", e);
            return Result.fail("获取统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/sensors")
    @Operation(summary = "获取烘干车间传感器数据", description = "获取烘干车间的环境监测、设备状态、能耗数据等综合数据")
    public Result<DryingTowerDataDTO> getDryingSensors(
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
                        .eq(Facility::getType, 3)
                        .last("LIMIT 1")
                        .one();
            }

            // 构建烘干塔数据
            DryingTowerDataDTO.DryingTowerDataDTOBuilder builder = DryingTowerDataDTO.builder();

            if (facility != null) {
                // 获取实时传感器数据
                FacilityRealtimeData realtimeData = facilityRealtimeDataMapper.selectOne(
                        new LambdaQueryWrapper<FacilityRealtimeData>()
                                .eq(FacilityRealtimeData::getFacilityId, facility.getId())
                                .orderByDesc(FacilityRealtimeData::getCollectTime)
                                .last("LIMIT 1")
                );

                // 获取设施状态
                FacilityStatus status = facilityStatusMapper.selectOne(
                        new LambdaQueryWrapper<FacilityStatus>()
                                .eq(FacilityStatus::getFacilityId, facility.getId())
                                .last("LIMIT 1")
                );

                // 1. 基础信息
                DryingTowerDataDTO.BaseInfoDTO baseInfo = buildBaseInfo(realtimeData, status);
                builder.baseInfo(baseInfo);

                // 2. 烘干塔传感器数据
                DryingTowerDataDTO.DryingSensorDTO dryingSensor = buildDryingSensor(realtimeData);
                builder.dryingSensor(dryingSensor);

                // 3. 烘干工艺数据
                DryingTowerDataDTO.DryingProcessOptimizedDTO dryingProcess = buildDryingProcess(status);
                builder.dryingProcess(dryingProcess);

                // 4. 烘干设备数据
                DryingTowerDataDTO.DryingEquipmentDTO dryingEquipment = buildDryingEquipment(status);
                builder.dryingEquipment(dryingEquipment);

                // 5. 能耗与告警数据
                DryingTowerDataDTO.DryingEnergyAlarmDTO energyAlarm = buildEnergyAlarm(status);
                builder.energyAlarm(energyAlarm);

                // 6. 烘干监控数据
                DryingTowerDataDTO.DryingVideoMonitorDTO videoMonitor = buildVideoMonitor();
                builder.videoMonitor(videoMonitor);
            }

            return Result.success(builder.build());
        } catch (Exception e) {
            log.error("获取烘干车间传感器数据失败, facilityId: {}", facilityId, e);
            return Result.fail("获取烘干数据失败: " + e.getMessage());
        }
    }

    private DryingTowerDataDTO.BaseInfoDTO buildBaseInfo(FacilityRealtimeData realtimeData, FacilityStatus status) {
        return DryingTowerDataDTO.BaseInfoDTO.builder()
                .runStatus(status != null && status.getRunStatus() != null ? status.getRunStatus() : "运行中")
                .innerTemperature(realtimeData != null && realtimeData.getTemperature() != null ? realtimeData.getTemperature() : new BigDecimal("65.5"))
                .outletMoisture(realtimeData != null && realtimeData.getOutletMoisture() != null ? realtimeData.getOutletMoisture() : new BigDecimal("13.5"))
                .hotAirTemperature(realtimeData != null && realtimeData.getHotAirTemperature() != null ? realtimeData.getHotAirTemperature() : new BigDecimal("78.2"))
                .build();
    }

    private DryingTowerDataDTO.DryingSensorDTO buildDryingSensor(FacilityRealtimeData realtimeData) {
        return DryingTowerDataDTO.DryingSensorDTO.builder()
                .innerTemperature(buildSensorItem(realtimeData != null && realtimeData.getTemperature() != null ? realtimeData.getTemperature() : new BigDecimal("65.5"), "°C"))
                .hotAirTemperature(buildSensorItem(realtimeData != null && realtimeData.getHotAirTemperature() != null ? realtimeData.getHotAirTemperature() : new BigDecimal("78.2"), "°C"))
                .outletMoisture(buildSensorItem(realtimeData != null && realtimeData.getOutletMoisture() != null ? realtimeData.getOutletMoisture() : new BigDecimal("13.5"), "%"))
                .grainLayerThickness(buildSensorItem(realtimeData != null && realtimeData.getGrainLayerThickness() != null ? realtimeData.getGrainLayerThickness() : new BigDecimal("1.8"), "m"))
                .build();
    }

    private DryingTowerDataDTO.SensorItemDTO buildSensorItem(BigDecimal value, String unit) {
        return DryingTowerDataDTO.SensorItemDTO.builder()
                .value(value)
                .status("正常")
                .unit(unit)
                .build();
    }

    private DryingTowerDataDTO.DryingProcessOptimizedDTO buildDryingProcess(FacilityStatus status) {
        return DryingTowerDataDTO.DryingProcessOptimizedDTO.builder()
                .grainType(status != null && status.getGrainTypeProcessed() != null ? status.getGrainTypeProcessed() : "水稻")
                .runStatus(status != null && status.getRunStatus() != null ? status.getRunStatus() : "运行中")
                .processingCapacity(status != null && status.getProcessingCapacity() != null ? status.getProcessingCapacity() : new BigDecimal("8.5"))
                .targetMoisture(status != null && status.getTargetMoistureProcess() != null ? status.getTargetMoistureProcess() : new BigDecimal("14.5"))
                .dryingDuration(120)
                .status("正常")
                .build();
    }

    private DryingTowerDataDTO.DryingEquipmentDTO buildDryingEquipment(FacilityStatus status) {
        return DryingTowerDataDTO.DryingEquipmentDTO.builder()
                .elevator(DryingTowerDataDTO.DeviceItemDTO.builder()
                        .status(status != null && status.getElevatorStatus() != null ? status.getElevatorStatus() : "运行中")
                        .build())
                .verticalDryingFan(DryingTowerDataDTO.DeviceItemDTO.builder()
                        .status(status != null && status.getVerticalDryingFanStatus() != null ? status.getVerticalDryingFanStatus() : "运行中")
                        .build())
                .circulatingFan(DryingTowerDataDTO.DeviceItemWithSpeedDTO.builder()
                        .status(status != null && status.getCirculatingFanStatus() != null ? status.getCirculatingFanStatus() : "运行中")
                        .speed(1450)
                        .build())
                .burner(DryingTowerDataDTO.DeviceItemDTO.builder()
                        .status(status != null && status.getBurnerStatus() != null ? status.getBurnerStatus() : "稳定")
                        .build())
                .exhaustValve(DryingTowerDataDTO.DeviceItemDTO.builder()
                        .status(status != null && status.getExhaustValveStatus() != null ? status.getExhaustValveStatus() : "自动")
                        .build())
                .build();
    }

    private DryingTowerDataDTO.DryingEnergyAlarmDTO buildEnergyAlarm(FacilityStatus status) {
        return DryingTowerDataDTO.DryingEnergyAlarmDTO.builder()
                .instantPower(DryingTowerDataDTO.EnergyItemDTO.builder()
                        .value(status != null && status.getInstantPower() != null ? status.getInstantPower() : new BigDecimal("45.2"))
                        .status("正常")
                        .unit("kW")
                        .build())
                .todayPowerConsumption(DryingTowerDataDTO.EnergyItemWithTagDTO.builder()
                        .value(status != null && status.getTodayPowerConsumption() != null ? status.getTodayPowerConsumption() : new BigDecimal("1250.5"))
                        .status("正常")
                        .tag("节能")
                        .unit("kWh")
                        .build())
                .gasFlowRate(DryingTowerDataDTO.EnergyItemDTO.builder()
                        .value(status != null && status.getGasFlowRate() != null ? status.getGasFlowRate() : new BigDecimal("25.8"))
                        .status("正常")
                        .unit("m³/h")
                        .build())
                .outletGrainCount(DryingTowerDataDTO.EnergyItemDTO.builder()
                        .value(status != null && status.getOutletGrainCount() != null ? status.getOutletGrainCount() : new BigDecimal("28.5"))
                        .status("正常")
                        .unit("t")
                        .build())
                .build();
    }

    private DryingTowerDataDTO.DryingVideoMonitorDTO buildVideoMonitor() {
        List<DryingTowerDataDTO.CameraDTO> cameras = new ArrayList<>();
        cameras.add(DryingTowerDataDTO.CameraDTO.builder()
                .id("camera_dry_001")
                .name("烘干塔进料口")
                .streamUrl("rtsp://192.168.1.100:554/stream1")
                .build());
        cameras.add(DryingTowerDataDTO.CameraDTO.builder()
                .id("camera_dry_002")
                .name("烘干塔内部")
                .streamUrl("rtsp://192.168.1.100:554/stream2")
                .build());
        cameras.add(DryingTowerDataDTO.CameraDTO.builder()
                .id("camera_dry_003")
                .name("烘干塔出料口")
                .streamUrl("rtsp://192.168.1.100:554/stream3")
                .build());

        return DryingTowerDataDTO.DryingVideoMonitorDTO.builder()
                .cameras(cameras)
                .currentCameraId("camera_dry_001")
                .build();
    }
}
