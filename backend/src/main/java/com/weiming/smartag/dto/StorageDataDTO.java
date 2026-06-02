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
@Schema(description = "仓库完整数据")
public class StorageDataDTO {

    @Schema(description = "基础数据")
    private BaseData baseData;

    @Schema(description = "仓库传感器数据")
    private StorageSensorData storageSensorData;

    @Schema(description = "库位与位置数据")
    private LocationData locationData;

    @Schema(description = "仓库设备数据")
    private DeviceData deviceData;

    @Schema(description = "库存状态数据")
    private StockStatus stockStatus;

    @Schema(description = "仓库监控视频数据")
    private VideoMonitorData videoMonitorData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "基础数据")
    public static class BaseData {
        @Schema(description = "库内温度")
        private String innerTemperature;

        @Schema(description = "当前库容")
        private String currentCapacity;

        @Schema(description = "库存重量")
        private String stockWeight;

        @Schema(description = "可用库位")
        private String availableLocation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "仓库传感器数据")
    public static class StorageSensorData {
        @Schema(description = "库内温度")
        private SensorItem innerTemperature;

        @Schema(description = "库内湿度")
        private SensorItem innerHumidity;

        @Schema(description = "粮堆温度")
        private SensorItem grainTemperature;

        @Schema(description = "氨气浓度")
        private SensorItem ammoniaConcentration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "库位与位置数据")
    public static class LocationData {
        @Schema(description = "当前库容")
        private SensorItem currentCapacity;

        @Schema(description = "可用库容")
        private SensorItem availableCapacity;

        @Schema(description = "库存总量")
        private SensorItem totalStock;

        @Schema(description = "库存批次")
        private SensorItem stockBatch;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "仓库设备数据")
    public static class DeviceData {
        @Schema(description = "通风系统")
        private DeviceItem ventilation;

        @Schema(description = "湿度控制")
        private DeviceItem humidityControl;

        @Schema(description = "门禁状态")
        private DeviceItem doorStatus;

        @Schema(description = "消防水压")
        private SensorItem fireWaterPressure;

        @Schema(description = "安防巡检")
        private DeviceItem securityInspection;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "库存状态数据")
    public static class StockStatus {
        @Schema(description = "库存粮种")
        private DeviceItem grainType;

        @Schema(description = "库存重量")
        private SensorItem stockWeight;

        @Schema(description = "入库批次")
        private SensorItem entryBatchCount;

        @Schema(description = "异常告警")
        private SensorItem abnormalAlertCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "仓库监控视频数据")
    public static class VideoMonitorData {
        @Schema(description = "摄像头列表")
        private List<CameraItem> cameras;

        @Schema(description = "当前摄像头ID")
        private String currentCameraId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "摄像头项")
    public static class CameraItem {
        @Schema(description = "摄像头ID")
        private String cameraId;

        @Schema(description = "摄像头名称")
        private String cameraName;

        @Schema(description = "推流地址")
        private String streamUrl;

        @Schema(description = "安装位置")
        private String position;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "传感器项")
    public static class SensorItem {
        @Schema(description = "值")
        private BigDecimal value;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "单位")
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "设备项")
    public static class DeviceItem {
        @Schema(description = "状态")
        private String status;
    }
}
