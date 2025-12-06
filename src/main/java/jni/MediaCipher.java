package jni;

public class MediaCipher {
    public static class MediaEncryptInfo {
        public byte[] data;
        public  byte[] origHash;
        public byte[] contentHash;
        public String token;
        public String mediaKey;
    }
    //mediaType : "image" "ptt" "video" "document"
    public static native MediaEncryptInfo Encrypt(String path, String mediaType);
    public static native MediaEncryptInfo EncryptMemory(byte[] data, String mediaType);
    public static native boolean Decrypt(String srcPath, String destPath, String mediaKey, String mediaType);
    public static native boolean DecryptMemory(byte[] data, String destPath, String mediaKey, String mediaType);
}
