package com.whatsapp.android.util;


import com.whatsapp.android.config.ThreadLocalAuthenticator;

/**
 * @author sunnoc
 * @date 2020-11-25 14:18
 */
public class ThreadLocalAuthUtil {
    public static final ThreadLocalAuthenticator AUTHENTICATOR = new ThreadLocalAuthenticator();
}
