package com.imx.apns.gen;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class RSAKeyPairGenerator {

    private static final String ALGORITHM = "RSA";
    public static final int DEFAULT_KEY_SIZE = 1024;

    public static KeyPair generate(int keysize) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(keysize);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static KeyPair generate() {
        return generate(DEFAULT_KEY_SIZE);
    }

}
