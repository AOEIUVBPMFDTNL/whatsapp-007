package com.imx.apns.common;

public final class Constants {

    public static final String PLIST_XML_CERTIFICATE_PATTERN = "<data>(.*?)</data>";

    public static final int APPLE_COURIER_PORT = 5223;

    // ================================= request url =================================

    public static final String DEVICE_ACTIVATION_URL = "https://albert.apple.com/deviceservices/deviceActivation?device=%s";

    public static final String BAG_URL = "http://init-p01st.push.apple.com/bag";


    // ================================= certificate =================================
    public static final String[] CERT_ROOT_FILES = {
            "/certs/root/albert.apple.com.digicert.cert",
            "/certs/root/profileidentity.ess.apple.com.cert",
            "/certs/root/init-p01st.push.apple.com.cert",
            "/certs/root/init.ess.apple.com.cert",
            "/certs/root/content-icloud-com.cert"
    };

}
