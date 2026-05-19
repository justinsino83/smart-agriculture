package com.weiming.smartag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartAgricultureApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartAgricultureApplication.class, args);
    }
}