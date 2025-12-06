package com.whatsapp.android.constant;

import lombok.Data;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author sunnoc
 * @date 2020-05-14 15:55
 */
@Data
@SpringBootConfiguration
@ConfigurationProperties(prefix = "whatsapp")
public class ServiceConstant {
    /**
     * 服务器地址
     */
    private String serverAddr;
    /**
     * 消息是否gzip压缩
     */
    private boolean gzip;
    /**
     * gzip压缩websocket连接地址
     */
    private String defaultConnectAddr;
    /**
     * 域名
     */
    private String domainName;
    /**
     * 不连接ws
     */
    private boolean disConWs;
    /**
     * 是否仅注册
     */
    private boolean onlyRegister;

}
