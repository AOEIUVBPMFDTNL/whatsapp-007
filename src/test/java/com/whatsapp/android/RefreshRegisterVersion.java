package com.whatsapp.android;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 刷新注册版本
 *
 * @author sunnoc
 * @date 2023-01-10 10:27
 */
@Slf4j
public class RefreshRegisterVersion {
    public static void main(String[] args) {
        String content = "";
        String[] split = StrUtil.split(content, "\n");
        for (String ip : split) {
            if (refreshVersion(ip)) {
                log.info("{}：刷新注册版本成功", ip);
            } else {
                log.info("{}：刷新注册版本失败", ip);
            }
        }
        log.info("刷新注册版本完成");
    }

    private static boolean refreshVersion(String ip) {
        try {
            String content = HttpUtil.get("http://" + ip + ":85/api/refreshRegisterVersionInfo");
            return "ok".equals(content);
        } catch (Exception ignore) {
            return false;
        }
    }
}
