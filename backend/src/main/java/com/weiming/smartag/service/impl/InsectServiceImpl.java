package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weiming.smartag.entity.InsectData;
import com.weiming.smartag.entity.InsectDevice;
import com.weiming.smartag.mapper.InsectDataMapper;
import com.weiming.smartag.mapper.InsectDeviceMapper;
import com.weiming.smartag.service.InsectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 虫情数据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsectServiceImpl implements InsectService {
    
    private final RestTemplate restTemplate;
    private final InsectDeviceMapper insectDeviceMapper;
    private final InsectDataMapper insectDataMapper;
    private final StringRedisTemplate stringRedisTemplate;
    
    // 虫情API配置
    @Value("${insect.api.base-url:http://182.40.36.93:8900}")
    private String baseUrl;
    
    @Value("${insect.api.appName:Insects}")
    private String appName;
    
    @Value("${insect.api.username:ktdz}")
    private String username;
    
    @Value("${insect.api.password:ktdz}")
    private String password;
    
    private static final String TOKEN_KEY = "insect:token";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER_T = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER_T);
            }
            return LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateStr);
            return null;
        }
    }
    
    /**
     * 获取Access Token（带缓存）
     */
    @Override
    public String getAccessToken() {
        try {
            // 先从Redis获取缓存的token
            String cachedToken = stringRedisTemplate.opsForValue().get(TOKEN_KEY);
            if (cachedToken != null && !cachedToken.isEmpty()) {
                log.debug("使用缓存的Token: {}", cachedToken);
                return cachedToken;
            }

            // 调用API获取新token
            String url = baseUrl + "/iotservice/insect/api/queryKey?username=" + username + "&password=" + password;
            log.info("获取Token URL: {}", url.replace(password, "******"));

            Map response = restTemplate.getForObject(url, Map.class);
            log.info("Token响应: code={}, data={}", response.get("code"), response.get("data"));

            if (response.get("code") != null && ((Number)response.get("code")).intValue() == 0) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                // 从列表中找到匹配appName的token
                for (Map<String, Object> item : dataList) {
                    String name = (String) item.get("name");
                    if (appName.equals(name) && item.get("token") != null) {
                        String token = (String) item.get("token");
                        // 缓存token（假设有效期2小时，这里缓存1小时）
                        stringRedisTemplate.opsForValue().set(TOKEN_KEY, token, java.time.Duration.ofHours(1));
                        log.info("获取新Token成功: {}...", token.substring(0, Math.min(10, token.length())));
                        return token;
                    }
                }
                throw new RuntimeException("未找到应用[" + appName + "]对应的Token");
            }
            throw new RuntimeException("API返回错误: " + response.get("msg"));
        } catch (Exception e) {
            log.error("获取Token失败", e);
            return null;
        }
    }
    
    /**
     * 获取设备列表
     */
    @Override
    public List<InsectDevice> getDeviceList() {
        try {
            String token = getAccessToken();
            if (token == null) {
                throw new RuntimeException("Token获取失败");
            }
            
            String url = baseUrl + "/iotservice/insect/api/queryDevStatus?appName=" + appName + "&token=" + token;
            log.info("获取设备列表 URL: {}", url);
            
            Map response = restTemplate.getForObject(url, Map.class);
            log.info("设备列表响应: code={}, data={}", response.get("code"), 
                    response.get("data") != null ? ((List)response.get("data")).size() + "条" : "null");
            
            if (response.get("code") != null && ((Number)response.get("code")).intValue() == 0) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                List<InsectDevice> devices = new ArrayList<>();
                
                for (Map<String, Object> item : dataList) {
                    InsectDevice device = new InsectDevice();
                    device.setRemoteId((Integer) item.get("id"));
                    device.setDevNum((String) item.get("devNum"));
                    device.setImei((String) item.get("imei"));
                    device.setDevName((String) item.get("devName"));
                    device.setDevType((String) item.get("devType"));
                    device.setDevTypeName((String) item.get("devTypeName"));
                    
                    // 经纬度处理
                    Object lngObj = item.get("lng");
                    Object latObj = item.get("lat");
                    if (lngObj != null) device.setLng(new BigDecimal(lngObj.toString()));
                    if (latObj != null) device.setLat(new BigDecimal(latObj.toString()));
                    
                    device.setDevState((Integer) item.get("devState"));
                    device.setOnlineStatus((String) item.get("onlineStatus"));
                    
                    // 处理时间
                    String lastDataTime = (String) item.get("lastDataTime");
                    if (lastDataTime != null) {
                        device.setLastDataTime(parseDateTime(lastDataTime));
                    }
                    
                    device.setAddress((String) item.get("address"));
                    device.setCreateTime(LocalDateTime.now());
                    device.setUpdateTime(LocalDateTime.now());
                    
                    devices.add(device);
                }
                
                return devices;
            }
            throw new RuntimeException("API返回错误: " + response.get("msg"));
        } catch (Exception e) {
            log.error("获取设备列表失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取虫情分析数据
     */
    @Override
    public List<InsectData> getInsectImagesByTimeRange(String imei, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            String token = getAccessToken();
            if (token == null) {
                throw new RuntimeException("Token获取失败");
            }
            
            String url = baseUrl + "/iotservice/insect/api/queryInsectImagesByTimeRange"
                    + "?stime=" + startTime.format(DATE_TIME_FORMATTER)
                    + "&etime=" + endTime.format(DATE_TIME_FORMATTER)
                    + "&imei=" + imei
                    + "&appName=" + appName
                    + "&token=" + token;
            
            log.info("获取虫情数据 URL: {}...imei={}", url.substring(0, 80), imei);
            
            Map response = restTemplate.getForObject(url, Map.class);
            
            if (response.get("code") != null && ((Number)response.get("code")).intValue() == 0) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                List<InsectData> result = new ArrayList<>();
                
                for (Map<String, Object> item : dataList) {
                    InsectData data = new InsectData();
                    data.setImei((String) item.get("imeinum"));
                    data.setImageUrl((String) item.get("image"));
                    data.setScaleImageUrl((String) item.get("scaleimage"));
                    data.setPlotImageUrl((String) item.get("plotimage"));
                    data.setObjectCount((Integer) item.get("objectCount"));
                    data.setDetectResult(escapeJson((String) item.get("detectResoult")));
                    data.setStorePath((String) item.get("storepath"));
                    
                    // 处理时间
                    String time = (String) item.get("time");
                    if (time != null) {
                        data.setRecordTime(parseDateTime(time));
                    }
                    
                    // 设置设备名称
                    InsectDevice device = insectDeviceMapper.selectOne(
                            new QueryWrapper<InsectDevice>().eq("imei", imei));
                    if (device != null) {
                        data.setDevName(device.getDevName());
                    }
                    
                    data.setCreateTime(LocalDateTime.now());
                    data.setUpdateTime(LocalDateTime.now());
                    
                    result.add(data);
                }
                
                log.info("获取虫情数据成功: imei={}, count={}", imei, result.size());
                return result;
            }
            throw new RuntimeException("API返回错误: " + response.get("msg"));
        } catch (Exception e) {
            log.error("获取虫情数据失败, imei={}", imei, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取虫情统计
     */
    @Override
    public List<Map<String, Object>> getInsectStatistic(String imei, String startDate, String endDate) {
        try {
            String token = getAccessToken();
            if (token == null) {
                throw new RuntimeException("Token获取失败");
            }
            
            String url = baseUrl + "/iotservice/insect/api/insectStatistic"
                    + "?stime=" + startDate
                    + "&etime=" + endDate
                    + "&imei=" + imei
                    + "&appName=" + appName
                    + "&token=" + token;
            
            log.info("获取虫情统计 URL: {}...imei={}", url.substring(0, 80), imei);
            
            Map response = restTemplate.getForObject(url, Map.class);
            
            if (response.get("code") != null && ((Number)response.get("code")).intValue() == 0) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                log.info("获取虫情统计成功: imei={}, count={}", imei, dataList.size());
                return dataList;
            }
            throw new RuntimeException("API返回错误: " + response.get("msg"));
        } catch (Exception e) {
            log.error("获取虫情统计失败, imei={}", imei, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 定时同步所有设备数据
     */
    @Override
    public void syncAllDevicesData() {
        log.info("开始同步所有设备数据...");
        
        // 1. 先同步设备列表
        List<InsectDevice> devices = getDeviceList();
        log.info("获取到 {} 个设备", devices.size());
        
        for (InsectDevice device : devices) {
            try {
                // 检查设备是否已存在
                InsectDevice existDevice = insectDeviceMapper.selectOne(
                        new QueryWrapper<InsectDevice>().eq("imei", device.getImei()));
                
                if (existDevice != null) {
                    // 更新
                    device.setId(existDevice.getId());
                    insectDeviceMapper.updateById(device);
                    log.debug("更新设备: {}", device.getDevName());
                } else {
                    // 新增
                    insectDeviceMapper.insert(device);
                    log.info("新增设备: {}", device.getDevName());
                }
            } catch (Exception e) {
                log.error("保存设备失败: {}", device.getDevName(), e);
            }
        }
        
        // 2. 同步每个设备最近24小时的数据
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusHours(24);
        
        for (InsectDevice device : devices) {
            try {
                syncDeviceData(device.getImei());
            } catch (Exception e) {
                log.error("同步设备数据失败: {}", device.getImei(), e);
            }
        }
        
        log.info("同步完成");
    }
    
    /**
     * 同步指定设备数据
     */
    @Override
    public void syncDeviceData(String imei) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusHours(24);
        
        List<InsectData> dataList = getInsectImagesByTimeRange(imei, yesterday, now);
        
        for (InsectData data : dataList) {
            try {
                // 检查是否已存在（根据图片名称和时间判断）
                InsectData exist = insectDataMapper.selectOne(
                        new QueryWrapper<InsectData>()
                                .eq("imei", data.getImei())
                                .eq("store_path", data.getStorePath())
                );
                
                if (exist == null) {
                    insectDataMapper.insert(data);
                    log.debug("新增虫情数据: {} - {}", data.getImei(), data.getRecordTime());
                } else {
                    // 更新
                    data.setId(exist.getId());
                    insectDataMapper.updateById(data);
                    log.debug("更新虫情数据: {} - {}", data.getImei(), data.getRecordTime());
                }
            } catch (Exception e) {
                log.error("保存虫情数据失败", e);
            }
        }
        
        log.info("设备 {} 同步完成，新增/更新 {} 条数据", imei, dataList.size());
    }
    
    /**
     * 转义JSON字符串
     */
    private String escapeJson(String str) {
        if (str == null) return null;
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public IPage<InsectDevice> getLocalDevicePage(int page, int size) {
        return insectDeviceMapper.selectPage(new Page<>(page, size),
                new QueryWrapper<InsectDevice>().orderByDesc("update_time"));
    }

    @Override
    public IPage<InsectData> getLocalDataPage(String imei, String startDate, String endDate, int page, int size) {
        QueryWrapper<InsectData> q = new QueryWrapper<>();
        if (imei != null && !imei.isEmpty()) q.eq("imei", imei);
        if (startDate != null) q.ge("record_time", startDate);
        if (endDate != null) q.le("record_time", endDate);
        q.orderByDesc("record_time");
        return insectDataMapper.selectPage(new Page<>(page, size), q);
    }
}