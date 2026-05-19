package com.weiming.smartag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 应用启动测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "mqtt.broker-url=tcp://localhost:1883"
})
class SmartAgricultureApplicationTests {

    @Test
    void contextLoads() {
        // 验证Spring上下文能正常加载
    }
}
