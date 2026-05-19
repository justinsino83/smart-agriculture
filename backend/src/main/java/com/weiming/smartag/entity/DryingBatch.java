package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 烘干批次实体
 */
@Data
@TableName("drying_batch")
public class DryingBatch {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 批次号 */
    private String batchNo;
    
    /** 粮食品种 */
    private String grainType;
    
    /** 初始含水率(%) - 同startMoisture */
    private Double initialMoisture;
    
    /** 起始含水率(%) */
    private Double startMoisture;
    
    /** 目标含水率(%) */
    private Double targetMoisture;
    
    /** 当前含水率(%) */
    private Double currentMoisture;
    
    /** 重量(kg) */
    private Double weight;
    
    /** 状态：0-待烘干 1-烘干中 2-暂停 3-已取消 4-已完成 */
    private Integer status;
    
    /** 烘干设备ID */
    private Long deviceId;
    
    /** 开始时间 */
    private LocalDateTime startTime;
    
    /** 结束时间 */
    private LocalDateTime endTime;
    
    /** 烘干时长(分钟) */
    private Integer dryingDuration;
    
    /** 用电量(kWh) */
    private Double powerUsage;
    
    /** 创建人 */
    private String createBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
