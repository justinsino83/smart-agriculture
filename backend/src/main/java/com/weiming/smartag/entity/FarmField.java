package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 农田地块实体
 */
@Data
@TableName("farm_field")
public class FarmField {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 地块名称 */
    private String name;
    
    /** 地块编号 */
    private String code;
    
    /** 面积(亩) */
    private Double area;
    
    /** 种植作物 */
    private String crop;
    
    /** 作物生长期 */
    private String growthStage;
    
    /** 负责人 */
    private String manager;
    
    /** 位置坐标(经纬度JSON) */
    private String location;
    
    /** 状态：0-闲置 1-种植中 2-休耕 */
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}