package com.weiming.smartag.websocket;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传感器数据WebSocket处理器
 */
@Slf4j
@Component
public class SensorWebSocketHandler extends TextWebSocketHandler {

    /** 存储所有连接会话 */
    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        SESSIONS.put(sessionId, session);
        log.info("WebSocket连接建立: {}, 当前连接数: {}", sessionId, SESSIONS.size());

        // 发送连接成功消息
        session.sendMessage(new TextMessage("{\"type\":\"connected\",\"message\":\"连接成功\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到WebSocket消息: {}", payload);

        try {
            Map<String, Object> data = JSON.parseObject(payload, Map.class);
            String type = (String) data.get("type");

            if ("subscribe".equals(type)) {
                String sensorId = (String) data.get("sensorId");
                session.getAttributes().put("subscribeSensorId", sensorId);
                session.sendMessage(new TextMessage(
                        "{\"type\":\"subscribed\",\"sensorId\":\"" + sensorId + "\"}"));
            } else if ("ping".equals(type)) {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", e.getMessage());
            session.sendMessage(new TextMessage(
                    "{\"type\":\"error\",\"message\":\"消息格式错误\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        SESSIONS.remove(sessionId);
        log.info("WebSocket连接关闭: {}, 状态: {}, 当前连接数: {}",
                sessionId, status, SESSIONS.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", exception.getMessage());
        SESSIONS.remove(session.getId());
    }

    /**
     * 获取所有在线会话
     */
    public Map<String, WebSocketSession> getSessions() {
        return new ConcurrentHashMap<>(SESSIONS);
    }

    /**
     * 向指定会话发送消息
     */
    public void sendMessage(String sessionId, String message) {
        WebSocketSession session = SESSIONS.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送WebSocket消息失败: {}", e.getMessage());
            }
        }
    }
}
