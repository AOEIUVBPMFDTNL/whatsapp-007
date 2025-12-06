package com.whatsapp.android.constant;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@SpringBootConfiguration
@ConfigurationProperties(prefix = "redis2")
public class RedisConstantTwo {

    @Value("${redis2.host}")
    private String host;
    @Value("${redis2.port}")
    private int port;
    @Value("${redis2.password}")
    private String password;
    @Value("${redis2.database}")
    private int database;

    //pool映射
    @Value("${redis2.lettuce.pool.max-active}")
    private int maxActive;
    @Value("${redis2.lettuce.pool.max-idle}")
    private int maxIdle;
    @Value("${redis2.lettuce.pool.min-idle}")
    private int minIdle;
    @Value("${redis2.lettuce.pool.max-wait}")
    private long maxWait;
}
