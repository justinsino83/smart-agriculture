package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "仓库仓位状态")
public class WarehousePositionDTO {

    @Schema(description = "仓位名称", example = "A区仓位")
    private String name;

    @Schema(description = "仓位范围", example = "1-12号")
    private String range;

    @Schema(description = "状态", example = "normal")
    private String status;

    @Schema(description = "使用率", example = "78%")
    private String usage;
}
