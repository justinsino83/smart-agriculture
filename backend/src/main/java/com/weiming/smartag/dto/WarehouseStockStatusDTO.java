package com.weiming.smartag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "仓库库存状态")
public class WarehouseStockStatusDTO {

    @Schema(description = "粮食品种", example = "稻谷")
    private String grainType;

    @Schema(description = "库存重量(t)", example = "286")
    private String stockWeight;

    @Schema(description = "入库批次", example = "8")
    private String inboundBatches;

    @Schema(description = "异常告警", example = "0条")
    private String abnormalAlerts;
}
