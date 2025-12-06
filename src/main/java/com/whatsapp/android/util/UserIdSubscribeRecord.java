package com.whatsapp.android.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserIdSubscribeRecord {
    private static final String PREFIX = "userIdSubscribeRecord:";
    private static final Map<String, Map<String, String>> storage = new ConcurrentHashMap<>();

    public UserIdSubscribeRecord() {
    }

    /**
     * 添加
     */
    public void set(String username, String userId, String time) {
        String key = PREFIX + username;
        storage.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .put(userId, time);
    }

    /**
     * 不存在再添加，false代表之前已经存在值
     */
    public boolean hPutIfAbsent(String username, String userId, String time) {
        String key = PREFIX + username;
        Map<String, String> userMap = storage.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

        if (userMap.containsKey(userId)) {
            return false;
        }
        userMap.put(userId, time);
        return true;
    }

    /**
     * 获取
     */
    public String get(String username, String userId) {
        String key = PREFIX + username;
        Map<String, String> userMap = storage.get(key);
        if (userMap == null) {
            return "";
        }
        return userMap.getOrDefault(userId, "");
    }

    /**
     * 是否存在
     */
    public boolean hasKey(String username, String userId) {
        String key = PREFIX + username;
        Map<String, String> userMap = storage.get(key);
        return userMap != null && userMap.containsKey(userId);
    }

    /**
     * 清理特定用户的所有记录
     */
    public void clear(String username) {
        String key = PREFIX + username;
        storage.remove(key);
    }

    /**
     * 清理所有记录（新增方法）
     */
    public void clearAll() {
        storage.clear();
    }
}