package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "烘干能耗数据")
public class DryingEnergyDTO {

    @Schema(description = "瞬时功率(kW)", example = "42.6")
    private Object instantPower;

    @Schema(description = "今日用电量(kWh)", example = "318")
    private Object todayPower;

    @Schema(description = "燃气流量(m³)", example = "21.4")
    private Object gasFlow;
}
