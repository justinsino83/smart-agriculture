package com.weiming.smartag;

import com.weiming.smartag.config.IotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(IotProperties.class)
public class SmartAgricultureApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartAgricultureApplication.class, args);
    }
}