package com.eureka.timetabling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ứng dụng chính của Hệ thống Xếp lịch Eureka English Center
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
public class TimetablingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimetablingApplication.class, args);
    }
}
