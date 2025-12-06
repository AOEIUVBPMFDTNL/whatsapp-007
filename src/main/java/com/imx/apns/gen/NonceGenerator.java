package com.imx.apns.gen;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;

public class NonceGenerator {


    /**
     * bytes = key (1 byte) + second timestamp (8 byte) + random (8 byte)
     */
    public static byte[] generateNonce(byte key) {
        long second = Instant.now().getEpochSecond() * 1000;
        byte[] sinceBytes = ByteBuffer.allocate(8).putLong(second).array();

        byte[] randBytes = new byte[8];
        new SecureRandom().nextBytes(randBytes);

        ByteBuffer bb = ByteBuffer.allocate(1 + 8 + 8);
        bb.put(key);
        bb.put(sinceBytes);
        bb.put(randBytes);

        return bb.array();
    }

}
