package com.whatsapp.android.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * @author sunnoc
 * @date 2022-11-21 22:28
 */
@Component
public class TomcatThreadPool {
    @Autowired
    ServletWebServerApplicationContext servletWebServerApplicationContext;

    @Bean(name = "tomcatThreadPoolExecutor")
    public Executor tomcatThreadPoolExecutor() {
        return ThreadPoolConfig.startThreadPool("tmLogin", 30, 400);
    }
}
