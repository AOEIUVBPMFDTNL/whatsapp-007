package com.whatsapp.android;

import cn.hutool.core.codec.Base64;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;

public class Curve25519PublicKeyCalculation {
    public static void main(String[] args) {
        // 假设这是你的私钥（16进制字符串）
        String hexPrivateKey = "ENTSHI2mdwJ8T9V4eZ+p88Q9XXvSdxcP/XKAMDftkkc=";
        byte[] privateKeyBytes = Base64.decode(hexPrivateKey);

        // 使用私钥创建 X25519PrivateKeyParameters 对象
        X25519PrivateKeyParameters privateKeyParameters = new X25519PrivateKeyParameters(privateKeyBytes, 0);
        X25519PublicKeyParameters x25519PublicKeyParameters = privateKeyParameters.generatePublicKey();
        byte[] encoded = x25519PublicKeyParameters.getEncoded();
        // 输出公钥
        System.out.println("Public Key: " + Base64.encode(encoded));
    }
}