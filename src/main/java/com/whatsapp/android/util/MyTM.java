package com.whatsapp.android.util;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;


/**
 * @author sunnoc
 * @date 2020-07-16 21:54
 */
public class MyTM implements TrustManager, X509TrustManager {


    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

    public boolean isServerTrusted(X509Certificate[] certs) {
        return true;
    }

    public boolean isClientTrusted(X509Certificate[] certs) {
        return true;
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) {
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType) {
    }

}
