package com.whatsapp.android.entity.pack.register;

import com.whatsapp.android.constant.RegisterInfoConstant;
import com.whatsapp.android.entity.ProxyInfo;
import lombok.Data;

/**
 * @author sunnoc
 * @date 2021-03-20 12:00
 */
@Data
public class RegisterPack {
    private ProxyInfo proxyInfo;
    /**
     * 完整用户名
     */
    private String username;
    /**
     * 手机号码区号
     */
    private String phoneAreaCode;
    /**
     * 手机号码，不带区号
     */
    private String phone;
    /**
     * 手机验证码
     */
    private String verifyCode;
    /**
     * 注册key
     */
    private String registerKey;
    /**
     * 是否是商业版本注册
     */
    private boolean businessVersion;
    /**
     * 是否发送语音呼叫短信
     */
    private boolean voiceCallSms;
    /**
     * 采用ios注册
     */
    private boolean ios;
    /**
     * 不插卡
     */
    private boolean noSimCard;
    /**
     * 发送验证码方法
     */
    private String method;
    /**
     * 指定注册信息
     */
    private RegisterInfoConstant.RegisterInfo registerInfo;
}
