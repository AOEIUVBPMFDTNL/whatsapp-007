package jni;

public class Register {
    public static native String EncodeExistRequest(boolean normal, byte[] params, byte[] env);

    public static native String CodeRequest(boolean normal, boolean sms, byte[] params, byte[] env);


    public static native String Register(boolean normal,byte[] params, byte[] env);

    public static native String GenerateEnc(String params);

    public static native String EncodeParam(byte[] params);

    //解压 apk， 计算 classes.dex md5,  然后base64 传入
    public static native void SetNormalMd5(String base64Md5);
    public static native void SetNormalVersion(String version);


    //解压 apk， 计算 classes.dex md5,  然后base64 传入
    public static native void SetBussinessMd5(String base64Md5);
    public static native void SetBussinessVersion(String version);


    //GCM 注册
    // proxyptype  "socks5" "http"
    public static native String GCMRegister(int version, String proxyptype, String proxy_server, int port, String username, String password);


    public static native long CreateGCMClient(String param);
    public static native void DestroyGCMClient(long instance);
    public static native String InitData(long instance, String proxyptype, String proxy_server, int port, String username, String password);
    public static native void Feed(long instance, byte[] buffer);
    //需要启动一个线程调用(只需要启动一个线程就可以)， 如果没有结果会阻塞， 所有的账号的结果都从这里获取， json格式有一个gcm_token字段标识是那种账号的。
    public static native String GetPnResult();
    /*
    *new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                System.out.println(Register.GetPnResult());
            }
        }
    }).start();
    * */
}
