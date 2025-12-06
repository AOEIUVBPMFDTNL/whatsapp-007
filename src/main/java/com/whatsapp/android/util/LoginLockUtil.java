package com.whatsapp.android.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 登录锁，以账号为纬度加锁
 *
 * @author sunnoc
 * @date 2021-03-12 10:27
 */
@Slf4j
public class LoginLockUtil {
    public static final ConcurrentMap<String, Integer> MAP = new ConcurrentHashMap<>();
}
