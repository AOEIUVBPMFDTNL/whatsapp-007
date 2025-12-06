package jni;

public class NoiseJni {

    public static native  String CheckWhatsappVersion(String proxyType, String proxyServer, int port, String userName, String password, String jniDir);

    public static native byte[] InitData(int osType, String version, byte[] routeInfo);

    public static native String stringFromJNI(String source);

    public static native String getCountryInfo2(String country);

    public static  native boolean InitConfig(String configPath);
}
