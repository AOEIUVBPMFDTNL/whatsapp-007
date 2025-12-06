package com.whatsapp.android.constant;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.entity.ProxyInfo;
import com.whatsapp.android.util.WhatsAppUtils;
import lombok.Data;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author sunnoc
 * @date 2023-05-23 16:00
 */
@Data
@SpringBootConfiguration
@ConfigurationProperties(prefix = "gcm")
public class GcmProxyConstant {
    private int version;
    /**
     * http:0，socks5:1
     */
    private int proxyType;
    private String proxyHost;
    private int proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    /**
     * APNS代理类型：1ELF，2皇冠
     */
    private int apnsProxyType;

    private List<String> gcmIpList;
    /**
     * elf proxy
     */
    private List<String> elfProxyList;
    /**
     * 皇冠代理
     */
    private List<String> iproyalProxyList;

    /**
     * 获取gcm代理
     */
    public ProxyInfo getRandomGcmListIp() {
        return randomProxyInfo(gcmIpList);
    }

    /**
     * 获取apns代理
     */
    public ProxyInfo randomAPNSProxy() {
        if (apnsProxyType == 1) {
            return randomElfProxyInfo();
        }
        return randomIproyalProxyInfo();
    }

    private ProxyInfo randomElfProxyInfo() {
        return randomProxyInfo(elfProxyList, true);
    }

    private ProxyInfo randomIproyalProxyInfo() {
        return randomProxyInfo(iproyalProxyList, true);
    }

    private ProxyInfo randomProxyInfo(List<String> proxyList) {
        return randomProxyInfo(proxyList, false);
    }

    private ProxyInfo randomProxyInfo(List<String> proxyList, boolean apns) {
        if (proxyList == null || proxyList.isEmpty()) {
            return null;
        }
        int randomIndex = RandomUtil.randomInt(0, proxyList.size());
        String ips = proxyList.get(randomIndex);
        String[] split = StrUtil.split(ips, ":");
        if (split.length >= 5) {
            ProxyInfo proxyInfo = new ProxyInfo();
            proxyInfo.setType(Convert.toInt(split[4], 0));
            proxyInfo.setProxyHost(split[0]);
            proxyInfo.setProxyPort(Convert.toInt(split[1], 0));
            proxyInfo.setProxyUser(split[2]);
            if (apns) {
                proxyInfo.setProxyPwd(WhatsAppUtils.changeProxy(split[3]));
            } else {
                proxyInfo.setProxyPwd(split[3]);
            }
            return proxyInfo;
        }
        return null;
    }
}
