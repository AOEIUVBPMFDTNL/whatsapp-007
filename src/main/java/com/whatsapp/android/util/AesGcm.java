package com.whatsapp.android.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@UtilityClass
public class AesGcm {
    private final int NONCE = 128;

    public byte[] encrypt(long iv, byte @NonNull [] input, byte @NonNull [] key) {
        return encrypt(iv, input, key, null);
    }

    public byte[] encrypt(long iv, byte @NonNull [] input, byte @NonNull [] key, byte[] additionalData) {
        return cipher(toIv(iv), input, key, additionalData, true);
    }

    private byte[] cipher(byte @NonNull [] iv, byte @NonNull [] input, byte @NonNull [] key, byte[] additionalData, boolean encrypt) {
        try {
            GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
            AEADParameters parameters = new AEADParameters(new KeyParameter(key), NONCE, iv, additionalData);
            cipher.init(encrypt, parameters);
            int outputLength = cipher.getOutputSize(input.length);
            byte[] output = new byte[outputLength];
            int outputOffset = cipher.processBytes(input, 0, input.length, output, 0);
            cipher.doFinal(output, outputOffset);
            return output;
        } catch (InvalidCipherTextException exception) {
            throw new RuntimeException(String.format("Cannot %s data", encrypt ? "encrypt" : "decrypt"), exception);
        }
    }

    private byte[] toIv(long iv) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            dataOutputStream.write(new byte[4]);
            dataOutputStream.writeLong(iv);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public byte[] decrypt(long iv, byte @NonNull [] input, byte @NonNull [] key) {
        return decrypt(iv, input, key, null);
    }

    public byte[] decrypt(long iv, byte @NonNull [] input, byte @NonNull [] key, byte[] additionalData) {
        return cipher(toIv(iv), input, key, additionalData, false);
    }

    public byte[] encrypt(byte @NonNull [] iv, byte @NonNull [] input, byte @NonNull [] key, byte[] additionalData) {
        return cipher(iv, input, key, additionalData, true);
    }

    public byte[] encrypt(byte @NonNull [] iv, byte @NonNull [] input, byte @NonNull [] key) {
        return cipher(iv, input, key, null, true);
    }

    public byte[] decrypt(byte @NonNull [] iv, byte @NonNull [] input, byte @NonNull [] key, byte[] additionalData) {
        return cipher(iv, input, key, additionalData, false);
    }
}
