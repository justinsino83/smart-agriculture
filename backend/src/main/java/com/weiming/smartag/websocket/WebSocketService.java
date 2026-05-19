package com.weiming.smartag.websocket;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

/**
 * WebSocket推送服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SensorWebSocketHandler sensorWebSocketHandler;

    /**
     * 推送传感器数据到所有订阅该传感器的客户端
     */
    public void pushSensorData(Long sensorId, Map<String, Object> data) {
        String message = JSON.toJSONString(Map.of(
                "type", "sensorData",
                "sensorId", sensorId,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));

        Map<String, WebSocketSession> sessions = sensorWebSocketHandler.getSessions();
        sessions.forEach((sessionId, session) -> {
            if (session.isOpen()) {
                Object subscribedId = session.getAttributes().get("subscribeSensorId");
                // 如果未指定订阅或订阅的是当前传感器，则推送
                if (subscribedId == null || String.valueOf(sensorId).equals(subscribedId)) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("推送传感器数据失败: {}", e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 推送预警信息
     */
    public void pushAlert(Map<String, Object> alert) {
        String message = JSON.toJSONString(Map.of(
                "type", "alert",
                "data", alert,
                "timestamp", System.currentTimeMillis()
        ));

        broadcast(message);
    }

    /**
     * 推送系统通知
     */
    public void pushNotification(String title, String content) {
        String message = JSON.toJSONString(Map.of(
                "type", "notification",
                "title", title,
                "content", content,
                "timestamp", System.currentTimeMillis()
        ));

        broadcast(message);
    }

    /**
     * 广播消息到所有连接
     */
    public void broadcast(String message) {
        Map<String, WebSocketSession> sessions = sensorWebSocketHandler.getSessions();
        sessions.forEach((sessionId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("广播消息失败: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * 获取当前在线连接数
     */
    public int getOnlineCount() {
        return sensorWebSocketHandler.getSessions().size();
    }
}
