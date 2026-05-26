package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "仓库设备状态")
public class WarehouseDeviceStatusDTO {

    @Schema(description = "设备名称", example = "通风系统")
    private String name;

    @Schema(description = "设备状态", example = "on")
    private String status;
}
