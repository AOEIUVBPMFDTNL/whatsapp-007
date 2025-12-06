package com.whatsapp.android.util;

import com.whatsapp.android.entity.GcmPn;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.UserRecord;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import jni.GCMLogin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author sunnoc
 * @date 2023-05-22 14:51
 */
public class GcmPnCacheUtil {
    private static final Map<String, GcmPn> gcmPnMap = new ConcurrentHashMap<>();
    public static final HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS);

    public static void put(String key, GcmPn gcmPn, int delay) {
        Timeout timeout = hashedWheelTimer.newTimeout(timeout1 -> {
            GcmPn remove = gcmPnMap.remove(key);
            String username = remove.getUsername();
            clear(remove);
            //断开账号连接
            User user = UserRecord.getRecord().get(username);
            if (user != null) {
                user.getGorgeousEngine().StopEngine();
            }
        }, delay, TimeUnit.SECONDS);
        gcmPn.setTimeout(timeout);
        gcmPnMap.put(key, gcmPn);
    }

    public static GcmPn get(String key) {
        return gcmPnMap.get(key);
    }

    public static void remove(String key) {
        GcmPn gcmPn = gcmPnMap.remove(key);
        if (gcmPn != null) {
            clear(gcmPn);
        }
    }

    public static void clear(GcmPn gcmPn) {
        GCMLogin gcmLogin = gcmPn.getGcmLogin();
        gcmLogin.disconnect(gcmLogin.socketChannel_, "");
        gcmPn.getTimeout().cancel();
    }

}
