package com.whatsapp.android.entity;
import lombok.Data;

/**
 * @author sunnoc
 * @date 2020-07-23 19:00
 */

@Data
public class ProxyInfo {
    /**
     * 使用方式,0代表使用http代理，1代表s5代理，默认为http代理
     */
    private int type = 0;
    /**
     * 代理host
     */
    private String proxyHost;
    /**
     * 代理端口
     */
    private int proxyPort;
    /**
     * 代理用户名
     */
    private String proxyUser;
    /**
     * 代理密码
     */
    private String proxyPwd;

    public ProxyInfo() {
    }

    public ProxyInfo(int type, String proxyHost, int proxyPort) {
        this.type = type;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    public ProxyInfo(int type, String proxyHost, int proxyPort, String proxyUser, String proxyPwd) {
        this.type = type;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPwd = proxyPwd;
    }
}
