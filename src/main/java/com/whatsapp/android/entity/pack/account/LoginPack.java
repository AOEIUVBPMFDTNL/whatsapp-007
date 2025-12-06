package com.whatsapp.android.entity.pack.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.whatsapp.android.entity.ProxyInfo;
import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-03-10 10:59
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginPack {
    /**
     * 是否使用系统ip
     */
    private boolean useSystemIp;
    /**
     * 使用动态ip
     */
    private boolean useDynamicIp;
    /**
     * 是否使用代理
     */
    private boolean makeProxy;
    /**
     * 代理信息
     */
    private ProxyInfo proxyInfo;
    /**
     * 账号
     */
    private String username;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 环境加密key
     */
    private String envEncryptKey;
    /**
     * 更新终端登陆
     */
    private boolean updateTerminalLogin;
    /**
     * 是否使用ipv6
     */
    private boolean ipv6;
    /**
     * ios登陆
     */
    private boolean iosLogin;
    /**
     * 是否是商业版本
     */
    private boolean businessVersion;
    /**
     * 是否异步登陆
     */
    private boolean async;
    /**
     * 使用rola ip 切换
     */
    private String rolaSwitchUrl;
    /**
     * 商户对接id
     */
    private String mchId;

    /**
     * 是否开启重登
     */
    private boolean forceLogin;
    /**
     * 指定wa版本
     */
    private String waVersion;
    /**
     * 执行初始化
     */
    private boolean executeInit;
    /**
     * 是否是劫持号模式，劫持号不执行初始化
     */
    private boolean hijackMode;
}
