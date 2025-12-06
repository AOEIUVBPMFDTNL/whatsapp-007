package com.whatsapp.android.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 处理未捕获异常
 *
 * @author sunnoc
 * @date 2023-06-20 11:12
 */
@Component
@Slf4j
public class UncaughtExceptionHandler {
    @Bean
    public void handler() {
        // 设置自定义的UncaughtExceptionHandler
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception in thread {}: {}", thread.getName(), throwable.getMessage(), throwable);
        });
    }
}
