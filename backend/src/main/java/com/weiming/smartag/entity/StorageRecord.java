package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 仓储记录实体
 */
@Data
@TableName("storage_record")
public class StorageRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 设施ID */
    private Long facilityId;
    
    /** 批次号 */
    private String batchNo;
    
    /** 粮食品种 */
    private String grainType;
    
    /** 仓库位置 */
    private String warehouse;
    
    /** 数量(吨) */
    private Double quantity;
    
    /** 重量(kg) - 兼容字段 */
    private Double weight;
    
    /** 含水率(%) */
    private Double moisture;
    
    /** 质量等级：1-一等 2-二等 3-三等 */
    private Integer quality;
    
    /** 入库时间 */
    private LocalDateTime entryDate;
    
    /** 出库时间 */
    private LocalDateTime exitDate;
    
    /** 保质期限 */
    private LocalDateTime expireDate;
    
    /** 状态：0-在库 1-出库 */
    private Integer status;
    
    /** 操作人 */
    private String operator;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
