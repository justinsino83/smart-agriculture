package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "烘干设备状态")
public class DryingDeviceStatusDTO {

    @Schema(description = "设备名称", example = "提升机")
    private String name;

    @Schema(description = "设备状态", example = "运行中")
    private String status;
}
