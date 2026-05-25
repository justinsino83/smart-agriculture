package com.weiming.smartag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weiming.smartag.common.Result;
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

import java.util.*;

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
    public Result<Map<String, Object>> getDryingSensors(
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
                        .eq(Facility::getType, 3)
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
                    realtimeSensor.put("innerHumidity", realtimeData.getHumidity());
                    realtimeSensor.put("hotAirTemperature", realtimeData.getHotAirTemperature());
                    realtimeSensor.put("outletMoisture", realtimeData.getOutletMoisture());
                    realtimeSensor.put("grainLayerThickness", realtimeData.getGrainLayerThickness());
                    result.put("realtimeSensor", realtimeSensor);
                }

                // 设施状态 - 从数据库读取
                FacilityStatus status = facilityStatusMapper.selectOne(
                        new LambdaQueryWrapper<FacilityStatus>()
                                .eq(FacilityStatus::getFacilityId, facility.getId())
                                .last("LIMIT 1")
                );
                if (status != null) {
                    // 运行状态
                    Map<String, Object> operationStatus = new HashMap<>();
                    operationStatus.put("runStatus", status.getRunStatus());
                    operationStatus.put("innerTemperature", realtimeData != null ? realtimeData.getTemperature() : null);
                    operationStatus.put("outletMoisture", realtimeData != null ? realtimeData.getOutletMoisture() : null);
                    operationStatus.put("hotAirTemperature", realtimeData != null ? realtimeData.getHotAirTemperature() : null);
                    result.put("operationStatus", operationStatus);

                    // 烘干工艺数据
                    Map<String, Object> processData = new HashMap<>();
                    processData.put("grainType", status.getGrainTypeProcessed());
                    processData.put("processingCapacity", status.getProcessingCapacity());
                    processData.put("targetMoisture", status.getTargetMoistureProcess());
                    result.put("processData", processData);

                    // 设备状态
                    Map<String, Object> deviceStatus = new HashMap<>();
                    deviceStatus.put("elevator", status.getElevatorStatus());
                    deviceStatus.put("circulatingFan", status.getCirculatingFanStatus());
                    deviceStatus.put("burner", status.getBurnerStatus());
                    deviceStatus.put("exhaustValve", status.getExhaustValveStatus());
                    deviceStatus.put("verticalDryingFan", status.getVerticalDryingFanStatus());
                    result.put("deviceStatus", deviceStatus);

                    // 能耗数据
                    Map<String, Object> energyConsumption = new HashMap<>();
                    energyConsumption.put("instantPower", status.getInstantPower());
                    energyConsumption.put("todayPowerConsumption", status.getTodayPowerConsumption());
                    energyConsumption.put("gasFlowRate", status.getGasFlowRate());
                    energyConsumption.put("outletGrainCount", status.getOutletGrainCount());
                    result.put("energyConsumption", energyConsumption);
                }
            }

            // 该设施的烘干批次
            List<DryingBatch> batches = new ArrayList<>();
            if (facility != null) {
                batches = dryingService.lambdaQuery()
                        .eq(DryingBatch::getFacilityId, facility.getId())
                        .orderByDesc(DryingBatch::getCreateTime)
                        .last("LIMIT 5")
                        .list();
            }
            result.put("recentBatches", batches);
            result.put("batchCount", batches.size());

            // 烘干视频监控
            Map<String, Object> videoMonitor = new HashMap<>();
            videoMonitor.put("channel1", "camera_dry_001");
            videoMonitor.put("channel2", "camera_dry_002");
            videoMonitor.put("channel3", "camera_dry_003");
            result.put("videoMonitor", videoMonitor);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取烘干车间传感器数据失败, facilityId: {}", facilityId, e);
            return Result.fail("获取烘干数据失败: " + e.getMessage());
        }
    }
}