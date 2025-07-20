package com.eos.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;

/**
 * 스케줄링 설정
 * - 스케줄링 기능 활성화
 * - 스케줄러 스레드 풀 설정
 * - cron 작업 관리
 */
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {
    
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 스케줄러용 전용 스레드 풀 설정
        // 스케줄러 작업이 메인 애플리케이션에 영향을 주지 않도록 분리
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(5));
    }
} 