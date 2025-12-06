package org.whispersystems.curve25519;


import java.security.SecureRandom;

public class NativeVOPRFExtension {
    public static void Sha1Random(byte[] bArr) {
        try {
            SecureRandom.getInstance("SHA1PRNG").nextBytes(bArr);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
    //1bebd8a069531c97192e82133037a200e9b3b6dc15025bd47f0d89f10cc1963a
    //32
    //63b065768accd2d51080213e18835e00de3bf1e6aee3613048cc388a5296bc16
    //32

    private native byte[] nativeBlind(byte[] bArr, int i, byte[] bArr2, int i2);

    private native byte[] nativeUnBlind(byte[] bArr, int i, byte[] bArr2, int i2, byte[] bArr3);

    public byte[] A01(int i) {
        byte[] bArr = new byte[i];
        Sha1Random(bArr);
        return bArr;
    }

    public byte[] A02(byte[] bArr, int i, byte[] bArr2, int i2, byte[] bArr3) {
        try {
            return nativeUnBlind(bArr, i, bArr2, i2, bArr3);
        } catch (SecurityException | UnsatisfiedLinkError unused) {
            return null;
        }
    }
        //1bebd8a069531c97192e82133037a200e9b3b6dc15025bd47f0d89f10cc1963a   //orignal_token
    //63b065768accd2d51080213e18835e00de3bf1e6aee3613048cc388a5296bc16
    // 32
    //result : 6d7b3401fde10b3f4ee92ca3e9f819b3f596c3e947079479c0e4d7678e996abe
    public byte[] A03(byte[] bArr, byte[] bArr2, int i) {
        try {
            return nativeBlind(bArr, i, bArr2, 32);
        } catch (SecurityException | UnsatisfiedLinkError unused) {
            return null;
        }
    }
}