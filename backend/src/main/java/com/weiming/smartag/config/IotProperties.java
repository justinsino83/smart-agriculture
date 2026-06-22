package com.weiming.smartag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部 IoT 平台配置
 *
 *   iot.platform.base-url  - IoT 接口基础地址
 *   iot.platform.token     - 访问 token
 *   iot.camera.farms       - 摄像头/水位计/排水阀按农场分组（推荐，YAML key 无中文）
 *                            farms:
 *                              - name: 维明农场
 *                                clientId: 2604073202TXD-01
 *                                stationName: 老叶维明农场
 *                                ids: [2067169374367645696]
 *                              - name: 红耕农场
 *                                clientId: 2604073202TXD-02
 *                                stationName: 根思金禾农田
 *                                ids: [2067194219855872000]
 *   iot.camera.device-ids  - （兼容旧写法）农场名 → 摄像头 deviceId 列表
 *                            注：key 中出现中文会被 Spring Boot 归一化，易丢失，不推荐。
 */
@ConfigurationProperties(prefix = "iot")
public class IotProperties {

    private Platform platform = new Platform();
    private Camera camera = new Camera();

    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }

    public Camera getCamera() { return camera; }
    public void setCamera(Camera camera) { this.camera = camera; }

    public static class Platform {
        private String baseUrl = "";
        private String token = "";
        private int connectTimeout = 15000;
        private int readTimeout = 15000;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public int getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(int connectTimeout) { this.connectTimeout = connectTimeout; }
        public int getReadTimeout() { return readTimeout; }
        public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    }

    /**
     * 一个农场对应的完整 IoT 配置：
     *  name        - 菜单中的农场名（维明农场 / 红耕农场）
     *  clientId    - 环境/土壤/气象的 clientId，用于 device_push_data 查询
     *  stationName - IoT listAllDevices 中设备的 station_name 关键字，用于过滤水位计/排水阀
     *  ids         - 摄像头 deviceId 列表
     */
    public static class CameraFarm {
        private String name = "";
        private String clientId = "";
        private String stationName = "";
        private List<String> ids = new ArrayList<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getStationName() { return stationName; }
        public void setStationName(String stationName) { this.stationName = stationName; }

        public List<String> getIds() { return ids; }
        public void setIds(List<String> ids) { this.ids = ids; }
    }

    public static class Camera {
        private List<CameraFarm> farms = new ArrayList<>();
        private Map<String, List<String>> deviceIds = new LinkedHashMap<>();

        public List<CameraFarm> getFarms() { return farms; }
        public void setFarms(List<CameraFarm> farms) { this.farms = farms; }

        public Map<String, List<String>> getDeviceIds() { return deviceIds; }
        public void setDeviceIds(Map<String, List<String>> deviceIds) { this.deviceIds = deviceIds; }
    }
}
