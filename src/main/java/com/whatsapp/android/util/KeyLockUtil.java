package com.whatsapp.android.util;

/**
 * @author sunnoc
 * @date 2021-05-14 15:30
 */
public class KeyLockUtil {
    public static final KeyLock<String> KEY_LOCK = new KeyLock<>();

    public static void lock(String key) {
        KEY_LOCK.lock(key);
    }
    public static void unlock(String key) {
        KEY_LOCK.unlock(key);
    }
}
