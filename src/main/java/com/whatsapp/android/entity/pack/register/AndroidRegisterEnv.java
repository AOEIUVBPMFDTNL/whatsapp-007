package com.whatsapp.android.entity.pack.register;

import com.whatsapp.android.entity.DeviceInfo;
import lombok.Data;

/**
 * @author sunnoc
 * @date 2023-06-27 17:01
 */
@Data
public class AndroidRegisterEnv {
    /**
     * KeyHelper.generateIdentityKeyPair();
     */
    private byte[] identityKeyPair;
    /**
     * KeyHelper.generateRegistrationId(true);
     */
    private int registrationId;
    /**
     * KeyHelper.generateSignedPreKey(identityKeyPair, 0);
     */
    private byte[] signedPreKey;
    private String fullPhone;
    private byte[] expId;
    private String fdId;
    private byte[] publicKey;
    private byte[] privateKey;
    private String mcc;
    private String mnc;
    private String iso639;
    private String iso3166;
    private DeviceInfo deviceInfo;
    private String backupToken;
    private String id;
    private String offlineAb;
    private String deviceRam;
    private int pid;
    private String token;
    private String version;
    private int mistyped;
}
