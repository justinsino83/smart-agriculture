package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "试验田灌溉控制数据")
public class TestFieldIrrigationDTO {

    @Schema(description = "灌溉阀门状态", example = "1号开启")
    private String valveStatus;

    @Schema(description = "瞬时流量(m³/h)", example = "12.4")
    private String instantFlow;

    @Schema(description = "管网压力(MPa)", example = "0.31")
    private String pipePressure;

    @Schema(description = "今日用水(m³)", example = "18.6")
    private String todayWater;
}
