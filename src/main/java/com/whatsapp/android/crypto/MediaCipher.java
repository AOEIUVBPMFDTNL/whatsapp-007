package com.whatsapp.android.crypto;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import lombok.Data;
import org.whispersystems.libsignal.kdf.HKDFv3;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 媒体消息加密
 *
 * @author Rocky
 */
public class MediaCipher {
    /**
     * 密钥扩展位数
     */
    private static final int EXPAND_SIZE = 80;
    private static final String AES_CBC = "AES/CBC/PKCS5Padding";

    @Data
    public static class MediaEncryptInfo {
        public byte[] data;
        public byte[] origHash;
        public byte[] contentHash;
        public String token;
        public String mediaKey;
    }

    public static MediaEncryptInfo encrypt(String path, String mediaType) {
        MediaEncryptInfo mediaEncryptInfo = new MediaEncryptInfo();
        String mediaSalt = getMediaSalt(mediaType);
        byte[] originMedia = FileUtil.readBytes(path);
        byte[] mediaKey = RandomUtil.randomBytes(32);
        byte[] expandKey = new HKDFv3().deriveSecrets(mediaKey, mediaSalt.getBytes(StandardCharsets.UTF_8), EXPAND_SIZE);
        byte[] iv = new byte[16];
        byte[] key = new byte[32];
        byte[] hashKey = new byte[32];
        System.arraycopy(expandKey, 0, iv, 0, 16);
        System.arraycopy(expandKey, 16, key, 0, 32);
        System.arraycopy(expandKey, 48, hashKey, 0, 32);
        // 加密消息
        try {
            Cipher instance = Cipher.getInstance(AES_CBC);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            instance.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
            byte[] encrypted = instance.doFinal(originMedia);
            byte[] hmacInput = new byte[16 + encrypted.length];
            System.arraycopy(iv, 0, hmacInput, 0, 16);
            System.arraycopy(encrypted, 0, hmacInput, 16, encrypted.length);
            mediaEncryptInfo.setMediaKey(Base64.encode(mediaKey));
            HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, hashKey);
            byte[] digest = hMac.digest(hmacInput);
            byte[] finalEncrypt = new byte[encrypted.length + 10];
            System.arraycopy(encrypted, 0, finalEncrypt, 0, encrypted.length);
            System.arraycopy(digest, 0, finalEncrypt, encrypted.length, 10);
            byte[] contentHash = DigestUtil.sha256(finalEncrypt);
            mediaEncryptInfo.setContentHash(contentHash);
            mediaEncryptInfo.setOrigHash(DigestUtil.sha256(originMedia));
            mediaEncryptInfo.setData(finalEncrypt);
            mediaEncryptInfo.setToken(Base64.encodeUrlSafe(contentHash));
            return mediaEncryptInfo;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ignored) {
        }
        return mediaEncryptInfo;
    }

    public static MediaEncryptInfo encryptMemory(byte[] data, String mediaType) {
        MediaEncryptInfo mediaEncryptInfo = new MediaEncryptInfo();
        String mediaSalt = getMediaSalt(mediaType);
        byte[] mediaKey = RandomUtil.randomBytes(32);
        byte[] expandKey = new HKDFv3().deriveSecrets(mediaKey, mediaSalt.getBytes(StandardCharsets.UTF_8), EXPAND_SIZE);
        byte[] iv = new byte[16];
        byte[] key = new byte[32];
        byte[] hashKey = new byte[32];
        System.arraycopy(expandKey, 0, iv, 0, 16);
        System.arraycopy(expandKey, 16, key, 0, 32);
        System.arraycopy(expandKey, 48, hashKey, 0, 32);
        // 加密消息
        try {
            Cipher instance = Cipher.getInstance(AES_CBC);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            instance.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
            byte[] encrypted = instance.doFinal(data);
            byte[] hmacInput = new byte[16 + encrypted.length];
            System.arraycopy(iv, 0, hmacInput, 0, 16);
            System.arraycopy(encrypted, 0, hmacInput, 16, encrypted.length);
            mediaEncryptInfo.setMediaKey(Base64.encode(mediaKey));
            HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, hashKey);
            byte[] digest = hMac.digest(hmacInput);
            byte[] finalEncrypt = new byte[encrypted.length + 10];
            System.arraycopy(encrypted, 0, finalEncrypt, 0, encrypted.length);
            System.arraycopy(digest, 0, finalEncrypt, encrypted.length, 10);
            byte[] contentHash = DigestUtil.sha256(finalEncrypt);
            mediaEncryptInfo.setContentHash(contentHash);
            mediaEncryptInfo.setOrigHash(DigestUtil.sha256(data));
            mediaEncryptInfo.setData(finalEncrypt);
            mediaEncryptInfo.setToken(Base64.encodeUrlSafe(contentHash));
            return mediaEncryptInfo;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ignored) {
        }
        return mediaEncryptInfo;
    }


    public static String getMediaSalt(String mediaType) {
        switch (mediaType) {
            case "audio":
            case "ptt":
                return "WhatsApp Audio Keys";
            case "document":
                return "WhatsApp Document Keys";
            case "gif":
            case "video":
                return "WhatsApp Video Keys";
            case "image":
            case "sticker":
                return "WhatsApp Image Keys";
            case "history":
                return "WhatsApp History Keys";
            default:
                return "";
        }
    }

}
