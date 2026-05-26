package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "烘干车间传感器数据")
public class DryingSensorDataDTO {

    @Schema(description = "库内湿度(%)", example = "58.6")
    private Object dryingHumidity;

    @Schema(description = "热风温度(°C)", example = "72.4")
    private Object hotAirTemp;

    @Schema(description = "粮食水分(%)", example = "13.2")
    private Object grainMoisture;

    @Schema(description = "粮层厚度(m)", example = "1.8")
    private Object grainLayerThickness;

    @Schema(description = "烘干车间监测数据")
    private DryingMonitorDTO dryingMonitor;

    @Schema(description = "烘干设备状态列表")
    private List<DryingDeviceStatusDTO> dryingDevices;

    @Schema(description = "烘干能耗数据")
    private DryingEnergyDTO dryingEnergy;

    @Schema(description = "烘干工艺数据")
    private DryingProcessDTO dryingProcess;
}
