package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weiming.smartag.entity.DevicePushData;
import com.weiming.smartag.mapper.DevicePushDataMapper;
import com.weiming.smartag.service.DevicePushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备推送数据服务实现
 */
@Slf4j
@Service
public class DevicePushServiceImpl extends ServiceImpl<DevicePushDataMapper, DevicePushData> 
        implements DevicePushService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean savePushData(DevicePushData data) {
        try {
            if (data.getCreateTime() == null) {
                data.setCreateTime(LocalDateTime.now());
            }
            
            // 数据去重逻辑：全量比对判断是否已存在相同数据（detectedTime每次不同，不参与比对）
            if (data.getClientId() != null) {
                LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DevicePushData::getClientId, data.getClientId());
                
                // 添加所有环境数据字段比对
                addDataFieldEq(wrapper, data);
                
                // 查询是否已存在完全相同的数据
                DevicePushData existData = getOne(wrapper);
                
                if (existData != null) {
                    // 业务数据相同但时间不同，更新时间字段
                    existData.setDetectedTime(data.getDetectedTime());
                    existData.setCreateTime(LocalDateTime.now());
                    log.info("业务数据相同，更新时间字段, clientId: {}, oldTime: {}, newTime: {}", 
                             data.getClientId(), existData.getDetectedTime(), data.getDetectedTime());
                    return updateById(existData);
                }
            }
            
            // 数据不存在或不完全相同，执行新增操作
            log.info("执行新增数据, clientId: {}, detectedTime: {}", 
                     data.getClientId(), data.getDetectedTime());
            return save(data);
        } catch (Exception e) {
            log.error("保存推送数据失败", e);
            throw new RuntimeException("数据保存失败", e);
        }
    }

    /**
     * 添加所有数据字段的等值条件到查询Wrapper
     */
    private void addDataFieldEq(LambdaQueryWrapper<DevicePushData> wrapper, DevicePushData data) {
        // 环境数据
        wrapper.eq(data.getAmbientTemperature() != null, DevicePushData::getAmbientTemperature, data.getAmbientTemperature());
        wrapper.eq(data.getAmbientHumidity() != null, DevicePushData::getAmbientHumidity, data.getAmbientHumidity());
        wrapper.eq(data.getPressure() != null, DevicePushData::getPressure, data.getPressure());
        wrapper.eq(data.getWindSpeed() != null, DevicePushData::getWindSpeed, data.getWindSpeed());
        wrapper.eq(data.getWindDirection() != null, DevicePushData::getWindDirection, data.getWindDirection());
        wrapper.eq(data.getWindScale() != null, DevicePushData::getWindScale, data.getWindScale());
        wrapper.eq(data.getRainfall() != null, DevicePushData::getRainfall, data.getRainfall());
        wrapper.eq(data.getEvaporation() != null, DevicePushData::getEvaporation, data.getEvaporation());
        wrapper.eq(data.getTotalRadiation() != null, DevicePushData::getTotalRadiation, data.getTotalRadiation());
        wrapper.eq(data.getRssi() != null, DevicePushData::getRssi, data.getRssi());
        
        // 土壤数据
        wrapper.eq(data.getSoilTemp() != null, DevicePushData::getSoilTemp, data.getSoilTemp());
        wrapper.eq(data.getSoilHumi() != null, DevicePushData::getSoilHumi, data.getSoilHumi());
        wrapper.eq(data.getSoilCond() != null, DevicePushData::getSoilCond, data.getSoilCond());
        wrapper.eq(data.getSoilPh() != null, DevicePushData::getSoilPh, data.getSoilPh());
        wrapper.eq(data.getSoilTemp1() != null, DevicePushData::getSoilTemp1, data.getSoilTemp1());
        wrapper.eq(data.getSoilTemp2() != null, DevicePushData::getSoilTemp2, data.getSoilTemp2());
        wrapper.eq(data.getSoilTemp3() != null, DevicePushData::getSoilTemp3, data.getSoilTemp3());
        wrapper.eq(data.getSoilTemp4() != null, DevicePushData::getSoilTemp4, data.getSoilTemp4());
        wrapper.eq(data.getSoilTemp5() != null, DevicePushData::getSoilTemp5, data.getSoilTemp5());
        wrapper.eq(data.getSoilTemp6() != null, DevicePushData::getSoilTemp6, data.getSoilTemp6());
        wrapper.eq(data.getSoilTemp7() != null, DevicePushData::getSoilTemp7, data.getSoilTemp7());
        wrapper.eq(data.getSoilTemp8() != null, DevicePushData::getSoilTemp8, data.getSoilTemp8());
        wrapper.eq(data.getSoilTemp9() != null, DevicePushData::getSoilTemp9, data.getSoilTemp9());
        wrapper.eq(data.getSoilTemp10() != null, DevicePushData::getSoilTemp10, data.getSoilTemp10());
        wrapper.eq(data.getSoilTemp11() != null, DevicePushData::getSoilTemp11, data.getSoilTemp11());
        wrapper.eq(data.getSoilTemp12() != null, DevicePushData::getSoilTemp12, data.getSoilTemp12());
        wrapper.eq(data.getSoilTemp13() != null, DevicePushData::getSoilTemp13, data.getSoilTemp13());
        wrapper.eq(data.getSoilTemp14() != null, DevicePushData::getSoilTemp14, data.getSoilTemp14());
        wrapper.eq(data.getSoilTemp15() != null, DevicePushData::getSoilTemp15, data.getSoilTemp15());
        wrapper.eq(data.getSoilHumi1() != null, DevicePushData::getSoilHumi1, data.getSoilHumi1());
        wrapper.eq(data.getSoilHumi2() != null, DevicePushData::getSoilHumi2, data.getSoilHumi2());
        wrapper.eq(data.getSoilHumi3() != null, DevicePushData::getSoilHumi3, data.getSoilHumi3());
        wrapper.eq(data.getSoilHumi4() != null, DevicePushData::getSoilHumi4, data.getSoilHumi4());
        wrapper.eq(data.getSoilHumi5() != null, DevicePushData::getSoilHumi5, data.getSoilHumi5());
        wrapper.eq(data.getSoilHumi6() != null, DevicePushData::getSoilHumi6, data.getSoilHumi6());
        wrapper.eq(data.getSoilHumi7() != null, DevicePushData::getSoilHumi7, data.getSoilHumi7());
        wrapper.eq(data.getSoilHumi8() != null, DevicePushData::getSoilHumi8, data.getSoilHumi8());
        wrapper.eq(data.getSoilHumi9() != null, DevicePushData::getSoilHumi9, data.getSoilHumi9());
        wrapper.eq(data.getSoilHumi10() != null, DevicePushData::getSoilHumi10, data.getSoilHumi10());
        wrapper.eq(data.getSoilHumi11() != null, DevicePushData::getSoilHumi11, data.getSoilHumi11());
        wrapper.eq(data.getSoilHumi12() != null, DevicePushData::getSoilHumi12, data.getSoilHumi12());
        wrapper.eq(data.getSoilCond1() != null, DevicePushData::getSoilCond1, data.getSoilCond1());
        wrapper.eq(data.getSoilCond2() != null, DevicePushData::getSoilCond2, data.getSoilCond2());
        wrapper.eq(data.getSoilCond3() != null, DevicePushData::getSoilCond3, data.getSoilCond3());
        wrapper.eq(data.getSoilCond4() != null, DevicePushData::getSoilCond4, data.getSoilCond4());
        wrapper.eq(data.getSoilCond5() != null, DevicePushData::getSoilCond5, data.getSoilCond5());
        wrapper.eq(data.getSoilCond6() != null, DevicePushData::getSoilCond6, data.getSoilCond6());
        wrapper.eq(data.getSoilCond7() != null, DevicePushData::getSoilCond7, data.getSoilCond7());
        wrapper.eq(data.getSoilCond8() != null, DevicePushData::getSoilCond8, data.getSoilCond8());
        wrapper.eq(data.getSoilCond9() != null, DevicePushData::getSoilCond9, data.getSoilCond9());
        wrapper.eq(data.getSoilCond10() != null, DevicePushData::getSoilCond10, data.getSoilCond10());
        wrapper.eq(data.getSoilCond11() != null, DevicePushData::getSoilCond11, data.getSoilCond11());
        wrapper.eq(data.getSoilCond12() != null, DevicePushData::getSoilCond12, data.getSoilCond12());
        
        // 水质数据
        wrapper.eq(data.getLevel() != null, DevicePushData::getLevel, data.getLevel());
        wrapper.eq(data.getWaterTemperature() != null, DevicePushData::getWaterTemperature, data.getWaterTemperature());
        wrapper.eq(data.getWaterPh() != null, DevicePushData::getWaterPh, data.getWaterPh());
        wrapper.eq(data.getWaterConductivity() != null, DevicePushData::getWaterConductivity, data.getWaterConductivity());
        wrapper.eq(data.getWaterOrp() != null, DevicePushData::getWaterOrp, data.getWaterOrp());
        wrapper.eq(data.getTurbidity() != null, DevicePushData::getTurbidity, data.getTurbidity());
        wrapper.eq(data.getDissolvedOxygen() != null, DevicePushData::getDissolvedOxygen, data.getDissolvedOxygen());
        wrapper.eq(data.getAmmonia() != null, DevicePushData::getAmmonia, data.getAmmonia());
        
        // 气象数据
        wrapper.eq(data.getLightIntensity() != null, DevicePushData::getLightIntensity, data.getLightIntensity());
        wrapper.eq(data.getSunHours() != null, DevicePushData::getSunHours, data.getSunHours());
        wrapper.eq(data.getUltravioletRays() != null, DevicePushData::getUltravioletRays, data.getUltravioletRays());
        wrapper.eq(data.getNetRadiation() != null, DevicePushData::getNetRadiation, data.getNetRadiation());
        wrapper.eq(data.getDewTemp() != null, DevicePushData::getDewTemp, data.getDewTemp());
        wrapper.eq(data.getVisibility() != null, DevicePushData::getVisibility, data.getVisibility());
        
        // 空气质量
        wrapper.eq(data.getPm25() != null, DevicePushData::getPm25, data.getPm25());
        wrapper.eq(data.getPm10() != null, DevicePushData::getPm10, data.getPm10());
        wrapper.eq(data.getTsp() != null, DevicePushData::getTsp, data.getTsp());
        wrapper.eq(data.getCo2() != null, DevicePushData::getCo2, data.getCo2());
        wrapper.eq(data.getCo() != null, DevicePushData::getCo, data.getCo());
        wrapper.eq(data.getSo2() != null, DevicePushData::getSo2, data.getSo2());
        wrapper.eq(data.getNo2() != null, DevicePushData::getNo2, data.getNo2());
        wrapper.eq(data.getO3() != null, DevicePushData::getO3, data.getO3());
        wrapper.eq(data.getOxygenContent() != null, DevicePushData::getOxygenContent, data.getOxygenContent());
        wrapper.eq(data.getTvoc() != null, DevicePushData::getTvoc, data.getTvoc());
        wrapper.eq(data.getNoise() != null, DevicePushData::getNoise, data.getNoise());
        wrapper.eq(data.getNegativeOxygen() != null, DevicePushData::getNegativeOxygen, data.getNegativeOxygen());
        
        // 其他数据
        wrapper.eq(data.getRainOrSnow() != null, DevicePushData::getRainOrSnow, data.getRainOrSnow());
        wrapper.eq(data.getAltitude() != null, DevicePushData::getAltitude, data.getAltitude());
        wrapper.eq(data.getLongitude() != null, DevicePushData::getLongitude, data.getLongitude());
        wrapper.eq(data.getLatitude() != null, DevicePushData::getLatitude, data.getLatitude());
        wrapper.eq(data.getVoltage() != null, DevicePushData::getVoltage, data.getVoltage());
        wrapper.eq(data.getSolarVoltage() != null, DevicePushData::getSolarVoltage, data.getSolarVoltage());
        wrapper.eq(data.getFlow() != null, DevicePushData::getFlow, data.getFlow());
        wrapper.eq(data.getFlows() != null, DevicePushData::getFlows, data.getFlows());
        wrapper.eq(data.getNitrogen() != null, DevicePushData::getNitrogen, data.getNitrogen());
        wrapper.eq(data.getPhosphorus() != null, DevicePushData::getPhosphorus, data.getPhosphorus());
        wrapper.eq(data.getPotassium() != null, DevicePushData::getPotassium, data.getPotassium());
        wrapper.eq(data.getPhotosynthesis() != null, DevicePushData::getPhotosynthesis, data.getPhotosynthesis());
        wrapper.eq(data.getLeafHumidity() != null, DevicePushData::getLeafHumidity, data.getLeafHumidity());
        wrapper.eq(data.getLeafTemperature() != null, DevicePushData::getLeafTemperature, data.getLeafTemperature());
        wrapper.eq(data.getHeatFlux() != null, DevicePushData::getHeatFlux, data.getHeatFlux());
    }

    @Override
    public DevicePushData getLatestData(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return null;
        }
        
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DevicePushData::getClientId, clientId)
               .orderByDesc(DevicePushData::getCreateTime)
               .last("LIMIT 1");
        
        return getOne(wrapper);
    }

    @Override
    public Map<String, Object> getHistoryData(String clientId, LocalDateTime startTime, 
                                              LocalDateTime endTime, int page, int size) {
        
        // 边界条件处理
        int validPage = page;
        int validSize = size;
        
        // 页码最小为1
        if (validPage < 1) {
            log.warn("页码参数过小({})，调整为默认值1", validPage);
            validPage = 1;
        }
        
        // 每页条数限制在1-100之间
        if (validSize < 1) {
            log.warn("每页条数参数过小({})，调整为默认值20", validSize);
            validSize = 20;
        }
        if (validSize > 100) {
            log.warn("每页条数参数过大({})，调整为最大值100", validSize);
            validSize = 100;
        }
        
        log.info("查询历史数据 - clientId: {}, startTime: {}, endTime: {}, page: {}, size: {}", 
                 clientId, startTime, endTime, validPage, validSize);
        
        Page<DevicePushData> pageObj = new Page<>(validPage, validSize);
        
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(clientId)) {
            wrapper.eq(DevicePushData::getClientId, clientId);
        }
        
        if (startTime != null) {
            wrapper.ge(DevicePushData::getCreateTime, startTime);
        }
        
        if (endTime != null) {
            wrapper.le(DevicePushData::getCreateTime, endTime);
        }
        
        wrapper.orderByDesc(DevicePushData::getCreateTime);
        
        Page<DevicePushData> result = page(pageObj, wrapper);
        
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("pages", result.getPages());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        
        log.info("查询完成 - total: {}, pages: {}, 当前页记录数: {}", 
                 result.getTotal(), result.getPages(), result.getRecords().size());
        
        return data;
    }

    @Override
    public Map<String, Object> getStatistics(String clientId) {
        Map<String, Object> stats = new HashMap<>();
        
        if (StringUtils.hasText(clientId)) {
            LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DevicePushData::getClientId, clientId);
            
            long totalCount = count(wrapper);
            stats.put("totalRecords", totalCount);
            
            // 获取最新数据用于计算
            DevicePushData latest = getLatestData(clientId);
            if (latest != null) {
                stats.put("latestData", latest);
                stats.put("lastUpdateTime", latest.getCreateTime());
            }
            
            // 计算24小时内的数据量
            wrapper.ge(DevicePushData::getCreateTime, LocalDateTime.now().minusHours(24));
            long todayCount = count(wrapper);
            stats.put("todayRecords", todayCount);
        } else {
            // 全局统计
            stats.put("totalRecords", count());
            
            // 获取活跃设备数（最近24小时有数据的）
            LambdaQueryWrapper<DevicePushData> activeWrapper = new LambdaQueryWrapper<>();
            activeWrapper.ge(DevicePushData::getCreateTime, LocalDateTime.now().minusHours(24))
                        .select(DevicePushData::getClientId)
                        .groupBy(DevicePushData::getClientId);
            List<DevicePushData> activeDevices = list(activeWrapper);
            stats.put("activeDevices", activeDevices.size());
        }
        
        return stats;
    }

    @Override
    public Map<String, List<Object>> getTrendData(String clientId, int hours) {
        Map<String, List<Object>> trend = new HashMap<>();
        
        if (!StringUtils.hasText(clientId)) {
            return trend;
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hours);
        
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DevicePushData::getClientId, clientId)
               .ge(DevicePushData::getCreateTime, startTime)
               .le(DevicePushData::getCreateTime, endTime)
               .orderByAsc(DevicePushData::getCreateTime);
        
        List<DevicePushData> dataList = list(wrapper);
        
        List<Object> times = new ArrayList<>();
        List<Object> temps = new ArrayList<>();
        List<Object> humis = new ArrayList<>();
        List<Object> soilTemps = new ArrayList<>();
        List<Object> soilHumis = new ArrayList<>();
        
        for (DevicePushData data : dataList) {
            times.add(data.getCreateTime());
            temps.add(data.getAmbientTemperature());
            humis.add(data.getAmbientHumidity());
            soilTemps.add(data.getSoilTemp());
            soilHumis.add(data.getSoilHumi());
        }
        
        trend.put("times", times);
        trend.put("temperatures", temps);
        trend.put("humidities", humis);
        trend.put("soilTemperatures", soilTemps);
        trend.put("soilHumidities", soilHumis);
        
        return trend;
    }

    @Override
    public List<Map<String, Object>> getActiveDevices() {
        LocalDateTime activeTime = LocalDateTime.now().minusHours(24);
        
        // 1. 先获取所有在活跃时间内有数据的设备ID
        LambdaQueryWrapper<DevicePushData> clientWrapper = new LambdaQueryWrapper<>();
        clientWrapper.ge(DevicePushData::getCreateTime, activeTime)
                     .select(DevicePushData::getClientId)
                     .groupBy(DevicePushData::getClientId);
        
        List<DevicePushData> clientIds = list(clientWrapper);
        
        // 2. 对每个设备分别获取其最新的数据
        List<Map<String, Object>> devices = new ArrayList<>();
        for (DevicePushData clientData : clientIds) {
            String clientId = clientData.getClientId();
            DevicePushData latestData = getLatestData(clientId);
            
            if (latestData != null) {
                Map<String, Object> device = new HashMap<>();
                device.put("clientId", clientId);
                device.put("detectedTime", latestData.getDetectedTime());
                device.put("lastSeen", latestData.getCreateTime());
                device.put("rssi", latestData.getRssi());
                device.put("latitude", latestData.getLatitude());
                device.put("longitude", latestData.getLongitude());
                device.put("voltage", latestData.getVoltage());
                device.put("status", "online");
                // 环境数据
                device.put("ambientTemperature", latestData.getAmbientTemperature());
                device.put("ambientHumidity", latestData.getAmbientHumidity());
                device.put("pressure", latestData.getPressure());
                device.put("windSpeed", latestData.getWindSpeed());
                device.put("windDirection", latestData.getWindDirection());
                device.put("rainfall", latestData.getRainfall());
                device.put("lightIntensity", latestData.getLightIntensity());
                device.put("dewTemp", latestData.getDewTemp());
                // 土壤数据
                device.put("soilTemp", latestData.getSoilTemp());
                device.put("soilHumi", latestData.getSoilHumi());
                device.put("soilCond", latestData.getSoilCond());
                device.put("soilPh", latestData.getSoilPh());
                // 其他数据
                device.put("co2", latestData.getCo2());
                devices.add(device);
            }
        }
        
        return devices;
    }

    @Override
    public Map<String, Object> getDashboardOverview(String clientId) {
        Map<String, Object> overview = new HashMap<>();
        
        // 1. 获取综合统计（支持按设备过滤）
        Map<String, Object> stats = getStatistics(clientId);
        overview.putAll(stats);
        
        // 2. 获取设备最新数据（支持按设备过滤）
        List<Map<String, Object>> activeDevices = getActiveDevicesWithFilter(clientId);
        overview.put("devices", activeDevices);
        
        // 3. 获取最新环境数据（支持按设备过滤）
        Map<String, Object> envData = getAverageEnvironmentDataWithFilter(clientId);
        overview.put("environment", envData);
        
        // 4. 获取设备状态分布（支持按设备过滤）
        Map<String, Object> statusDistribution = getDeviceStatusDistributionWithFilter(clientId);
        overview.put("statusDistribution", statusDistribution);
        
        // 5. 获取告警信息（支持按设备过滤）
        List<Map<String, Object>> alerts = generateAlertsWithFilter(clientId);
        overview.put("alerts", alerts);
        
        // 6. 获取最新一条设备数据作为示例展示（支持按设备过滤）
        DevicePushData latestRecord = getLatestRecordWithFilter(clientId);
        overview.put("latestRecord", latestRecord);
        
        return overview;
    }
    
    /**
     * 获取最近一条记录
     */
    private DevicePushData getLatestRecord() {
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(DevicePushData::getCreateTime)
               .last("LIMIT 1");
        return getOne(wrapper);
    }
    
    /**
     * 获取平均环境数据
     */
    private Map<String, Object> getAverageEnvironmentData() {
        Map<String, Object> result = new HashMap<>();
        
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(DevicePushData::getCreateTime, LocalDateTime.now().minusHours(1))
               .orderByDesc(DevicePushData::getCreateTime)
               .last("LIMIT 100");
        
        List<DevicePushData> recentData = list(wrapper);
        
        if (recentData.isEmpty()) {
            return result;
        }
        
        BigDecimal tempSum = BigDecimal.ZERO;
        BigDecimal humiSum = BigDecimal.ZERO;
        BigDecimal soilPhSum = BigDecimal.ZERO;
        BigDecimal soilCondSum = BigDecimal.ZERO;
        BigDecimal lightSum = BigDecimal.ZERO;
        BigDecimal co2Sum = BigDecimal.ZERO;
        int count = 0;
        
        for (DevicePushData data : recentData) {
            if (data.getAmbientTemperature() != null) {
                tempSum = tempSum.add(data.getAmbientTemperature());
            }
            if (data.getAmbientHumidity() != null) {
                humiSum = humiSum.add(data.getAmbientHumidity());
            }
            if (data.getSoilPh() != null) {
                soilPhSum = soilPhSum.add(data.getSoilPh());
            }
            if (data.getSoilCond() != null) {
                soilCondSum = soilCondSum.add(data.getSoilCond());
            }
            if (data.getLightIntensity() != null) {
                lightSum = lightSum.add(data.getLightIntensity());
            }
            if (data.getCo2() != null) {
                co2Sum = co2Sum.add(data.getCo2());
            }
            count++;
        }
        
        if (count > 0) {
            result.put("avgTemperature", tempSum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP));
            result.put("avgHumidity", humiSum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP));
            result.put("avgSoilPH", soilPhSum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
            // 将 uS/cm 转换为 mS/cm (除以 1000)
            result.put("avgSoilEC", soilCondSum.divide(BigDecimal.valueOf(count * 1000), 3, RoundingMode.HALF_UP));
            result.put("avgLight", lightSum.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP));
            result.put("avgCO2", co2Sum.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP));
        }
        
        return result;
    }
    
    /**
     * 获取设备状态分布
     */
    private Map<String, Object> getDeviceStatusDistribution() {
        Map<String, Object> dist = new HashMap<>();
        
        List<Map<String, Object>> devices = getActiveDevices();
        int total = devices.size();
        int online = 0;
        int warning = 0;
        
        for (Map<String, Object> device : devices) {
            Object rssi = device.get("rssi");
            if (rssi instanceof Number) {
                int signal = ((Number) rssi).intValue();
                if (signal > -60) {
                    online++;
                } else if (signal > -80) {
                    warning++;
                }
            }
        }
        
        dist.put("online", online);
        dist.put("warning", warning);
        dist.put("offline", Math.max(0, total - online - warning));
        dist.put("total", total);
        
        return dist;
    }
    
    /**
     * 生成告警信息
     */
    private List<Map<String, Object>> generateAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        // 检查最近数据是否有异常
        LocalDateTime checkTime = LocalDateTime.now().minusHours(1);
        
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(DevicePushData::getCreateTime, checkTime);
        
        List<DevicePushData> recentData = list(wrapper);
        
        for (DevicePushData data : recentData) {
            // 检查温度异常
            if (data.getAmbientTemperature() != null && 
                (data.getAmbientTemperature().compareTo(new BigDecimal("35")) > 0 ||
                 data.getAmbientTemperature().compareTo(new BigDecimal("5")) < 0)) {
                addAlert(alerts, data.getClientId(), "temperature", 
                        "温度异常: " + data.getAmbientTemperature() + "°C", "warning");
            }
            
            // 检查土壤湿度过低
            if (data.getSoilHumi() != null && 
                data.getSoilHumi().compareTo(new BigDecimal("20")) < 0) {
                addAlert(alerts, data.getClientId(), "soil_humidity", 
                        "土壤湿度过低: " + data.getSoilHumi() + "%", "danger");
            }
            
            // 检查信号弱
            if (data.getRssi() != null && data.getRssi() < -85) {
                addAlert(alerts, data.getClientId(), "signal", 
                        "信号弱: RSSI " + data.getRssi() + "dBm", "warning");
            }
        }
        
        // 限制告警数量
        if (alerts.size() > 10) {
            alerts = alerts.subList(0, 10);
        }
        
        return alerts;
    }
    
    private void addAlert(List<Map<String, Object>> alerts, String clientId, 
                         String type, String message, String level) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("clientId", clientId);
        alert.put("type", type);
        alert.put("message", message);
        alert.put("level", level);
        alert.put("time", LocalDateTime.now());
        alerts.add(alert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deduplicateHistoryData() {
        log.info("开始执行历史数据去重...");
        
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 统计去重前的数据量
            long totalBefore = count();
            result.put("totalBefore", totalBefore);
            log.info("去重前总记录数：{}", totalBefore);
            
            // 2. 分批次处理去重（防止内存溢出）
            int pageSize = 1000;
            int currentPage = 1;
            int totalDeleted = 0;
            
            // 先按clientId分组，找到需要保留的记录ID
            Set<Long> idsToKeep = new HashSet<>();
            Map<String, Long> dataKeyToMaxId = new HashMap<>();
            List<Long> allIds = new ArrayList<>(); // 收集所有ID
            int duplicateCount = 0; // 统计发现的重复数
            
            // 分页查询所有数据
            Page<DevicePushData> pageResult;
            do {
                Page<DevicePushData> page = new Page<>(currentPage, pageSize);
                pageResult = page(page, new LambdaQueryWrapper<DevicePushData>()
                        .orderByAsc(DevicePushData::getClientId)
                        .orderByAsc(DevicePushData::getId));
                
                for (DevicePushData data : pageResult.getRecords()) {
                    allIds.add(data.getId());
                    
                    // 生成数据特征key（所有业务字段组合）
                    String dataKey = generateDataKey(data);
                    
                    if (dataKey != null) {
                        String groupKey = data.getClientId() + "_" + dataKey;
                        
                        if (dataKeyToMaxId.containsKey(groupKey)) {
                            // 已存在相同数据，保留ID大的
                            Long currentMaxId = dataKeyToMaxId.get(groupKey);
                            duplicateCount++;
                            if (data.getId() > currentMaxId) {
                                idsToKeep.remove(currentMaxId); // 旧ID不需要保留
                                idsToKeep.add(data.getId()); // 新ID加入保留列表
                                dataKeyToMaxId.put(groupKey, data.getId());
                            }
                            // 否则，新ID比旧ID小，不保留
                        } else {
                            // 第一次出现，保留这个ID
                            idsToKeep.add(data.getId());
                            dataKeyToMaxId.put(groupKey, data.getId());
                        }
                    }
                }
                
                currentPage++;
            } while (currentPage <= pageResult.getPages());
            
            log.info("发现重复数据次数：{}", duplicateCount);
            log.info("需要保留的记录数：{}", idsToKeep.size());
            
            // 3. 删除不在idsToKeep中的记录
            List<Long> idsToDelete = allIds.stream()
                    .filter(id -> !idsToKeep.contains(id))
                    .collect(Collectors.toList());
            
            log.info("需要删除的记录数：{}", idsToDelete.size());
            
            if (!idsToDelete.isEmpty()) {
                // 分批删除
                int deleteBatchSize = 500;
                for (int i = 0; i < idsToDelete.size(); i += deleteBatchSize) {
                    int end = Math.min(i + deleteBatchSize, idsToDelete.size());
                    List<Long> batchIds = idsToDelete.subList(i, end);
                    removeByIds(batchIds);
                    totalDeleted += batchIds.size();
                    log.info("已删除 {}/{} 条", totalDeleted, idsToDelete.size());
                }
            }
            
            // 4. 统计去重后的数据量
            long totalAfter = count();
            result.put("totalAfter", totalAfter);
            result.put("deletedCount", totalDeleted);
            result.put("success", true);
            
            long endTime = System.currentTimeMillis();
            result.put("costTimeMs", endTime - startTime);
            
            log.info("历史数据去重完成，删除{}条重复记录，耗时{}ms", 
                     totalDeleted, (endTime - startTime));
            
        } catch (Exception e) {
            log.error("历史数据去重失败", e);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 生成数据特征key（用于判断数据是否相同）
     * 不包含：id, detectedTime, createTime
     */
    private String generateDataKey(DevicePushData data) {
        StringBuilder sb = new StringBuilder();
        // 环境数据
        appendValue(sb, data.getAmbientTemperature());
        appendValue(sb, data.getAmbientHumidity());
        appendValue(sb, data.getPressure());
        appendValue(sb, data.getWindSpeed());
        appendValue(sb, data.getWindDirection());
        appendValue(sb, data.getWindScale());
        appendValue(sb, data.getRainfall());
        appendValue(sb, data.getEvaporation());
        appendValue(sb, data.getTotalRadiation());
        appendValue(sb, data.getRssi());
        // 土壤数据
        appendValue(sb, data.getSoilTemp());
        appendValue(sb, data.getSoilHumi());
        appendValue(sb, data.getSoilCond());
        appendValue(sb, data.getSoilPh());
        appendValue(sb, data.getSoilTemp1());
        appendValue(sb, data.getSoilTemp2());
        appendValue(sb, data.getSoilTemp3());
        appendValue(sb, data.getSoilTemp4());
        appendValue(sb, data.getSoilTemp5());
        appendValue(sb, data.getSoilTemp6());
        appendValue(sb, data.getSoilTemp7());
        appendValue(sb, data.getSoilTemp8());
        appendValue(sb, data.getSoilTemp9());
        appendValue(sb, data.getSoilTemp10());
        appendValue(sb, data.getSoilTemp11());
        appendValue(sb, data.getSoilTemp12());
        appendValue(sb, data.getSoilTemp13());
        appendValue(sb, data.getSoilTemp14());
        appendValue(sb, data.getSoilTemp15());
        appendValue(sb, data.getSoilHumi1());
        appendValue(sb, data.getSoilHumi2());
        appendValue(sb, data.getSoilHumi3());
        appendValue(sb, data.getSoilHumi4());
        appendValue(sb, data.getSoilHumi5());
        appendValue(sb, data.getSoilHumi6());
        appendValue(sb, data.getSoilHumi7());
        appendValue(sb, data.getSoilHumi8());
        appendValue(sb, data.getSoilHumi9());
        appendValue(sb, data.getSoilHumi10());
        appendValue(sb, data.getSoilHumi11());
        appendValue(sb, data.getSoilHumi12());
        appendValue(sb, data.getSoilCond1());
        appendValue(sb, data.getSoilCond2());
        appendValue(sb, data.getSoilCond3());
        appendValue(sb, data.getSoilCond4());
        appendValue(sb, data.getSoilCond5());
        appendValue(sb, data.getSoilCond6());
        appendValue(sb, data.getSoilCond7());
        appendValue(sb, data.getSoilCond8());
        appendValue(sb, data.getSoilCond9());
        appendValue(sb, data.getSoilCond10());
        appendValue(sb, data.getSoilCond11());
        appendValue(sb, data.getSoilCond12());
        // 水质数据
        appendValue(sb, data.getLevel());
        appendValue(sb, data.getWaterTemperature());
        appendValue(sb, data.getWaterPh());
        appendValue(sb, data.getWaterConductivity());
        appendValue(sb, data.getWaterOrp());
        appendValue(sb, data.getTurbidity());
        appendValue(sb, data.getDissolvedOxygen());
        appendValue(sb, data.getAmmonia());
        // 气象数据
        appendValue(sb, data.getLightIntensity());
        appendValue(sb, data.getSunHours());
        appendValue(sb, data.getUltravioletRays());
        appendValue(sb, data.getNetRadiation());
        appendValue(sb, data.getDewTemp());
        appendValue(sb, data.getVisibility());
        // 空气质量
        appendValue(sb, data.getPm25());
        appendValue(sb, data.getPm10());
        appendValue(sb, data.getTsp());
        appendValue(sb, data.getCo2());
        appendValue(sb, data.getCo());
        appendValue(sb, data.getSo2());
        appendValue(sb, data.getNo2());
        appendValue(sb, data.getO3());
        appendValue(sb, data.getOxygenContent());
        appendValue(sb, data.getTvoc());
        appendValue(sb, data.getNoise());
        appendValue(sb, data.getNegativeOxygen());
        // 其他数据
        appendValue(sb, data.getRainOrSnow());
        appendValue(sb, data.getAltitude());
        appendValue(sb, data.getLongitude());
        appendValue(sb, data.getLatitude());
        appendValue(sb, data.getVoltage());
        appendValue(sb, data.getSolarVoltage());
        appendValue(sb, data.getFlow());
        appendValue(sb, data.getFlows());
        appendValue(sb, data.getNitrogen());
        appendValue(sb, data.getPhosphorus());
        appendValue(sb, data.getPotassium());
        appendValue(sb, data.getPhotosynthesis());
        appendValue(sb, data.getLeafHumidity());
        appendValue(sb, data.getLeafTemperature());
        appendValue(sb, data.getHeatFlux());
        
        return sb.toString();
    }
    
    private void appendValue(StringBuilder sb, Object value) {
        sb.append(value == null ? "NULL" : value.toString()).append("|");
    }
    
    /**
     * 获取活跃设备列表（支持按设备过滤）
     */
    private List<Map<String, Object>> getActiveDevicesWithFilter(String clientId) {
        LocalDateTime activeTime = LocalDateTime.now().minusHours(24);
        
        // 1. 先获取所有在活跃时间内有数据的设备ID
        LambdaQueryWrapper<DevicePushData> clientWrapper = new LambdaQueryWrapper<>();
        clientWrapper.ge(DevicePushData::getCreateTime, activeTime)
                     .select(DevicePushData::getClientId)
                     .groupBy(DevicePushData::getClientId);
        
        // 如果指定了clientId，只查指定设备
        if (StringUtils.hasText(clientId)) {
            clientWrapper.eq(DevicePushData::getClientId, clientId);
        }
        
        List<DevicePushData> clientIds = list(clientWrapper);
        
        // 2. 对每个设备分别获取其最新的数据
        List<Map<String, Object>> devices = new ArrayList<>();
        for (DevicePushData clientData : clientIds) {
            String cId = clientData.getClientId();
            DevicePushData latestData = getLatestData(cId);
            
            if (latestData != null) {
                Map<String, Object> device = new HashMap<>();
                device.put("clientId", cId);
                device.put("detectedTime", latestData.getDetectedTime());
                device.put("lastSeen", latestData.getCreateTime());
                device.put("rssi", latestData.getRssi());
                device.put("latitude", latestData.getLatitude());
                device.put("longitude", latestData.getLongitude());
                device.put("voltage", latestData.getVoltage());
                device.put("status", "online");
                // 环境数据
                device.put("ambientTemperature", latestData.getAmbientTemperature());
                device.put("ambientHumidity", latestData.getAmbientHumidity());
                device.put("pressure", latestData.getPressure());
                device.put("windSpeed", latestData.getWindSpeed());
                device.put("windDirection", latestData.getWindDirection());
                device.put("rainfall", latestData.getRainfall());
                device.put("lightIntensity", latestData.getLightIntensity());
                device.put("dewTemp", latestData.getDewTemp());
                // 土壤数据
                device.put("soilTemp", latestData.getSoilTemp());
                device.put("soilHumi", latestData.getSoilHumi());
                device.put("soilCond", latestData.getSoilCond());
                device.put("soilPh", latestData.getSoilPh());
                // 其他数据
                device.put("co2", latestData.getCo2());
                devices.add(device);
            }
        }
        
        return devices;
    }
    
    /**
     * 获取平均环境数据（支持按设备过滤）
     */
    private Map<String, Object> getAverageEnvironmentDataWithFilter(String clientId) {
        Map<String, Object> result = new HashMap<>();
        
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(DevicePushData::getCreateTime, LocalDateTime.now().minusHours(1))
               .orderByDesc(DevicePushData::getCreateTime)
               .last("LIMIT 100");
        
        // 如果指定了clientId，只查指定设备
        if (StringUtils.hasText(clientId)) {
            wrapper.eq(DevicePushData::getClientId, clientId);
        }
        
        List<DevicePushData> recentData = list(wrapper);
        
        if (recentData.isEmpty()) {
            return result;
        }
        
        BigDecimal tempSum = BigDecimal.ZERO;
        BigDecimal humiSum = BigDecimal.ZERO;
        BigDecimal soilPhSum = BigDecimal.ZERO;
        BigDecimal soilCondSum = BigDecimal.ZERO;
        BigDecimal lightSum = BigDecimal.ZERO;
        BigDecimal co2Sum = BigDecimal.ZERO;
        int count = 0;
        
        for (DevicePushData data : recentData) {
            if (data.getAmbientTemperature() != null) {
                tempSum = tempSum.add(data.getAmbientTemperature());
            }
            if (data.getAmbientHumidity() != null) {
                humiSum = humiSum.add(data.getAmbientHumidity());
            }
            if (data.getSoilPh() != null) {
                soilPhSum = soilPhSum.add(data.getSoilPh());
            }
            if (data.getSoilCond() != null) {
                soilCondSum = soilCondSum.add(data.getSoilCond());
            }
            if (data.getLightIntensity() != null) {
                lightSum = lightSum.add(data.getLightIntensity());
            }
            if (data.getCo2() != null) {
                co2Sum = co2Sum.add(data.getCo2());
            }
            count++;
        }
        
        if (count > 0) {
            result.put("avgTemperature", tempSum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP));
            result.put("avgHumidity", humiSum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP));
            result.put("avgSoilPH", soilPhSum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
            result.put("avgSoilEC", soilCondSum.divide(BigDecimal.valueOf(count * 1000), 3, RoundingMode.HALF_UP));
            result.put("avgLight", lightSum.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP));
            result.put("avgCO2", co2Sum.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP));
        }
        
        return result;
    }
    
    /**
     * 获取设备状态分布（支持按设备过滤）
     */
    private Map<String, Object> getDeviceStatusDistributionWithFilter(String clientId) {
        Map<String, Object> dist = new HashMap<>();
        
        List<Map<String, Object>> devices = getActiveDevicesWithFilter(clientId);
        int total = devices.size();
        int online = 0;
        int warning = 0;
        
        for (Map<String, Object> device : devices) {
            Object rssi = device.get("rssi");
            if (rssi instanceof Number) {
                int signal = ((Number) rssi).intValue();
                if (signal > -60) {
                    online++;
                } else if (signal > -80) {
                    warning++;
                }
            }
        }
        
        dist.put("online", online);
        dist.put("warning", warning);
        dist.put("offline", Math.max(0, total - online - warning));
        dist.put("total", total);
        
        return dist;
    }
    
    /**
     * 生成告警信息（支持按设备过滤）
     */
    private List<Map<String, Object>> generateAlertsWithFilter(String clientId) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        LocalDateTime checkTime = LocalDateTime.now().minusHours(1);
        
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(DevicePushData::getCreateTime, checkTime);
        
        // 如果指定了clientId，只查指定设备
        if (StringUtils.hasText(clientId)) {
            wrapper.eq(DevicePushData::getClientId, clientId);
        }
        
        List<DevicePushData> recentData = list(wrapper);
        
        for (DevicePushData data : recentData) {
            // 检查RSSI过低
            if (data.getRssi() != null && data.getRssi() < -80) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("clientId", data.getClientId());
                alert.put("type", "rssi");
                alert.put("level", "warning");
                alert.put("message", "信号弱: " + data.getRssi() + " dBm");
                alert.put("time", data.getCreateTime());
                alerts.add(alert);
            }
            
            // 检查温度异常
            if (data.getAmbientTemperature() != null && 
                (data.getAmbientTemperature().compareTo(new BigDecimal("40")) > 0 || 
                 data.getAmbientTemperature().compareTo(new BigDecimal("0")) < 0)) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("clientId", data.getClientId());
                alert.put("type", "temperature");
                alert.put("level", "warning");
                alert.put("message", "温度异常: " + data.getAmbientTemperature() + "℃");
                alert.put("time", data.getCreateTime());
                alerts.add(alert);
            }
            
            // 检查湿度异常
            if (data.getAmbientHumidity() != null && 
                (data.getAmbientHumidity().compareTo(new BigDecimal("90")) > 0 || 
                 data.getAmbientHumidity().compareTo(new BigDecimal("10")) < 0)) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("clientId", data.getClientId());
                alert.put("type", "humidity");
                alert.put("level", "warning");
                alert.put("message", "湿度异常: " + data.getAmbientHumidity() + "%");
                alert.put("time", data.getCreateTime());
                alerts.add(alert);
            }
            
            // 检查土壤pH异常
            if (data.getSoilPh() != null && 
                (data.getSoilPh().compareTo(new BigDecimal("8")) > 0 || 
                 data.getSoilPh().compareTo(new BigDecimal("5")) < 0)) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("clientId", data.getClientId());
                alert.put("type", "soilPh");
                alert.put("level", "warning");
                alert.put("message", "土壤pH异常: " + data.getSoilPh());
                alert.put("time", data.getCreateTime());
                alerts.add(alert);
            }
        }
        
        return alerts;
    }
    
    /**
     * 获取最近一条记录（支持按设备过滤）
     */
    private DevicePushData getLatestRecordWithFilter(String clientId) {
        LambdaQueryWrapper<DevicePushData> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(DevicePushData::getCreateTime)
               .last("LIMIT 1");
        
        // 如果指定了clientId，只查指定设备
        if (StringUtils.hasText(clientId)) {
            wrapper.eq(DevicePushData::getClientId, clientId);
        }
        
        return getOne(wrapper);
    }
}
