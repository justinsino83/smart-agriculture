package com.weiming.smartag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("facility_camera")
public class FacilityCamera {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long facilityId;

    private String cameraId;

    private String cameraName;

    private String streamUrl;

    private String position;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
