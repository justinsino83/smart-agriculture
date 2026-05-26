package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weiming.smartag.entity.Facility;
import com.weiming.smartag.mapper.FacilityMapper;
import com.weiming.smartag.service.FacilityService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FacilityServiceImpl extends ServiceImpl<FacilityMapper, Facility> implements FacilityService {

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();
        
        List<Facility> allFacilities = list();
        
        long testFieldCount = allFacilities.stream().filter(f -> f.getType() == 1).count();
        long warehouseCount = allFacilities.stream().filter(f -> f.getType() == 2).count();
        long dryingWorkshopCount = allFacilities.stream().filter(f -> f.getType() == 3).count();
        
        result.put("totalCount", allFacilities.size());
        result.put("testFieldCount", testFieldCount);
        result.put("warehouseCount", warehouseCount);
        result.put("dryingWorkshopCount", dryingWorkshopCount);
        result.put("testFields", allFacilities.stream().filter(f -> f.getType() == 1).toList());
        result.put("warehouses", allFacilities.stream().filter(f -> f.getType() == 2).toList());
        result.put("dryingWorkshops", allFacilities.stream().filter(f -> f.getType() == 3).toList());
        
        return result;
    }

    @Override
    public List<Facility> getByType(Integer type) {
        return lambdaQuery().eq(Facility::getType, type).list();
    }
}
