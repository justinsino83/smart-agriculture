package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 能耗记录实体
 */
@Data
@TableName("energy_record")
public class EnergyRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 设备名称 */
    private String device;
    
    /** 能耗类型：电/水 */
    private String type;
    
    /** 用量 */
    private Double energyUsage;
    
    /** 单位：kWh 或 m³ */
    private String unit;
    
    /** 费用(元) */
    private Double cost;
    
    /** 记录时间 */
    private LocalDateTime recordTime;
    
    /** 能效等级 1-5 */
    private Integer efficiency;
    
    /** 备注 */
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}