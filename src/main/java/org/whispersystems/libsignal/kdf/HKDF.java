/**
 * Copyright (C) 2013-2016 Open Whisper Systems
 * <p>
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.libsignal.kdf;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.ByteArrayOutputStream;

public abstract class HKDF {

    private static final int HASH_OUTPUT_SIZE = 32;

    public static HKDF createFor(int messageVersion) {
        switch (messageVersion) {
            case 2:
                return new HKDFv2();
            case 3:
                return new HKDFv3();
            default:
                throw new AssertionError("Unknown version: " + messageVersion);
        }
    }

    public byte[] deriveSecrets(byte[] inputKeyMaterial, byte[] info, int outputLength) {
        byte[] salt = new byte[HASH_OUTPUT_SIZE];
        return deriveSecrets(inputKeyMaterial, salt, info, outputLength);
    }

    public byte[] deriveSecrets(byte[] inputKeyMaterial, byte[] salt, byte[] info, int outputLength) {
        byte[] prk = extract(salt, inputKeyMaterial);
        return expand(prk, info, outputLength);
    }

    private byte[] extract(byte[] salt, byte[] inputKeyMaterial) {
        try {
            HMac hMac = new HMac(new SHA256Digest());
            hMac.init(new KeyParameter(salt));
            hMac.update(inputKeyMaterial, 0, inputKeyMaterial.length);
            byte[] out = new byte[hMac.getMacSize()];
            hMac.doFinal(out, 0);
            return out;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private byte[] expand(byte[] prk, byte[] info, int outputSize) {
        try {
            int iterations = (int) Math.ceil((double) outputSize / (double) HASH_OUTPUT_SIZE);
            byte[] mixin = new byte[0];
            ByteArrayOutputStream results = new ByteArrayOutputStream();
            int remainingBytes = outputSize;

            for (int i = getIterationStartOffset(); i < iterations + getIterationStartOffset(); i++) {
                HMac hMac = new HMac(new SHA256Digest());
                hMac.init(new KeyParameter(prk));
                hMac.update(mixin, 0, mixin.length);
                if (info != null) {
                    hMac.update(info, 0, info.length);
                }
                hMac.update((byte) i);
                byte[] stepResult = new byte[hMac.getMacSize()];
                hMac.doFinal(stepResult, 0);
                int stepSize = Math.min(remainingBytes, stepResult.length);
                results.write(stepResult, 0, stepSize);
                mixin = stepResult;
                remainingBytes -= stepSize;
            }

            return results.toByteArray();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    protected abstract int getIterationStartOffset();

}
