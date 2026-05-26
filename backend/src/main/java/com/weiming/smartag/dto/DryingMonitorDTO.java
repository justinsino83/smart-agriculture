package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "烘干车间监测数据")
public class DryingMonitorDTO {

    @Schema(description = "烘干状态", example = "运行中")
    private Object status;

    @Schema(description = "粮食温度(°C)", example = "58.6")
    private Object grainTemp;

    @Schema(description = "粮食水分(%)", example = "13.2")
    private Object moisturePercent;

    @Schema(description = "热风温度(°C)", example = "72.4")
    private Object windTemp;
}
