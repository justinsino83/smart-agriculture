package com.weiming.smartag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weiming.smartag.entity.Facility;
import java.util.List;
import java.util.Map;

public interface FacilityService extends IService<Facility> {
    Map<String, Object> getOverview();
    List<Facility> getByType(Integer type);
}
