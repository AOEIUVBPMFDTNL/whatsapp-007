package com.whatsapp.android.entity.response.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoipAcceptResult {
    /**
     * 主密钥
     */
    private String masterKey;
    /**
     * 用户id
     */
    private String senderJid;
    /**
     * 粉丝id
     */
    private String receiveJid;
    /**
     * 拨打id
     */
    private String callId;
    /**
     * udp连接地址
     */
    private String serverIp;
    /**
     * udp连接端口
     */
    private String port;
    /**
     * udp ipv6连接地址
     */
    private String ipv6ServerIp;
    /**
     * udp ipv6连接端口
     */
    private String ipv6Port;
    /**
     * TURN令牌
     */
    private String token;
    /**
     * 通信key
     */
    private String password;
    /**
     * 命令
     */
    private String command;
}