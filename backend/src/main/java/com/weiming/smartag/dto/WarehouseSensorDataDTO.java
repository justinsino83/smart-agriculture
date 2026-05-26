package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "仓库传感器数据")
public class WarehouseSensorDataDTO {

    @Schema(description = "仓库内温度(°C)", example = "18.4")
    private Object warehouseTemp;

    @Schema(description = "仓库内湿度(%)", example = "70")
    private Object warehouseHumidity;

    @Schema(description = "粮库温度(°C)", example = "16.9")
    private Object grainTemp;

    @Schema(description = "氨气浓度", example = "20.8")
    private Object ammoniaLevel;

    @Schema(description = "仓位状态列表")
    private List<WarehousePositionDTO> warehousePositions;

    @Schema(description = "仓库设备状态列表")
    private List<WarehouseDeviceStatusDTO> warehouseDevices;

    @Schema(description = "库存状态数据")
    private WarehouseStockStatusDTO stockStatus;

    @Schema(description = "库存统计数据")
    private Map<String, Object> stockStats;
}
