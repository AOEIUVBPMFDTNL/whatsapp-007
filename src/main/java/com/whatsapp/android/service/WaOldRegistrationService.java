package com.whatsapp.android.service;

import cn.hutool.core.convert.Convert;
import com.whatsapp.android.util.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WaOldRegistrationService {
    private final RedisService redisService;

    /**
     * 存储验证码
     *
     * @param username   小号
     * @param verifyCode 验证码
     */
    public void saveVerifyCode(String username, String verifyCode) {
        String key = username + ":verifyCode";
        redisService.set(key, verifyCode, 3 * 60L);
    }

    /**
     * 获取验证码
     *
     * @param username 小号
     * @return 验证码
     */
    public String getVerifyCode(String username) {
        String key = username + ":verifyCode";
        return Convert.toStr(redisService.get(key));
    }
}
