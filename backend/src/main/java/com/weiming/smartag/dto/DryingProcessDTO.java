package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "烘干工艺数据")
public class DryingProcessDTO {

    @Schema(description = "处理粮种", example = "稻谷")
    private String grainType;

    @Schema(description = "处理能力(t/h)", example = "8.5")
    private String processCapacity;

    @Schema(description = "降水幅度(%)", example = "4.8")
    private String moistureReduction;
}
