package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "试验田气象数据")
public class TestFieldWeatherDTO {

    @Schema(description = "风速(m/s)", example = "0.5")
    private String windSpeed;

    @Schema(description = "风向", example = "北")
    private String windDirection;

    @Schema(description = "累计雨量(mm)", example = "102.5")
    private String totalRainfall;

    @Schema(description = "气压(hPa)", example = "1012.7")
    private String pressure;

    @Schema(description = "总辐射(W/㎡)", example = "620.8")
    private String totalRadiation;
}
