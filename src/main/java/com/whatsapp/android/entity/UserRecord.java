package com.whatsapp.android.entity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author sunnoc
 * @date 2020-07-28 19:10
 */
public class UserRecord {
    private static final ConcurrentMap<String, User> record = new ConcurrentHashMap<>();
    /**
     * 注册记录
     */
    private static final ConcurrentMap<String, Register> registerRecord = new ConcurrentHashMap<>();

    public static ConcurrentMap<String, User> getRecord() {
        return record;
    }

    public static ConcurrentMap<String, Register> getRegisterRecord() {
        return registerRecord;
    }
}
