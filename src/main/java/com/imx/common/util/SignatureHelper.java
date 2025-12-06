package com.imx.common.util;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;

public class SignatureHelper {

    private static final String ALGORITHM = "SHA1withRSA";

    public static byte[] sign(PrivateKey privateKey, byte[] data, String algorithm) {
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static byte[] sign(PrivateKey privateKey, byte[] data) {
        return sign(privateKey, data, ALGORITHM);
    }

}
