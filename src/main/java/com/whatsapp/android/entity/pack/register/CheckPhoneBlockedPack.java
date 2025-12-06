package com.whatsapp.android.entity.pack.register;

import com.whatsapp.android.entity.ProxyInfo;
import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-11-04 18:15
 */
@Data
public class CheckPhoneBlockedPack {
    /**
     * 带区号手机号
     */
    private String phone;
    /**
     * 国家代码
     */
    private String countryCode;
    /**
     * 代理ip
     */
    private ProxyInfo proxyInfo;

}
