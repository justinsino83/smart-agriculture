package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "试验田传感器数据")
public class TestFieldSensorDataDTO {

    @Schema(description = "空气温度(°C)", example = "16.75")
    private Object airTemp;

    @Schema(description = "空气湿度(%)", example = "68")
    private Object airHumidity;

    @Schema(description = "光照强度(Lux)", example = "83909")
    private Object lightIntensity;

    @Schema(description = "CO₂浓度(ppm)", example = "421")
    private Object co2Concentration;

    @Schema(description = "土壤温度(°C)", example = "10.89")
    private Object soilTemp;

    @Schema(description = "土壤湿度(%)", example = "26.57")
    private Object soilHumidity;

    @Schema(description = "土壤pH", example = "6.8")
    private Object soilPh;

    @Schema(description = "试验田气象数据")
    private TestFieldWeatherDTO weather;

    @Schema(description = "灌溉控制数据")
    private TestFieldIrrigationDTO irrigation;
}
