package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 土壤监测数据（时序数据）
 */
@Data
@TableName("soil_data")
public class SoilData {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 传感器ID */
    private Long sensorId;
    
    /** 土壤湿度(%) */
    private Double moisture;
    
    /** 土壤温度(°C) */
    private Double temperature;
    
    /** pH值 */
    private Double ph;
    
    /** EC值(mS/cm) */
    private Double ec;
    
    /** 氮含量(mg/kg) */
    private Double nitrogen;
    
    /** 磷含量(mg/kg) */
    private Double phosphorus;
    
    /** 钾含量(mg/kg) */
    private Double potassium;
    
    /** 采集时间 */
    private LocalDateTime collectTime;
}