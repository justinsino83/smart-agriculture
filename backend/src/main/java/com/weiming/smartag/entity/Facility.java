package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("facility")
public class Facility {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String code;
    
    private Integer type;
    
    private String location;
    
    private String locationName;
    
    private Double area;
    
    private String manager;
    
    private Integer status;
    
    private String description;

    private String insectDeviceId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
