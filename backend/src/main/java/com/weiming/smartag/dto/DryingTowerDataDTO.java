package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "烘干塔完整数据")
public class DryingTowerDataDTO {

    @Schema(description = "基础信息")
    private BaseInfoDTO baseInfo;

    @Schema(description = "烘干塔传感器数据")
    private DryingSensorDTO dryingSensor;

    @Schema(description = "烘干工艺数据")
    private DryingProcessOptimizedDTO dryingProcess;

    @Schema(description = "烘干设备数据")
    private DryingEquipmentDTO dryingEquipment;

    @Schema(description = "能耗与告警数据")
    private DryingEnergyAlarmDTO energyAlarm;

    @Schema(description = "烘干监控数据")
    private DryingVideoMonitorDTO videoMonitor;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "基础信息")
    public static class BaseInfoDTO {
        @Schema(description = "运行状态", example = "运行中")
        private String runStatus;
        @Schema(description = "塔内温度(°C)", example = "65.2")
        private BigDecimal innerTemperature;
        @Schema(description = "出粮水分(%)", example = "13.5")
        private BigDecimal outletMoisture;
        @Schema(description = "热风温度(°C)", example = "78.3")
        private BigDecimal hotAirTemperature;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "烘干塔传感器数据")
    public static class DryingSensorDTO {
        @Schema(description = "塔内温度")
        private SensorItemDTO innerTemperature;
        @Schema(description = "热风温度")
        private SensorItemDTO hotAirTemperature;
        @Schema(description = "出粮水分")
        private SensorItemDTO outletMoisture;
        @Schema(description = "粮层厚度")
        private SensorItemDTO grainLayerThickness;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "传感器项")
    public static class SensorItemDTO {
        @Schema(description = "值", example = "65.2")
        private BigDecimal value;
        @Schema(description = "状态", example = "正常")
        private String status;
        @Schema(description = "单位", example = "°C")
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "烘干工艺数据")
    public static class DryingProcessOptimizedDTO {
        @Schema(description = "处理粮种", example = "水稻")
        private String grainType;
        @Schema(description = "运行状态", example = "运行中")
        private String runStatus;
        @Schema(description = "处理能力(t/h)", example = "8.5")
        private BigDecimal processingCapacity;
        @Schema(description = "目标水分(%)", example = "14.5")
        private BigDecimal targetMoisture;
        @Schema(description = "烘干时长(min)", example = "120")
        private Integer dryingDuration;
        @Schema(description = "状态", example = "正常")
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "烘干设备数据")
    public static class DryingEquipmentDTO {
        @Schema(description = "提升机运行状态")
        private DeviceItemDTO elevator;
        @Schema(description = "垂直烘干风机运行状态")
        private DeviceItemDTO verticalDryingFan;
        @Schema(description = "循环风机")
        private DeviceItemWithSpeedDTO circulatingFan;
        @Schema(description = "燃烧器状态")
        private DeviceItemDTO burner;
        @Schema(description = "排风阀运行模式")
        private DeviceItemDTO exhaustValve;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "设备项")
    public static class DeviceItemDTO {
        @Schema(description = "状态", example = "运行中")
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "带转速的设备项")
    public static class DeviceItemWithSpeedDTO {
        @Schema(description = "状态", example = "运行中")
        private String status;
        @Schema(description = "转速(r/min)", example = "1450")
        private Integer speed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "能耗与告警数据")
    public static class DryingEnergyAlarmDTO {
        @Schema(description = "瞬时功率")
        private EnergyItemDTO instantPower;
        @Schema(description = "今日耗电")
        private EnergyItemWithTagDTO todayPowerConsumption;
        @Schema(description = "燃气流量")
        private EnergyItemDTO gasFlowRate;
        @Schema(description = "出粮量")
        private EnergyItemDTO outletGrainCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "能耗项")
    public static class EnergyItemDTO {
        @Schema(description = "值", example = "45.2")
        private BigDecimal value;
        @Schema(description = "状态", example = "正常")
        private String status;
        @Schema(description = "单位", example = "kW")
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "带标签的能耗项")
    public static class EnergyItemWithTagDTO {
        @Schema(description = "值", example = "1250.5")
        private BigDecimal value;
        @Schema(description = "状态", example = "正常")
        private String status;
        @Schema(description = "标签", example = "节能")
        private String tag;
        @Schema(description = "单位", example = "kWh")
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "烘干监控数据")
    public static class DryingVideoMonitorDTO {
        @Schema(description = "摄像头列表")
        private List<CameraDTO> cameras;
        @Schema(description = "当前摄像头ID")
        private String currentCameraId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "摄像头信息")
    public static class CameraDTO {
        @Schema(description = "摄像头ID", example = "camera_dry_001")
        private String id;
        @Schema(description = "摄像头名称", example = "烘干塔进料口")
        private String name;
        @Schema(description = "推流地址", example = "rtsp://192.168.1.100:554/stream1")
        private String streamUrl;
    }
}
