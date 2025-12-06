package com.whatsapp.android.entity;

import io.netty.util.Timeout;
import jni.GCMLogin;
import lombok.Data;

/**
 * @author sunnoc
 * @date 2023-05-22 14:52
 */
@Data
public class GcmPn {
    private String username;
    private GCMLogin gcmLogin;
    private Timeout timeout;

    public GcmPn() {
    }

    public GcmPn(String username, GCMLogin gcmLogin) {
        this.username = username;
        this.gcmLogin = gcmLogin;
    }
}
