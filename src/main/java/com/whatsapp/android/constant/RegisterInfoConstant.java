package com.whatsapp.android.constant;

import lombok.Data;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 注册信息服务
 *
 * @author sunnoc
 * @date 2023-03-11 11:28
 */
@Data
@SpringBootConfiguration
@ConfigurationProperties(prefix = "register")
public class RegisterInfoConstant {
    private String ocrUrl;
    private RegisterInfo normalVersionRegisterInfo;
    private RegisterInfo normalIosVersionRegisterInfo;
    private RegisterInfo businessVersionRegisterInfo;
    private RegisterInfo businessIosVersionRegisterInfo;

    @Data
    public static class RegisterInfo {
        private String version;
        private String classesMd5Base64;
        private String key;
    }
}
